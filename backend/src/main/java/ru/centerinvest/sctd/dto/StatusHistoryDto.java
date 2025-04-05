package ru.centerinvest.sctd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.StatusHistory;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryDto {
    private Long id;
    private UserDto user;
    private Document.Status status;
    private String comment;
    private LocalDateTime date;
    
    public static StatusHistoryDto fromEntity(StatusHistory history) {
        return StatusHistoryDto.builder()
                .id(history.getId())
                .user(UserDto.fromEntity(history.getUser()))
                .status(history.getStatus())
                .comment(history.getComment())
                .date(history.getCreatedAt())
                .build();
    }
} 