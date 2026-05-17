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
		       "JOIN StayPayment p ON p.stay.stayId = s.stayId " +
		       "WHERE s.stayRoomType.roomTypeId = :roomTypeId " +
		       "AND s.stayStartDate < :endDate " +
		       "AND s.stayEndDate > :startDate " +
		       "AND s.stayStatus != 'CANCELLED' " +
		       "AND p.paymentStatus = 'SUCCESS'")
		List<Stay> findOverlappingStays(
		        @Param("roomTypeId") Integer roomTypeId,
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate
		);
	
	// 依照會員名字搜尋
	@Query("SELECT s FROM Stay s WHERE s.pet.member.memberName LIKE %:name%")
	List<Stay> findByMemberName(@Param("name") String name);

	// 依照訂單編號搜尋
	List<Stay> findByStayId(Integer stayId);

	
	//	不過濾 roomTypeId 的空房狀態
	@Query("SELECT s FROM Stay s " +
		       "WHERE s.stayStartDate < :endDate " +
		       "AND s.stayEndDate > :startDate " +
		       "AND s.stayStatus != 'CANCELLED'")
		List<Stay> findAllOverlappingStays(
		        @Param("startDate") LocalDate startDate,
		        @Param("endDate") LocalDate endDate
		);
	// 今日預約
	List<Stay> findByStayStartDate(LocalDate date);
}
