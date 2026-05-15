package com.petlife.repository;

import com.petlife.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {

    // 查詢等待中的 Session（後台客服用）
    List<ChatSession> findByStatusOrderByCreatedAtAsc(String status);

    // 查詢已結束的 Session（歷史紀錄用）
    List<ChatSession> findByStatusOrderByCreatedAtDesc(String status);

    // 查詢等待中 + 進行中的 Session（後台客服總覽）
    List<ChatSession> findByStatusInOrderByCreatedAtAsc(List<String> statuses);

    // 查詢某客服負責的 Session
    List<ChatSession> findByAssignedEmpIdAndStatusOrderByUpdatedAtDesc(Integer empId, String status);

    // 查詢消費者未關閉的 Session
    ChatSession findFirstByMemberIdAndStatusNotOrderByCreatedAtDesc(Integer memberId, String status);

    // ── 新增：按會員分組，取得每個會員最新的一個 Session ──
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChatSession s WHERE s.sessionId IN " +
            "(SELECT MAX(s2.sessionId) FROM ChatSession s2 GROUP BY s2.memberId) " +
            "ORDER BY s.updatedAt DESC")
    List<ChatSession> findLatestSessionsGroupByMember();

    // ── 新增：按會員分組，取得每個會員最新的一個「已結束」Session ──
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChatSession s WHERE s.status = 'closed' AND s.sessionId IN " +
            "(SELECT MAX(s2.sessionId) FROM ChatSession s2 WHERE s2.status = 'closed' GROUP BY s2.memberId) " +
            "ORDER BY s.updatedAt DESC")
    List<ChatSession> findLatestClosedSessionsGroupByMember();
}
