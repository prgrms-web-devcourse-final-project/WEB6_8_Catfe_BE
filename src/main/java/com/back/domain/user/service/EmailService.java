package com.back.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String FROM_ADDRESS;

    @Value("${frontend.base-url}")
    private String FRONTEND_BASE_URL;

    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "[Catfe] 이메일 인증 안내";
        String verificationUrl = FRONTEND_BASE_URL + "/verify-email?token=" + token;

        String text = """
                안녕하세요, Catfe에 가입해 주셔서 감사합니다.

                아래 링크를 클릭하여 이메일 인증을 완료해주세요:
                %s

                이 링크는 24시간 동안만 유효합니다.
                """.formatted(verificationUrl);

        sendEmail(toEmail, subject, text);
    }

    private void sendEmail(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
