package com.petlife.controller;

import com.petlife.model.ChatHistory;
import com.petlife.model.ChatSession;
import com.petlife.model.Faq;
import com.petlife.model.Order;
import com.petlife.repository.ChatHistoryRepository;
import com.petlife.repository.ChatSessionRepository;
import com.petlife.repository.FaqRepository;
import com.petlife.repository.OrderRepository;
import com.petlife.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatBotController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    // ══════════════════════════════════════════════
    // AI 智能客服 API（原有功能，不動）
    // ══════════════════════════════════════════════

    // ── AI 對話（含 FAQ 優先查詢） ──
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String userIdStr = payload.get("userId");

        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("問題不能為空");
        }

        // 儲存使用者訊息
        if (userIdStr != null) {
            saveHistory(Integer.parseInt(userIdStr), "user", prompt, null);
        }

        String answer;

        // ★ 第一步：先查 FAQ 資料庫，看有沒有匹配的答案
        List<Faq> matchedFaqs = faqRepository.findAllByOrderByIdAsc();
        String faqAnswer = null;
        for (Faq faq : matchedFaqs) {
            if (prompt.contains(faq.getQuestion()) || faq.getQuestion().contains(prompt)) {
                faqAnswer = faq.getAnswer();
                break;
            }
        }

        if (faqAnswer != null) {
            answer = faqAnswer;
        } else {
            answer = geminiService.askGemini(prompt);
        }

        // 儲存機器人回覆
        if (userIdStr != null) {
            saveHistory(Integer.parseInt(userIdStr), "bot", answer, null);
        }

        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);

        return ResponseEntity.ok(response);
    }

    // ── 取得 FAQ 清單（從資料庫） ──
    @GetMapping("/faq")
    public ResponseEntity<?> getFaqList() {
        List<Faq> faqs = faqRepository.findAllByOrderByIdAsc();
        return ResponseEntity.ok(faqs);
    }

    // ── 查詢會員最近訂單 ──
    @GetMapping("/my-orders/{memberId}")
    public ResponseEntity<?> getMyOrders(@PathVariable Integer memberId) {
        List<Order> orders = orderRepository
                .findByMemberIdAndIsDeletedFalseOrderByOrderDateDesc(memberId);

        List<Map<String, Object>> result = orders.stream()
                .limit(3)
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderId", order.getOrderId());
                    map.put("orderDate", order.getOrderDate() != null
                            ? order.getOrderDate().toString().substring(0, 10) : "未知");
                    map.put("orderTotal", order.getOrderTotal());
                    map.put("orderStatus", order.getOrderStatus());
                    map.put("items", order.getDetails().stream()
                            .map(d -> d.getProductName() + " x" + d.getQuantity())
                            .collect(Collectors.joining("、")));
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── 查詢聊天紀錄 ──
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getChatHistory(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "50") Integer size) {
        // 修改：使用分頁查詢最新的 N 筆，避免一次抓取過多
        List<ChatHistory> history = chatHistoryRepository.findMemberHistoryPaged(userId, PageRequest.of(0, size));
        
        // 因為是 DESC 排序（最新在前），回傳前要反轉回來符合時間線
        Collections.reverse(history);
        
        return ResponseEntity.ok(history);
    }

    // ══════════════════════════════════════════════
    // 真人客服 Session API（新功能）
    // ══════════════════════════════════════════════

    // ── 消費者：建立真人客服 Session ──
    @PostMapping("/session/create")
    public ResponseEntity<?> createSession(@RequestBody Map<String, String> payload) {
        Integer memberId = Integer.parseInt(payload.get("memberId"));
        String memberName = payload.get("memberName");

        // 檢查是否已有未關閉的 Session
        ChatSession existing = chatSessionRepository
                .findFirstByMemberIdAndStatusNotOrderByCreatedAtDesc(memberId, "closed");
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }

        ChatSession session = new ChatSession();
        session.setMemberId(memberId);
        session.setMemberName(memberName);
        session.setStatus("waiting");
        chatSessionRepository.save(session);

        // 自動新增一則系統訊息
        saveHistory(memberId, "bot", "您已進入真人客服排隊中，請稍候...\n客服人員接手後會立即回覆您！", session.getSessionId());

        return ResponseEntity.ok(session);
    }

    // ── 消費者/客服：發送訊息 ──
    @PostMapping("/session/{sessionId}/send")
    public ResponseEntity<?> sendMessage(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> payload) {
        String role = payload.get("role");       // "user" 或 "staff"
        String message = payload.get("message");
        Integer userId = Integer.parseInt(payload.get("userId"));

        ChatHistory record = new ChatHistory();
        record.setUserId(userId);
        record.setRole(role);
        record.setMessage(message);
        record.setSessionId(sessionId);
        chatHistoryRepository.save(record);

        // 更新 session 的 updatedAt
        chatSessionRepository.findById(sessionId).ifPresent(s -> {
            s.setUpdatedAt(java.time.LocalDateTime.now());
            chatSessionRepository.save(s);
        });

        return ResponseEntity.ok(record);
    }

    // ── 查詢 Session 的所有訊息 ──
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getSessionMessages(@PathVariable Integer sessionId) {
        List<ChatHistory> messages = chatHistoryRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);
        return ResponseEntity.ok(messages);
    }

    // ── 輪詢：取得新訊息（ID 大於 lastId 的） ──
    @GetMapping("/session/{sessionId}/poll")
    public ResponseEntity<?> pollMessages(
            @PathVariable Integer sessionId,
            @RequestParam(defaultValue = "0") Integer lastId) {

        List<ChatHistory> newMessages = chatHistoryRepository
                .findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(sessionId, lastId);

        // 同時回傳 Session 狀態
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("messages", newMessages);
        result.put("session", session);

        return ResponseEntity.ok(result);
    }

    // ── 後台：取得當前活躍 Session 列表（按會員分組） ──
    @GetMapping("/session/waiting")
    public ResponseEntity<?> getWaitingSessions() {
        // 取得所有會員最新的 Session
        List<ChatSession> allLatest = chatSessionRepository
                .findLatestSessionsGroupByMember();
        
        // 只保留「等待中」或「對話中」的會員
        List<ChatSession> activeOnes = allLatest.stream()
                .filter(s -> Arrays.asList("waiting", "active").contains(s.getStatus()))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(activeOnes);
    }

    // ── 後台：取得歷史已關閉 Session 列表（按會員分組） ──
    @GetMapping("/session/closed")
    public ResponseEntity<?> getClosedSessions() {
        // 使用我們新寫的分組查詢
        List<ChatSession> sessions = chatSessionRepository
                .findLatestClosedSessionsGroupByMember();
        return ResponseEntity.ok(sessions);
    }

    // ── 輪詢：按會員 ID 取得新訊息（ID 大於 lastId 的） ──
    @GetMapping("/member/{memberId}/poll")
    public ResponseEntity<?> pollMemberMessages(
            @PathVariable Integer memberId,
            @RequestParam(defaultValue = "0") Integer lastId) {

        // 優化：直接在 SQL 過濾 lastId，效能大幅提升
        List<ChatHistory> newMessages = chatHistoryRepository
                .findNewMessagesByMember(memberId, lastId);

        // 取得該會員目前的活躍 Session (若有)
        ChatSession session = chatSessionRepository
                .findFirstByMemberIdAndStatusNotOrderByCreatedAtDesc(memberId, "closed");

        Map<String, Object> result = new HashMap<>();
        result.put("messages", newMessages);
        result.put("session", session);

        return ResponseEntity.ok(result);
    }

    // ── 後台：客服接手 Session ──
    @PutMapping("/session/{sessionId}/accept")
    public ResponseEntity<?> acceptSession(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> payload) {

        Integer empId = Integer.parseInt(payload.get("empId"));
        String empName = payload.getOrDefault("empName", "客服人員");

        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        session.setStatus("active");
        session.setAssignedEmpId(empId);
        chatSessionRepository.save(session);

        // 自動新增一則系統訊息通知消費者
        saveHistory(session.getMemberId(), "bot",
                "🎉 客服人員「" + empName + "」已加入對話，請問有什麼可以幫您的嗎？",
                sessionId);

        return ResponseEntity.ok(session);
    }

    // ── 後台：關閉 Session ──
    @PutMapping("/session/{sessionId}/close")
    public ResponseEntity<?> closeSession(@PathVariable Integer sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        session.setStatus("closed");
        chatSessionRepository.save(session);

        // 自動新增一則結束訊息
        saveHistory(session.getMemberId(), "bot",
                "本次對話已結束，感謝您的耐心等候！\n如有其他問題歡迎隨時再次聯繫我們 🐾",
                sessionId);

        return ResponseEntity.ok(session);
    }

    // ══════════════════════════════════════════════
    // 內部方法
    // ══════════════════════════════════════════════

    private void saveHistory(Integer userId, String role, String message, Integer sessionId) {
        try {
            ChatHistory record = new ChatHistory();
            record.setUserId(userId);
            record.setRole(role);
            record.setMessage(message);
            record.setSessionId(sessionId);
            chatHistoryRepository.save(record);
        } catch (Exception e) {
            System.err.println("儲存聊天紀錄失敗: " + e.getMessage());
        }
    }
}
