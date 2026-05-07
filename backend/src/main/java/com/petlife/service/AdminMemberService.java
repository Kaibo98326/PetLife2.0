package com.petlife.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.petlife.repository.MemberRepository;
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
	
	
}	
