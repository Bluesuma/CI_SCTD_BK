package ru.centerinvest.sctd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.centerinvest.sctd.dto.LegalDocumentDto;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.StatusHistory;
import ru.centerinvest.sctd.model.User;
import ru.centerinvest.sctd.repository.DocumentRepository;
import ru.centerinvest.sctd.repository.StatusHistoryRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LegalDocumentService {

    private final RestTemplate restTemplate;
    private final DocumentRepository documentRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final String uploadsDir;

    // API доступа к правовой информации
    private static final String PRAVO_GOV_API_URL = "http://pravo.gov.ru/proxy/ips";
    private static final String CONSULTANT_API_URL = "https://api.consultant.ru/documents";

    @Autowired
    public LegalDocumentService(DocumentRepository documentRepository, 
                              StatusHistoryRepository statusHistoryRepository) {
        this.documentRepository = documentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.restTemplate = new RestTemplate();
        this.uploadsDir = System.getProperty("user.dir") + "/uploads/";

        // Создание директории для загрузок если она не существует
        new java.io.File(uploadsDir).mkdirs();
    }

    /**
     * Поиск документов по ключевым словам
     */
    public List<LegalDocumentDto> searchLegalDocuments(String query) {
        // В реальном проекте здесь должен быть вызов API для поиска документов
        // Для примера возвращаем моковые данные
        return Arrays.asList(
            new LegalDocumentDto("Федеральный закон №395-1", 
                "О банках и банковской деятельности", 
                "https://pravo.gov.ru/proxy/ips/?doc_itself=&nd=102010268"),
            new LegalDocumentDto("Федеральный закон №86-ФЗ", 
                "О Центральном банке РФ (Банке России)", 
                "https://pravo.gov.ru/proxy/ips/?doc_itself=&nd=102076584"),
            new LegalDocumentDto("Федеральный закон №173-ФЗ", 
                "О валютном регулировании и валютном контроле", 
                "https://pravo.gov.ru/proxy/ips/?doc_itself=&nd=102084008")
        );
    }

    /**
     * Загрузка документа по URL
     */
    public Document importLegalDocument(String title, String sourceUrl, String department, User author) {
        try {
            // Загрузка файла из внешнего источника
            byte[] fileContent = restTemplate.getForObject(sourceUrl, byte[].class);
            
            // Генерация уникального имени файла
            String fileName = UUID.randomUUID().toString() + ".pdf";
            String filePath = uploadsDir + fileName;
            
            // Сохранение файла
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), fileContent);
            
            // Создание документа
            Document document = Document.builder()
                    .title(title)
                    .description("Нормативно-правовой акт, загруженный из внешнего источника: " + sourceUrl)
                    .department(department)
                    .author(author)
                    .status(Document.Status.DRAFT)
                    .filePath(filePath)
                    .fileName(title + ".pdf")
                    .fileType("application/pdf")
                    .fileSize((long) fileContent.length)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            Document savedDocument = documentRepository.save(document);
            
            // Добавление истории статусов
            StatusHistory statusHistory = StatusHistory.builder()
                    .document(savedDocument)
                    .user(author)
                    .status(Document.Status.DRAFT)
                    .comment("Автоматический импорт нормативно-правового акта")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            statusHistoryRepository.save(statusHistory);
            
            return savedDocument;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при импорте документа: " + e.getMessage(), e);
        }
    }
} 