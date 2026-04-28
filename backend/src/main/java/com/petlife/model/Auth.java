package com.petlife.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth")
@Data                   // 自動生成 getter/setter/toString/equals/hashCode
@NoArgsConstructor      // 無參數建構子
@AllArgsConstructor     // 全參數建構子
@Builder                // Builder 模式
public class Auth {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Integer authId;   // 綁定編號

    @ManyToOne(fetch = FetchType.LAZY)   // 多個 Auth 對應一個 Member
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;    // 會員編號 (外鍵)

    @Column(nullable = false, length = 50)
    private String provider;  // 登入來源 (Google, Line, Facebook...)

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;   // 第三方使用者ID

    @Column(name = "unique_identifier", length = 100)
    private String uniqueIdentifier; // 唯一識別碼

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
