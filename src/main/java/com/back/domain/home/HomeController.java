package com.back.domain.home;

import com.back.global.common.dto.RsData;
import com.back.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class HomeController {
    @GetMapping
    public String main() {
        return "redirect:/swagger-ui/index.html";
    }

    // CORS 스프링 시큐리티 필터 테스트용 컨트롤러 메서드
    @GetMapping("/api/test")
    @ResponseBody
    public String test() {
        return "Hello World";
    }
}