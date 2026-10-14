package com.ag.deprem.controller;

import com.ag.deprem.service.IndexNowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SeoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private IndexNowService indexNowService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void getSitemap_returnsValidXmlWith81Provinces() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")))
                .andExpect(content().string(containsString("/son-depremler/istanbul")))
                .andExpect(content().string(containsString("/son-depremler/izmir")))
                .andExpect(content().string(containsString("/son-depremler/van")))
                .andExpect(content().string(containsString("/son-depremler/duzce")));
    }

    @Test
    void getRobotsTxt_containsAiBotsAndSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("User-agent: Google-Extended")))
                .andExpect(content().string(containsString("User-agent: GPTBot")))
                .andExpect(content().string(containsString("User-agent: PerplexityBot")))
                .andExpect(content().string(containsString("Sitemap: https://depremizleme.com/sitemap.xml")));
    }

    @Test
    void getIndexNowKeyFile_returnsKey() throws Exception {
        String key = indexNowService.getIndexNowKey();
        mockMvc.perform(get("/" + key + ".txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(key));
    }

    @Test
    void getIndexNowKeyFile_invalidKeyReturns404() throws Exception {
        mockMvc.perform(get("/nonexistent-invalid-key.txt"))
                .andExpect(status().isNotFound());
    }
}
