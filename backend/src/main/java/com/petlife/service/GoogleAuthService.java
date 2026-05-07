package com.petlife.service;




import com.petlife.repository.GoogleUserInfo;
import com.petlife.repository.LoginResponse;
import com.petlife.model.Auth;
import com.petlife.model.Member;
import com.petlife.repository.AuthRepository;
import com.petlife.repository.MemberRepository;
import com.petlife.repository.UserResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {
	
	private final MemberRepository memberRepository;
	private final AuthRepository authRepository;
	private final JwtUtils jwtUtils;
	
	
	// ✅ 用 code 換取 access_token
	public String exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri) {
	    RestTemplate restTemplate = new RestTemplate();
	    String tokenUrl = "https://oauth2.googleapis.com/token";

	    // ✅ 使用 MultiValueMap 確保是 x-www-form-urlencoded 格式
	    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
	    params.add("code", code);
	    params.add("client_id", clientId);
	    params.add("client_secret", clientSecret);
	    params.add("redirect_uri", redirectUri);
	    params.add("grant_type", "authorization_code");

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

	    // ✅ 呼叫 Google Token API
	    Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);

	    if (response == null || response.get("access_token") == null) {
	        throw new RuntimeException("Failed to exchange code for access token: " + response);
	    }

	    return (String) response.get("access_token");
	}

    // ✅ 用 access_token 呼叫 Google UserInfo API
    public GoogleUserInfo getUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String userInfoUrl = "https://openidconnect.googleapis.com/v1/userinfo";
        return restTemplate.getForObject(userInfoUrl + "?access_token=" + accessToken, GoogleUserInfo.class);
    }

 // ✅ 登入或註冊會員 → 簽發 JWT
    public LoginResponse loginOrRegister(GoogleUserInfo userInfo) {
        Auth auth = authRepository.findByProviderAndProviderUserId("google", userInfo.getSub()).orElse(null);
        Member member;
        boolean mustSetPassword = false;

        if (auth != null) {
            // 舊會員 → 更新最後登入時間
        	member = auth.getMember();
        		
        		if ("disable".equals(member.getAccountStatus())) {
        			throw new IllegalStateException("此帳號已停權，請聯繫客服");
        		}

        		if ("delete".equals(member.getAccountStatus())) {
        			throw new IllegalStateException("此帳號已刪除，無法登入");
        		}

        		if (!"active".equals(member.getAccountStatus())) {
        			throw new IllegalStateException("此帳號狀態異常，請聯繫客服");
        		}
        		
            member.setLastLogin(LocalDateTime.now());
            member.setProvider("google");
            member.setProviderUserId(userInfo.getSub());
            memberRepository.save(member);

            auth.setLastLoginAt(LocalDateTime.now());
            authRepository.save(auth);
        } else {
            // 新會員 → 建立 Member
            member = Member.builder()
                    .memberName(userInfo.getName())
                    .email(userInfo.getEmail())
                    .userImage(userInfo.getPicture())
                    .passwordHash(PasswordUtils.hashPassword("1234"))
                    .provider("google")
                    .providerUserId(userInfo.getSub())
                    .registerTime(LocalDateTime.now())
                    .accountStatus("active")
                    .bonusPoints(0)
                    .build();
            member.setProvider("google");
            member.setProviderUserId(userInfo.getSub());
            member.setLastLogin(LocalDateTime.now());
            memberRepository.save(member);

            auth = Auth.builder()
                    .member(member)
                    .provider("google")
                    .providerUserId(userInfo.getSub())
                    .createAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            authRepository.save(auth);

            // 新會員 → 強制設定密碼
            mustSetPassword = true;
        }

        String jwt = jwtUtils.generateToken(
                member.getMemberId(),
                mustSetPassword
        );

        UserResponse userResponse = new UserResponse(
                member.getMemberId(),
                member.getMemberName(),
                member.getEmail(),
                member.getUserImage()
        );

        return new LoginResponse(jwt, userResponse);
    }

	
	
}
