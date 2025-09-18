package com.back.domain.home;

import com.back.global.common.dto.RsData;
import com.back.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class HomeController {
    @GetMapping
    public String main() {
        return "redirect:/swagger-ui/index.html";
    }
}
