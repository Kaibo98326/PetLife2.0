package com.petlife.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {
	
	
	private final JavaMailSender mailSender;
	
	public void sendResetPasswordEmail(String toEmail,String resetLink) {
		
		SimpleMailMessage message = new SimpleMailMessage();
		
		message.setTo(toEmail);
		message.setSubject("Petlife 重設密碼");
		
		message.setText(
				"您好:\n\n"
				+"請點擊以下連結重設密碼:\n\n"
				+resetLink
				+"\n\n"
				+"此連結將於5分鐘後失效。"
				);
		
		mailSender.send(message);
		
	}
}
