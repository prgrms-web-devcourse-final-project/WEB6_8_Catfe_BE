package com.back.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("이메일 인증 메일 전송 성공")
    void sendVerificationEmail() {
        // given
        String toEmail = "test@example.com";
        String token = "sample-token";

        // when
        emailService.sendVerificationEmail(toEmail, token);

        // then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
