package ru.centerinvest.sctd.config;

import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.CompositeGrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.check.AccessPredicate;
import net.devh.boot.grpc.server.security.check.AccessPredicateVoter;
import net.devh.boot.grpc.server.security.check.GrpcSecurityMetadataSource;
import net.devh.boot.grpc.server.security.check.ManualGrpcSecurityMetadataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import ru.centerinvest.sctd.grpc.interceptor.ExceptionHandlingInterceptor;
import ru.centerinvest.sctd.grpc.interceptor.JwtAuthenticationInterceptor;
import ru.centerinvest.sctd.security.JwtTokenProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class GrpcConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Bean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        final List<GrpcAuthenticationReader> readers = new ArrayList<>();
        readers.add(new BasicGrpcAuthenticationReader());
        return new CompositeGrpcAuthenticationReader(readers);
    }

    @Bean
    public GrpcSecurityMetadataSource grpcSecurityMetadataSource() {
        final ManualGrpcSecurityMetadataSource source = new ManualGrpcSecurityMetadataSource();
        
        // Настройка доступа к методам сервиса аутентификации
        source.set("ru.centerinvest.sctd.grpc.AuthService/login", AccessPredicate.permitAll());
        source.set("ru.centerinvest.sctd.grpc.AuthService/register", AccessPredicate.permitAll());
        source.set("ru.centerinvest.sctd.grpc.AuthService/validateToken", AccessPredicate.permitAll());
        
        // Настройка доступа к методам сервиса документов
        source.set("ru.centerinvest.sctd.grpc.DocumentService/*", AccessPredicate.hasAnyRole("USER", "ADMIN"));
        
        // Настройка доступа к методам сервиса НПА
        source.set("ru.centerinvest.sctd.grpc.LegalDocumentService/*", AccessPredicate.hasAnyRole("USER", "ADMIN"));
        
        return source;
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        final List<AccessDecisionVoter<?>> voters = new ArrayList<>();
        voters.add(new AccessPredicateVoter());
        return new UnanimousBased(voters);
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor exceptionHandlingInterceptor() {
        return new ExceptionHandlingInterceptor();
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor jwtAuthenticationInterceptor() {
        return new JwtAuthenticationInterceptor(jwtTokenProvider, authenticationManager);
    }
} 