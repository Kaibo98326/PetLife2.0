package com.petlife.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.petlife.model.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class EmpJwtUtil {
	
	@Value("${jwt.secret}")
	private String secret;
	@Value("${jwt.expiration}")
	private long expiration;
	
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}
	
	//員工登入用:包含 empId , username , empName
	public String generateToken(Integer empId, String username, String empName , List<Role> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empName", empName);
        claims.put("username", username);
        claims.put("roles" , roles.stream().map(Role::getRoleName).toList());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(empId)) // sub = 員工編號
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 驗證 Token，回傳 empId
    public String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject(); // 這裡就是 empId
        } catch (JwtException e) {
            return null;
        }
    }

    // 額外方法：解析 Token 取出 empName
    public String extractEmpName(String token) {
        try {
            return (String) Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("empName");
        } catch (JwtException e) {
            return null;
        }
    }

    // 額外方法：解析 Token 取出 username
    public String extractUsername(String token) {
        try {
            return (String) Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("username");
        } catch (JwtException e) {
            return null;
        }
    }
	
	
}
