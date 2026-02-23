package com.lgcns.bebee.chat.core.config;

import com.lgcns.bebee.chat.core.utils.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenValidator jwtTokenValidator;

    /**
     * 메시지 전송 전에 호출됩니다.
     * CONNECT 명령에서 인증 정보를 추출하여 Principal을 설정합니다.
     *
     * @param message 전송될 메시지
     * @param channel 메시지 채널
     * @return 처리된 메시지
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        log.info("수신된 STOMP 명령: {}", accessor.getCommand());

        if(accessor == null){
            log.error("STOMP accessor가 NULL 입니다. 파싱할 수 없는 메시지 형식입니다.");
            throw new MessageDeliveryException("잘못된 메시지 형식입니다.");
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnectMessage(accessor);
        }else if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribeMessage(accessor);
        }

        return message;
    }

    private void handleConnectMessage(StompHeaderAccessor accessor){
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        // Authorization 헤더에서 토큰 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtTokenValidator.parseClaims(token);
                Long memberId = jwtTokenValidator.extractMemberId(claims);

                MemberPrincipal principal = new MemberPrincipal(memberId);
                accessor.setUser(principal);
                log.info("WebSocket authentication successful - Member ID: {}", memberId);
            } catch (JwtException e) {
                log.warn("WebSocket authentication failed - Invalid token: {}", e.getMessage());
                throw new MessageDeliveryException("유효하지 않은 인증 토큰입니다.");
            }
        } else {
            log.warn("WebSocket connection without authentication token");
            throw new MessageDeliveryException("인증 토큰이 필요합니다.");
        }
    }

    private void handleSubscribeMessage(StompHeaderAccessor accessor){
        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();

        if(principal == null || destination == null || !destination.startsWith("/sub/member:")){
            log.warn("STOMP SUBSCRIBE denied - Missing principal or destination. Session ID: {}",
                    accessor.getSessionId());

            throw new MessageDeliveryException("구독 정보가 유효하지 않습니다.");
        }
    }
}