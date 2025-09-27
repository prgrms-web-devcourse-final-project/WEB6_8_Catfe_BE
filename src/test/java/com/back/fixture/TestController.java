package com.back.fixture;

import com.back.global.security.user.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

//    @GetMapping("/public")
//    public String publicApi() {
//        return "누구나 접근 가능";
//    }

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal CustomUserDetails user) {
        return "내 정보: " + user.getUsername() + " (id=" + user.getUserId() + ")";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "관리자 전용 API";
    }
}
