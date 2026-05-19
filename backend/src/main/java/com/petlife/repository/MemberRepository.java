package com.petlife.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
	
	//依照Email 找會員
	Optional<Member> findByEmail(String email);
	
	//依照電話找會員
	Optional<Member> findByPhone(String phone);
	
	//模糊搜尋會員姓名
	List<Member> findByMemberNameContaining(String keyword);
	
	//以下為員工端使用
	//分頁形式(模糊搜尋姓名)
	Page<Member> findByMemberNameContaining(String keyword, Pageable pageable);
	//分頁形式(模糊搜尋eamil)
	Page<Member> findByEmailContaining(String keyword, Pageable pageable);
	//分頁形式(模糊搜尋會員末三碼)
	Page<Member> findByPhoneEndingWith(String keyword, Pageable pageable);
	//分頁形式(搜尋帳號狀態)
	Page<Member> findByAccountStatus(String accountStatus, Pageable pageable);
	//分頁形式(搜尋是否為第三方登入帳號)
	Page<Member> findByProvider(String provider, Pageable pageable);
	//分頁形式(搜尋全部會員)
	Page<Member> findAll(Pageable pageable);
	//分頁形式(搜尋本地端會員)
	Page<Member> findByProviderIsNull(Pageable pageable);
	
	//後台會員狀態分析
	long countByAccountStatus(String accountStatus);
	//後台會員登入來源分析
	long countByProvider(String provider);
	
	
	//  新增/修改：扣除紅利點數，確保剩餘點數足夠
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.bonusPoints = m.bonusPoints - :points " +
           "WHERE m.memberId = :memberId AND m.bonusPoints >= :points")
    int deductBonusPoints(@Param("memberId") Integer memberId, @Param("points") Integer points);
	
 // 增加紅利點數 (用於訂單完成發放、或取消退回)
    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.bonusPoints = m.bonusPoints + :points WHERE m.memberId = :memberId")
    int addBonusPoints(@Param("memberId") Integer memberId, @Param("points") Integer points);
    
    
	//每月註冊趨勢圖
	@Query(value = """
		    SELECT 
		        FORMAT(register_time, 'yyyy-MM') AS month,
		        COUNT(*) AS count
		    FROM Member
		    GROUP BY FORMAT(register_time, 'yyyy-MM')
		    ORDER BY FORMAT(register_time, 'yyyy-MM')
		""", nativeQuery = true)
	List<Object[]> getMonthlyRegisterStatsRaw();
	
	@Query(value = """
		    SELECT *
		    FROM member
		    WHERE FORMAT(register_time, 'yyyy-MM') = :month
		""", nativeQuery = true)
	List<Member> findMembersByRegisterMonth(
		        @Param("month") String month);
}
