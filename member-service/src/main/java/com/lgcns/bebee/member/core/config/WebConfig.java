package com.lgcns.bebee.member.core.config;

import com.lgcns.bebee.common.web.CurrentMemberArgumentResolver;
import com.lgcns.bebee.common.web.MemberAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final MemberAuthenticationInterceptor memberAuthenticationInterceptor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(memberAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/signup",
                        "/auth/check-email",
                        "/auth/check-nickname",
                        "/auth/reissue",
                        "/documents/**", "/api/documents/**",
                        "/test/**", "/error",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
}