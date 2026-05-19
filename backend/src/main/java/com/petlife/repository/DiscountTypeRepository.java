package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.petlife.model.DiscountType;

@Repository
public interface DiscountTypeRepository extends JpaRepository<DiscountType, Integer> {
    // 裡面什麼都不用寫！
    // 只要繼承了 JpaRepository，Spring 就會自動送你 findAll() 和 findById() 讓你做查詢。
    // 至於 save() 和 delete()，我們在 Service 裡面刻意不去呼叫它們就好了。
}