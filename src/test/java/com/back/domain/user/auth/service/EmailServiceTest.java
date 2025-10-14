package com.back.domain.user.auth.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailService, "FROM_ADDRESS", "test@example.com");
        ReflectionTestUtils.setField(emailService, "FRONTEND_BASE_URL", "http://localhost:3000");
    }

    @Test
    @DisplayName("이메일 인증 메일 전송 성공")
    void sendVerificationEmail_success() {
        // given
        String toEmail = "test@example.com";
        String token = "sample-token";

        // MimeMessage Stub 반환하도록 설정
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendVerificationEmail(toEmail, token);

        // then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
