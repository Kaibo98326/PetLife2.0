package com.petlife.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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
		List<Stay> findOverlappingStays(
		        @Param("roomTypeId") Integer roomTypeId,
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate
		    );
	
	// ========== 新增方法（後台搜尋）==========
	
		/**
		 * 後台訂單搜尋（複雜查詢）
		 */
		@Query("SELECT s FROM Stay s " +
		       "WHERE 1=1 " +
		       "AND (:stayId IS NULL OR s.stayId = :stayId) " +
		       "AND (:stayStatus IS NULL OR s.stayStatus = :stayStatus) " +
		       "AND (:memberName IS NULL OR s.pet.member.memberName LIKE %:memberName%) " +
		       "AND (:memberPhone IS NULL OR s.pet.member.phone LIKE %:memberPhone%) " +
		       "AND (:startDate IS NULL OR s.stayStartDate >= :startDate) " +
		       "AND (:endDate IS NULL OR s.stayEndDate <= :endDate)")
		Page<Stay> searchStays(
		        @Param("stayId") Integer stayId,
		        @Param("stayStatus") String stayStatus,
		        @Param("memberName") String memberName,
		        @Param("memberPhone") String memberPhone,
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate,
		        Pageable pageable
		);
	
}
