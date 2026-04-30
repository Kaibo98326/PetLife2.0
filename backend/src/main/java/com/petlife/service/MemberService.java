package com.petlife.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.petlife.model.Member;
import com.petlife.repository.LoginRequest;
import com.petlife.repository.MemberRepository;
import com.petlife.repository.MemberUpdateRequest;
import com.petlife.repository.RegisterRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService implements IMemberService{
	
	private final MemberRepository memberRepos;
	
	private final JwtUtils jwtUtils;
	
	//註冊
	@Override
	public Member register(RegisterRequest request) {
		//檢查email
		if(memberRepos.findByEmail(request.getEmail()).isPresent()) {
			throw new IllegalArgumentException("此電子郵件已被使用");
		}
		
		//檢查手機
		if(memberRepos.findByPhone(request.getPhone()).isPresent()) {
			throw new IllegalArgumentException("此電話已被使用");
		}
		
		
		//建立會員
		Member member = new Member();
		
		member.setMemberName(request.getMemberName());
		member.setEmail(request.getEmail());
		member.setPasswordHash(PasswordUtils.hashPassword(request.getPassword()));
		member.setRegisterTime(LocalDateTime.now());
		member.setPhone(request.getPhone());
		member.setAddress(request.getAddress());
		member.setAccountStatus("active");
		member.setUserImage("/images/member/petlife.jpg");
		member.setBonusPoints(0);
		
		return memberRepos.save(member);
	}
	
	//登入
	@Override
	public Optional<Member> login(LoginRequest request){
		return memberRepos.findByEmail(request.getEmail())
				.filter( m -> PasswordUtils.checkPassword(request.getPassword(), m.getPasswordHash()));
	}
	
	
	//正式登入用
	@Override
    public Optional<String> loginAndGenerateToken(LoginRequest request) {
        return memberRepos.findByEmail(request.getEmail())
                .filter(m -> PasswordUtils.checkPassword(request.getPassword(), m.getPasswordHash()))
                .map(m -> {
                	 m.setLastLogin(LocalDateTime.now());
                	 memberRepos.save(m);
                	return jwtUtils.generateToken(m.getMemberId(),m.getEmail(),m.getMemberName(),m.getUserImage());
                	}); // 用設定檔的 secret 來簽發
    }
	
	//修改會員資料(會員端)
	@Override
	public Member updateMember(MemberUpdateRequest req) {
		
		Member member = memberRepos.findById(req.getMemberId())
						.orElseThrow(() -> new  IllegalArgumentException("會員不存在"));
		//檢查email
		memberRepos.findByEmail(req.getEmail())
					.filter(m -> !m.getMemberId().equals(req.getMemberId()))
					.ifPresent(m -> {throw new IllegalArgumentException("此電子郵件已被使用");});
		
		//檢查電話
		memberRepos.findByPhone(req.getPhone())
					.filter(m -> !m.getPhone().equals(req.getPhone()))
					.ifPresent(m ->{throw new IllegalArgumentException("此電話已被使用");});
		
		//可修改欄位
		member.setPhone(req.getPhone());
		member.setEmail(req.getEmail());
		member.setAddress(req.getAddress());
		
		if(req.getPassword() != null && !req.getPassword().isEmpty()) {
			member.setPasswordHash(PasswordUtils.hashPassword(req.getPassword()));
		}
		
		if(member.getProvider() != null && member.getProviderUserId() != null) {
			member.setProvider(req.getProvider());
			member.setProviderUserId(req.getProviderUserId());
		}
		// 4/29更新使用者大頭貼功能
		if(req.getUserImage() != null && !req.getUserImage().isEmpty()) {
			member.setUserImage(req.getUserImage());
		}
		
		
		return memberRepos.save(member);
		
		
		
	}
	@Override
	public boolean isEmailAvailable(String email) {
	    return memberRepos.findByEmail(email).isEmpty();
	}
	@Override
	public boolean isPhoneAvailable(String phone) {
	    return memberRepos.findByPhone(phone).isEmpty();
	}
	
	@Override
	public Optional<Member> findById(Integer id) {
	    return memberRepos.findById(id);
	}
	
	@Override
	public Member updateMemberImage(Integer memberId, String newImagePath) {
	    Member member = memberRepos.findById(memberId)
	        .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

	    // 刪除舊檔案 (避免刪掉預設頭像)
	    if (member.getUserImage() != null && !member.getUserImage().equals("/images/member/petlife.jpg")) {
	        try {
	            String oldFileName = Paths.get(member.getUserImage()).getFileName().toString();
	            Path oldPath = Paths.get("C:/PetLife2.0/uploads/images/member/").resolve(oldFileName);
	            Files.deleteIfExists(oldPath);
	        } catch (Exception e) {
	            System.err.println("刪除舊頭像失敗: " + e.getMessage());
	        }
	    }

	    // 更新資料庫欄位
	    member.setUserImage(newImagePath);
	    return memberRepos.save(member);
	}
	
	
	
	
	
	
	
	
	
	
	
}
