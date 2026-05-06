package com.petlife.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petlife.model.Stay;

@Repository
public interface StayRepository extends JpaRepository<Stay, Integer>{

	//從寵物取得會員ID
	List<Stay> findByPet_Member_MemberId(Integer pet_Member_MemberId);
	
	//電話末三碼
	List<Stay> findByPet_Member_PhoneEndingWith(String phone);
	
	// 查日期區間重疊的預約
	// 查這段日期內，這個房型已被預約的房間
	// 預約區間重疊的條件：
	// 已有預約的 startDate < 我的 endDate
	// 已有預約的 endDate > 我的 startDate
	// 且 stayStatus != '取消' 的選項
    @Query("SELECT s FROM Stay s " +
            "WHERE s.stayRoomType.roomTypeId = :roomTypeId " +
            "AND s.stayStartDate < :endDate " +
            "AND s.stayEndDate > :startDate " +
            "AND s.stayStatus != 'CANCELLED'")
    // @Param 對應到 :Bean 的欄位 然後轉換成 
    // 尋找重疊的住宿紀錄 
    List<Stay> findOverlappingStays(
    	    @Param("roomTypeId") Integer roomTypeId,
    	    @Param("startDate") LocalDate startDate,
    	    @Param("endDate") LocalDate endDate
    	);
}
