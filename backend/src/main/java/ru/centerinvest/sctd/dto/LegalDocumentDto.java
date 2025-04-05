package ru.centerinvest.sctd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentDto {
    private String title;
    private String description;
    private String sourceUrl;
} 