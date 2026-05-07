package com.petlife.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.petlife.repository.MemberRepository;
import com.petlife.repository.MonthlyRegisterStatsDto;
import com.petlife.repository.UpdateMemberRequest;
import com.petlife.model.Member;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
	
	
	private final MemberRepository memberRepository;
	
	
	//會員分頁+搜尋
	public Map<String ,Object> getMembers(int page , int size , String searchType , String keyword){
		
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Member> result;
		
		//沒有搜尋條件
		if(searchType == null || keyword == null || keyword.isBlank()) {
			
			result = memberRepository.findAll(pageable);
			
		}else {
			
			switch(searchType) {
				
				//會員姓名
			  	case "memberName":
			  		
			  		result = memberRepository.findByMemberNameContaining(keyword, pageable);
			  		break;
			  	//手機末三碼	
			  	case "phoneLast3":
			  		
			  		result = memberRepository.findByPhoneEndingWith(keyword, pageable);
			  		break;
			  		
			  	case "email":
			  		result = memberRepository.findByEmailContaining(keyword, pageable);
			  		break;
			  	//帳號狀態
			  	case "accountStatus":
			  		
			  		result = memberRepository.findByAccountStatus(keyword, pageable);
			  		break;
			  	//第三方登入來源
			  	case "provider":
			  		
			  		//本地帳號
			  		if("local".equals(keyword)) {
			  			
			  			result = memberRepository.findByProviderIsNull(pageable);
			  		}else {
			  			result = memberRepository.findByProvider(keyword, pageable);
			  		}
			  		
			  		break;
			  		
			  	default:
			  		
			  		result = memberRepository.findAll(pageable);
			}
			
		}
		
		return Map.of(
					"content" , result.getContent(),
					"currenPage",result.getNumber(),
					"totalPages",result.getTotalPages(),
					"totalElements",result.getTotalElements(),
					"size",result.getSize()
				
				);
		
	}
	
	//快速修改會員狀態
	public void updateMemberStatus(Integer memberId , String accountStatus) {
		
		Member member = memberRepository.findById(memberId)
				.orElseThrow( () -> new RuntimeException("會員不存在"));
		
		member.setAccountStatus(accountStatus);
		
		memberRepository.save(member);
	}
	
	//修改會員資料
	public void updateMember(Integer memberId, UpdateMemberRequest request ) {
		
		Member member = memberRepository.findById(memberId)
						.orElseThrow(() -> new RuntimeException("會員不存在"));
		
		member.setMemberName(request.getMemberName());
		member.setPhone(request.getPhone());
		member.setAddress(request.getAddress());
		member.setAccountStatus(request.getAccountStatus());
		
		memberRepository.save(member);
		
	}
	
	//會員狀態分析
	public Map<String , Long> getMemberStatusAnalysis(){
		
		long active = memberRepository.countByAccountStatus("active");
		
		long disable = memberRepository.countByAccountStatus("disable");
		
		long delete = memberRepository.countByAccountStatus("delete");
		
		return Map.of(
				"active"  , active,
				"disable" , disable,
				"delete"  , delete
				);
	}
	//會員登入來源分析
	public Map<String, Long> getProviderAnalysis(){
		
		long local = memberRepository.countByProvider("local");
		
		long google = memberRepository.countByProvider("google");
		
		
		return Map.of(
				"local",local,
				"google",google
				);
		
	}
	//會員每月註冊分析
	public List<MonthlyRegisterStatsDto> getMonthlyRegisterStats(){
		return memberRepository.getMonthlyRegisterStatsRaw()
				.stream()
				.map(row -> new MonthlyRegisterStatsDto(
						row[0].toString(),
						((Number) row[1]).longValue()
				)).toList();
	}
	//月份切換
	public List<Member> getMembersByRegisterMonth(String month){
		
		return memberRepository.findMembersByRegisterMonth(month);
	}
	
	
	
	
	
	
}	
