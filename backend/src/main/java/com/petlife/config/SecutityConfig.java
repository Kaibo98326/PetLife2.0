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
					.requestMatchers("/api/pets/**").permitAll()
					.requestMatchers("/callback/**").permitAll()
					.requestMatchers("/images/**").permitAll()
					.requestMatchers("/api/admin/**").permitAll()
					.requestMatchers("/api/employee/login").permitAll() // ✅ 員工登入 API 開放
					.requestMatchers("/api/oauth2/**").permitAll()	//預留OAuth2
	                .requestMatchers("/api/member/me").permitAll()
	                .requestMatchers("/api/beauty/**").permitAll()
	                .requestMatchers("/api/categories", "/api/categories/**").permitAll() 
	                .requestMatchers("/api/products", "/api/products/**").permitAll()
	                .requestMatchers("/api/shop", "/api/shop/**").permitAll()
					.requestMatchers("/error").permitAll()
					.requestMatchers("/api/cart/**").permitAll()
	                .requestMatchers("/api/orders/**").permitAll()
	                .requestMatchers("/api/checkout/**").permitAll()
	                .requestMatchers("/api/checkoutsuccess/**").permitAll()
	                .requestMatchers("/api/productorders/**").permitAll()
	                .requestMatchers("/api/order/**").permitAll()
	                .requestMatchers("/api/history/**").permitAll()
	                
					.anyRequest().authenticated()
					);
		return http.build();
	}
}
