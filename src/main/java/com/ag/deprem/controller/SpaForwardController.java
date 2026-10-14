package com.ag.deprem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/son-depremler",
            "/son-depremler/**",
            "/deprem/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
