package com.petlife.service;

import java.util.Optional;

import com.petlife.model.Member;
import com.petlife.repository.LoginRequest;
import com.petlife.repository.MemberUpdateRequest;
import com.petlife.repository.RegisterRequest;

public interface IMemberService {
	
	Member register(RegisterRequest request);
	
	Optional<Member> login(LoginRequest request);
	
	 Optional<String> loginAndGenerateToken(LoginRequest requset);
	 
	 
	 	// 新增：修改會員資料 4/28 新增
	    Member updateMember(MemberUpdateRequest request);

	    // 新增：檢查 Email 是否可用
	    boolean isEmailAvailable(String email);

	    // 新增：檢查 Phone 是否可用
	    boolean isPhoneAvailable(String phone);
	    
	    // 新增：依 ID 查詢會員
	    Optional<Member> findById(Integer id);
	    
	    //更新大頭貼
	    Member updateMemberImage(Integer memberId, String newImagePath);
	 
}
