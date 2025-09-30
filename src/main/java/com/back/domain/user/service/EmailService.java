package com.back.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

// 이메일 전송 서비스
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String FROM_ADDRESS;

    @Value("${frontend.base-url}")
    private String FRONTEND_BASE_URL;

    // 이메일 인증 메일 전송
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "[Catfe] 이메일 인증 안내";
        String verificationUrl = FRONTEND_BASE_URL + "/verify-email?token=" + token;

        String htmlContent = """
                <p>안녕하세요, Catfe에 가입해 주셔서 감사합니다.</p>
                <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>
                <br>
                <p>
                    <a href="%s" style="display:inline-block;padding:10px 20px;
                    background-color:#4CAF50;color:#fff;text-decoration:none;
                    border-radius:5px;">이메일 인증하기</a>
                </p>
                <br>
                <p>이 링크는 24시간 동안만 유효합니다.</p>
                """.formatted(verificationUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    // HTML 이메일 전송 공통 메서드
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true → HTML 모드

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }
}