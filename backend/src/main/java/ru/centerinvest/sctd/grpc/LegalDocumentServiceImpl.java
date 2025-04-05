package ru.centerinvest.sctd.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.centerinvest.sctd.dto.LegalDocumentDto;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.StatusHistory;
import ru.centerinvest.sctd.model.User;
import ru.centerinvest.sctd.repository.DocumentRepository;
import ru.centerinvest.sctd.service.LegalDocumentService;
import ru.centerinvest.sctd.service.StatusHistoryService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class LegalDocumentServiceImpl extends LegalDocumentServiceGrpc.LegalDocumentServiceImplBase {

    private final LegalDocumentService legalDocumentService;
    private final DocumentRepository documentRepository;
    private final StatusHistoryService statusHistoryService;

    @Override
    public void searchLegalDocuments(SearchLegalDocumentsRequest request, StreamObserver<SearchLegalDocumentsResponse> responseObserver) {
        try {
            // Выполняем поиск НПА
            List<LegalDocumentDto> searchResults = legalDocumentService.searchLegalDocuments(
                    request.getQuery(),
                    request.getDocumentType(),
                    request.getIssuedAfter(),
                    request.getIssuedBefore()
            );
            
            // Преобразуем результаты в формат gRPC
            List<LegalDocumentProto> protoResults = searchResults.stream()
                    .map(this::convertToProto)
                    .collect(Collectors.toList());
            
            // Формируем и отправляем ответ
            SearchLegalDocumentsResponse response = SearchLegalDocumentsResponse.newBuilder()
                    .addAllDocuments(protoResults)
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при поиске НПА: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void importLegalDocument(ImportLegalDocumentRequest request, StreamObserver<ImportLegalDocumentResponse> responseObserver) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            
            // Импортируем документ
            Document importedDocument = legalDocumentService.importLegalDocument(
                    request.getSourceUrl(),
                    request.getTitle(),
                    request.getDescription(),
                    currentUser.getId()
            );
            
            // Формируем и отправляем ответ
            ImportLegalDocumentResponse response = ImportLegalDocumentResponse.newBuilder()
                    .setSuccess(true)
                    .setDocumentId(importedDocument.getId())
                    .setMessage("Документ успешно импортирован")
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при импорте НПА: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getLegalDocumentDetails(GetLegalDocumentDetailsRequest request, StreamObserver<LegalDocumentDetailsResponse> responseObserver) {
        try {
            // Получаем документ
            Document document = documentRepository.findById(request.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Документ не найден"));
            
            // Получаем историю статусов
            List<StatusHistory> statusHistoryList = statusHistoryService.getStatusHistoryByDocument(document);
            
            // Формируем ответ
            LegalDocumentDetailsResponse.Builder responseBuilder = LegalDocumentDetailsResponse.newBuilder()
                    .setId(document.getId())
                    .setTitle(document.getTitle())
                    .setDescription(document.getDescription() != null ? document.getDescription() : "")
                    .setFileName(document.getFileName())
                    .setFileType(document.getFileType())
                    .setFileSize(document.getFileSize())
                    .setFileUrl("/api/documents/" + document.getId() + "/file")
                    .setStatus(document.getStatus().name());
            
            // Добавляем timestamp'ы
            if (document.getCreatedAt() != null) {
                Instant createdInstant = document.getCreatedAt().toInstant(ZoneOffset.UTC);
                responseBuilder.setCreatedAt(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(createdInstant.getEpochSecond())
                        .setNanos(createdInstant.getNano())
                        .build());
            }
            
            // Добавляем историю статусов
            for (StatusHistory history : statusHistoryList) {
                StatusHistoryProto.Builder historyBuilder = StatusHistoryProto.newBuilder()
                        .setId(history.getId())
                        .setStatus(history.getStatus().name())
                        .setComment(history.getComment() != null ? history.getComment() : "");
                
                if (history.getUser() != null) {
                    historyBuilder.setUserName(history.getUser().getName());
                }
                
                if (history.getCreatedAt() != null) {
                    Instant historyInstant = history.getCreatedAt().toInstant(ZoneOffset.UTC);
                    historyBuilder.setDate(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(historyInstant.getEpochSecond())
                            .setNanos(historyInstant.getNano())
                            .build());
                }
                
                responseBuilder.addStatusHistory(historyBuilder.build());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении деталей НПА: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Вспомогательный метод для конвертации DTO в proto
    private LegalDocumentProto convertToProto(LegalDocumentDto dto) {
        return LegalDocumentProto.newBuilder()
                .setTitle(dto.getTitle())
                .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
                .setSourceUrl(dto.getSourceUrl() != null ? dto.getSourceUrl() : "")
                .build();
    }
} 