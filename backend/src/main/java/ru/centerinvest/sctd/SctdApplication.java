package ru.centerinvest.sctd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Система проверки технических заданий",
        version = "1.0",
        description = "API для системы проверки технических заданий Центр-Инвест"
    )
)
public class SctdApplication {
    public static void main(String[] args) {
        SpringApplication.run(SctdApplication.class, args);
    }
} 