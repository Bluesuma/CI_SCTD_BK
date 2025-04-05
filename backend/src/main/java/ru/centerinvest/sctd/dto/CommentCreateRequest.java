package ru.centerinvest.sctd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(max = 1000, message = "Длина комментария не должна превышать 1000 символов")
    private String text;
} 