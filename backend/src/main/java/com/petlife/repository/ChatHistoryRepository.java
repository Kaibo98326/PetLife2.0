package com.petlife.repository;

import com.petlife.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {

    // 查詢某位使用者的聊天紀錄（按時間正序）
    List<ChatHistory> findByUserIdOrderByCreatedAtAsc(Integer userId);

    // ── 查詢該會員所有的對話（包含 AI 回覆、本人訊息、以及客服回覆） ──
    @org.springframework.data.jpa.repository.Query("SELECT h FROM ChatHistory h " +
            "LEFT JOIN ChatSession s ON h.sessionId = s.sessionId " +
            "WHERE h.userId = :memberId OR s.memberId = :memberId " +
            "ORDER BY h.createdAt ASC")
    List<ChatHistory> findAllByMemberId(@org.springframework.data.repository.query.Param("memberId") Integer memberId);

    // ── 優化 1：分頁查詢（抓取最新的 N 筆） ──
    @org.springframework.data.jpa.repository.Query("SELECT h FROM ChatHistory h " +
            "LEFT JOIN ChatSession s ON h.sessionId = s.sessionId " +
            "WHERE h.userId = :memberId OR s.memberId = :memberId " +
            "ORDER BY h.createdAt DESC")
    List<ChatHistory> findMemberHistoryPaged(@org.springframework.data.repository.query.Param("memberId") Integer memberId, org.springframework.data.domain.Pageable pageable);

    // ── 優化 3：高效輪詢（直接在 SQL 過濾 lastId） ──
    @org.springframework.data.jpa.repository.Query("SELECT h FROM ChatHistory h " +
            "LEFT JOIN ChatSession s ON h.sessionId = s.sessionId " +
            "WHERE (h.userId = :memberId OR s.memberId = :memberId) AND h.id > :lastId " +
            "ORDER BY h.createdAt ASC")
    List<ChatHistory> findNewMessagesByMember(@org.springframework.data.repository.query.Param("memberId") Integer memberId, @org.springframework.data.repository.query.Param("lastId") Integer lastId);

    // 查詢某位使用者最近 N 筆紀錄
    List<ChatHistory> findTop20ByUserIdOrderByCreatedAtDesc(Integer userId);

    // 依 Session 查詢訊息（按時間正序）
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(Integer sessionId);

    // 依 Session 查詢 ID 大於某值的新訊息（輪詢用）
    List<ChatHistory> findBySessionIdAndIdGreaterThanOrderByCreatedAtAsc(Integer sessionId, Integer lastId);

    // 依 會員 ID 查詢 ID 大於某值的新訊息（跨 Session 輪詢用）
    List<ChatHistory> findByUserIdAndIdGreaterThanOrderByCreatedAtAsc(Integer userId, Integer lastId);
}
