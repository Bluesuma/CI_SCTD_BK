package ru.centerinvest.sctd.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.centerinvest.sctd.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String department;
    private User.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    @Builder.Default
    private UserStatsDto stats = new UserStatsDto();
    
    @Builder.Default
    private List<UserActivityDto> activities = new ArrayList<>();
    
    @JsonIgnore
    private String password;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .department(user.getDepartment())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatsDto {
        @Builder.Default
        private int documentsCreated = 0;
        
        @Builder.Default
        private int documentsReviewed = 0;
        
        @Builder.Default
        private int commentsLeft = 0;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityDto {
        private String description;
        private Long documentId;
        private LocalDateTime date;
    }
} 