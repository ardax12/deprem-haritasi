package com.ag.deprem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.NoSuchElementException;

@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/son-depremler",
            "/deprem/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/son-depremler/{slug}")
    public String forwardProvinceRoute(@PathVariable String slug) {
        if (slug == null || !SeoController.TURKISH_PROVINCE_SLUGS.contains(slug.toLowerCase().trim())) {
            throw new NoSuchElementException("İl bulunamadı: " + slug);
        }
        return "forward:/index.html";
    }
}
