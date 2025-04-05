package ru.centerinvest.sctd.grpc.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.centerinvest.sctd.security.JwtTokenProvider;

/**
 * Перехватчик для аутентификации по JWT токену в gRPC сервисах
 */
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationInterceptor.class);
    
    private static final Metadata.Key<String> AUTHORIZATION_HEADER =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        // Проверяем методы, которые не требуют аутентификации
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        if (isPublicMethod(fullMethodName)) {
            return next.startCall(call, headers);
        }
        
        // Пытаемся получить токен из заголовка
        String token = getBearerToken(headers);
        if (token == null) {
            logger.warn("Отсутствует токен авторизации в запросе к методу {}", fullMethodName);
            call.close(Status.UNAUTHENTICATED.withDescription("Отсутствует токен авторизации"), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
        
        try {
            // Проверяем токен
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Недействительный токен авторизации в запросе к методу {}", fullMethodName);
                call.close(Status.UNAUTHENTICATED.withDescription("Недействительный токен авторизации"), new Metadata());
                return new ServerCall.Listener<ReqT>() {};
            }
            
            // Получаем информацию о пользователе из токена
            String email = jwtTokenProvider.getUserEmailFromToken(token);
            
            // Создаем аутентификацию
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            
            // Устанавливаем аутентификацию в контекст безопасности
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("Пользователь {} успешно аутентифицирован для метода {}", email, fullMethodName);
            
            // Продолжаем обработку запроса
            return next.startCall(call, headers);
        } catch (Exception e) {
            logger.error("Ошибка при аутентификации: {}", e.getMessage(), e);
            call.close(Status.UNAUTHENTICATED.withDescription("Ошибка аутентификации: " + e.getMessage()), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
    }

    private String getBearerToken(Metadata headers) {
        String authHeader = headers.get(AUTHORIZATION_HEADER);
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    private boolean isPublicMethod(String fullMethodName) {
        return fullMethodName.equals("ru.centerinvest.sctd.grpc.AuthService/login") ||
               fullMethodName.equals("ru.centerinvest.sctd.grpc.AuthService/register") ||
               fullMethodName.equals("ru.centerinvest.sctd.grpc.AuthService/validateToken");
    }
} 