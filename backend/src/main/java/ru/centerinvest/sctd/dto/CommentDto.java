package ru.centerinvest.sctd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.centerinvest.sctd.model.Comment;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private UserDto user;
    private String text;
    private LocalDateTime date;
    
    public static CommentDto fromEntity(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(UserDto.fromEntity(comment.getUser()))
                .text(comment.getText())
                .date(comment.getCreatedAt())
                .build();
    }
} 