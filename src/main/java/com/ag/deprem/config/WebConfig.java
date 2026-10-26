package com.ag.deprem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Value("${app.archive.directory:./archive}")
    private String archiveDirectory;

    private final AdminApiKeyInterceptor adminApiKeyInterceptor;

    public WebConfig(AdminApiKeyInterceptor adminApiKeyInterceptor) {
        this.adminApiKeyInterceptor = adminApiKeyInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "If-None-Match", "Accept", "X-Admin-Key")
                .exposedHeaders("ETag", "Cache-Control")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(adminApiKeyInterceptor)
                .addPathPatterns(
                        "/api/v1/archive/export",
                        "/api/v1/archive/verify/**",
                        "/api/v1/archive/backfill"
                );
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String archiveLocation = java.nio.file.Paths.get(archiveDirectory).toUri().toString();
        registry.addResourceHandler("/archive/**")
                .addResourceLocations(archiveLocation);
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setFilter(new ShallowEtagHeaderFilter());
        filterRegistration.addUrlPatterns("/api/*");
        return filterRegistration;
    }

    @Bean
    public FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> contentLanguageHeaderFilter() {
        FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setFilter(new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                response.setHeader("Content-Language", "tr");
                response.setLocale(java.util.Locale.forLanguageTag("tr-TR"));
                filterChain.doFilter(request, response);
            }
        });
        filterRegistration.addUrlPatterns("/*");
        filterRegistration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10);
        return filterRegistration;
    }
}
