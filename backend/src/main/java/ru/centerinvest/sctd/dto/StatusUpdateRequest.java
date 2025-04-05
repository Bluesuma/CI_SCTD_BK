package ru.centerinvest.sctd.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.centerinvest.sctd.model.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    @NotNull(message = "Статус не может быть пустым")
    private Document.Status status;
    
    private String comment;
} 