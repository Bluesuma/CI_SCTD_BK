package ru.centerinvest.sctd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.centerinvest.sctd.model.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private Long id;
    private String title;
    private String description;
    private String department;
    private UserDto author;
    private Document.Status status;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private List<StatusHistoryDto> statusHistory = new ArrayList<>();
    
    @Builder.Default
    private List<CommentDto> comments = new ArrayList<>();
    
    public static DocumentDto fromEntity(Document document) {
        DocumentDto dto = DocumentDto.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .department(document.getDepartment())
                .author(UserDto.fromEntity(document.getAuthor()))
                .status(document.getStatus())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .fileUrl("/api/documents/" + document.getId() + "/file")
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
        
        if (document.getStatusHistory() != null) {
            dto.setStatusHistory(document.getStatusHistory().stream()
                    .map(StatusHistoryDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        if (document.getComments() != null) {
            dto.setComments(document.getComments().stream()
                    .map(CommentDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
} 