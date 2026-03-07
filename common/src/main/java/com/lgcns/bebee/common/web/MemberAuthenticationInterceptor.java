package com.lgcns.bebee.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.lgcns.bebee.common.exception.AuthenticationErrors.INVALID_TOKEN;
import static com.lgcns.bebee.common.web.AuthenticationUtil.MEMBER_KEY;

/**
 * 멤버 인증 인터셉터
 * - Gateway에서 JWT 검증 후 전달한 X-Member-Id 헤더에서 memberId를 추출
 * - 요청 속성에 memberId를 저장하여 컨트롤러에서 사용 가능하도록 함
 */

@Slf4j
@Component
public class MemberAuthenticationInterceptor implements HandlerInterceptor {

    private static final String X_MEMBER_ID_HEADER = "X-Member-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
//        log.info("인증 인터셉터 진입: URI={}, Method={}", request.getRequestURI(), request.getMethod());
        if (handler instanceof HandlerMethod) {
            String memberId = extractMemberId(request);
            request.setAttribute(MEMBER_KEY, memberId);
        }
        return true;
    }

    /**
     * X-Member-Id 헤더에서 memberId 추출
     * Gateway에서 JWT 검증 후 전달한 헤더 값을 사용
     */
    private String extractMemberId(HttpServletRequest request) {
        String memberId = request.getHeader(X_MEMBER_ID_HEADER);

        if (memberId == null || memberId.isEmpty()) {
            log.warn("X-Member-Id 헤더가 없습니다.");
            throw INVALID_TOKEN.toException();
        }

        log.debug("X-Member-Id 헤더에서 memberId 추출: {}", memberId);
        return memberId;
    }
}
