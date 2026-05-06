package com.petlife.controller;

import com.petlife.repository.GoogleUserInfo;
import com.petlife.repository.LoginResponse;
import com.petlife.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GoogleOAuthController {

	 private final GoogleAuthService googleAuthService;

	 @Value("${google.client-id}")
	 private String clientId;

	 @Value("${google.client-secret}")
	 private String clientSecret;

	 @Value("${google.redirect-uri}")
	 private String redirectUri;

	    @GetMapping("/callback/google")
	    public ResponseEntity<LoginResponse> googleLogin(@RequestParam String code) {

	        String accessToken = googleAuthService.exchangeCodeForToken(
	                code,
	                clientId,
	                clientSecret,
	                redirectUri
	        );

	        GoogleUserInfo userInfo =
	                googleAuthService.getUserInfo(accessToken);

	        LoginResponse response =
	                googleAuthService.loginOrRegister(userInfo);

	        return ResponseEntity.ok(response);
	    }
}

