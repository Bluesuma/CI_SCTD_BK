package ru.centerinvest.sctd.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.centerinvest.sctd.model.User;
import ru.centerinvest.sctd.security.JwtTokenProvider;
import ru.centerinvest.sctd.service.UserService;

@GrpcService
@RequiredArgsConstructor
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            // Аутентификация пользователя
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Устанавливаем аутентификацию в контекст безопасности
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Получаем информацию о пользователе
            User user = (User) authentication.getPrincipal();
            
            // Генерируем токен
            String token = jwtTokenProvider.generateToken(authentication);
            
            // Создаем и отправляем ответ
            LoginResponse response = LoginResponse.newBuilder()
                    .setToken(token)
                    .setUser(UserResponse.newBuilder()
                            .setId(user.getId())
                            .setName(user.getName())
                            .setEmail(user.getEmail())
                            .setDepartment(user.getDepartment())
                            .setRole(user.getRole().name())
                            .build())
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (AuthenticationException e) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Неверные учетные данные")
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при аутентификации: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            // Проверяем, существует ли пользователь с таким email
            if (userService.existsByEmail(request.getEmail())) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Пользователь с таким email уже существует")
                        .asRuntimeException());
                return;
            }
            
            // Создаем нового пользователя
            User newUser = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(request.getPassword()) // Хеширование пароля происходит в сервисе
                    .department(request.getDepartment())
                    .role(User.Role.valueOf(request.getRole()))
                    .build();
                    
            User registeredUser = userService.registerUser(newUser);
            
            // Аутентифицируем пользователя
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            // Устанавливаем аутентификацию в контекст безопасности
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Генерируем токен
            String token = jwtTokenProvider.generateToken(authentication);
            
            // Создаем и отправляем ответ
            LoginResponse response = LoginResponse.newBuilder()
                    .setToken(token)
                    .setUser(UserResponse.newBuilder()
                            .setId(registeredUser.getId())
                            .setName(registeredUser.getName())
                            .setEmail(registeredUser.getEmail())
                            .setDepartment(registeredUser.getDepartment())
                            .setRole(registeredUser.getRole().name())
                            .build())
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при регистрации: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            // Проверяем токен
            boolean isValid = jwtTokenProvider.validateToken(request.getToken());
            
            if (isValid) {
                // Получаем информацию о пользователе из токена
                String email = jwtTokenProvider.getUserEmailFromToken(request.getToken());
                User user = userService.findByEmail(email);
                
                if (user == null) {
                    responseObserver.onError(Status.UNAUTHENTICATED
                            .withDescription("Пользователь не найден")
                            .asRuntimeException());
                    return;
                }
                
                // Создаем и отправляем ответ
                ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                        .setValid(true)
                        .setUser(UserResponse.newBuilder()
                                .setId(user.getId())
                                .setName(user.getName())
                                .setEmail(user.getEmail())
                                .setDepartment(user.getDepartment())
                                .setRole(user.getRole().name())
                                .build())
                        .build();
                        
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                // Токен недействителен
                ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                        .setValid(false)
                        .build();
                        
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            // Токен недействителен или произошла ошибка
            ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
} 