package ru.centerinvest.sctd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreateRequest {
    @NotBlank(message = "Название документа не может быть пустым")
    private String title;
    
    private String description;
    
    @NotBlank(message = "Отдел не может быть пустым")
    private String department;
    
    private MultipartFile file;
} 