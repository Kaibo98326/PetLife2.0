package com.petlife.repository;

import com.petlife.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Integer> {

    // 依分類查詢
    List<Faq> findByCategoryOrderByIdAsc(String category);

    // 查詢所有（按 ID 排序）
    List<Faq> findAllByOrderByIdAsc();

    // 模糊搜尋：使用者輸入的問題是否包含在 FAQ 的 question 中
    List<Faq> findByQuestionContaining(String keyword);
}
