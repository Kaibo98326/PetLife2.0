package com.petlife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecutityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http)throws Exception {
		http.cors(cors ->{})
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/member/**").permitAll() //會員API開放
					.requestMatchers("/api/employee/login").permitAll() // ✅ 員工登入 API 開放
					.requestMatchers("/oauth2/**").permitAll()	//預留OAuth2
					.requestMatchers("/images/**").permitAll()  //靜態圖片放行
					.anyRequest().authenticated()
					);
		return http.build();
	}
}
