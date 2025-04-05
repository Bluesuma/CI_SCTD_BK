package ru.centerinvest.sctd.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import ru.centerinvest.sctd.model.Comment;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.StatusHistory;
import ru.centerinvest.sctd.model.User;
import ru.centerinvest.sctd.repository.DocumentRepository;
import ru.centerinvest.sctd.service.CommentService;
import ru.centerinvest.sctd.service.DocumentService;
import ru.centerinvest.sctd.service.StatusHistoryService;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class DocumentServiceImpl extends DocumentServiceGrpc.DocumentServiceImplBase {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final CommentService commentService;
    private final StatusHistoryService statusHistoryService;

    @Override
    public void getDocument(GetDocumentRequest request, StreamObserver<DocumentResponse> responseObserver) {
        try {
            Document document = documentService.getDocumentById(request.getId());
            DocumentResponse response = convertToDocumentResponse(document);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении документа: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listDocuments(ListDocumentsRequest request, StreamObserver<ListDocumentsResponse> responseObserver) {
        try {
            // Создаем запрос пагинации
            PageRequest pageRequest = PageRequest.of(
                    request.getPage() > 0 ? request.getPage() : 0,
                    request.getSize() > 0 ? request.getSize() : 10
            );

            // Фильтрация по статусу и/или отделу
            Page<Document> documentsPage;
            if (!request.getStatus().isEmpty() && !request.getDepartment().isEmpty()) {
                Document.Status status = Document.Status.valueOf(request.getStatus());
                documentsPage = documentRepository.findByStatusAndDepartment(status, request.getDepartment(), pageRequest);
            } else if (!request.getStatus().isEmpty()) {
                Document.Status status = Document.Status.valueOf(request.getStatus());
                documentsPage = documentRepository.findByStatus(status, pageRequest);
            } else if (!request.getDepartment().isEmpty()) {
                documentsPage = documentRepository.findByDepartment(request.getDepartment(), pageRequest);
            } else {
                documentsPage = documentRepository.findAll(pageRequest);
            }

            // Создаем ответ
            List<DocumentResponse> documentResponses = documentsPage.getContent().stream()
                    .map(this::convertToDocumentResponse)
                    .collect(Collectors.toList());

            ListDocumentsResponse response = ListDocumentsResponse.newBuilder()
                    .addAllDocuments(documentResponses)
                    .setTotalPages(documentsPage.getTotalPages())
                    .setTotalElements(documentsPage.getTotalElements())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при получении списка документов: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createDocument(CreateDocumentRequest request, StreamObserver<DocumentResponse> responseObserver) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            // Создаем временный файл из содержимого
            String fileName = UUID.randomUUID().toString() + "_" + request.getFileName();
            String filePath = System.getProperty("user.dir") + "/uploads/" + fileName;
            
            // Убедимся, что директория существует
            new java.io.File(System.getProperty("user.dir") + "/uploads/").mkdirs();
            
            // Сохраняем файл
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), request.getFileContent().toByteArray());

            // Создаем документ
            Document document = Document.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .department(request.getDepartment())
                    .author(currentUser)
                    .status(Document.Status.DRAFT)
                    .filePath(filePath)
                    .fileName(request.getFileName())
                    .fileType(request.getFileType())
                    .fileSize((long) request.getFileContent().size())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Document savedDocument = documentRepository.save(document);

            // Добавляем запись в историю статусов
            StatusHistory statusHistory = StatusHistory.builder()
                    .document(savedDocument)
                    .user(currentUser)
                    .status(Document.Status.DRAFT)
                    .comment("Документ создан")
                    .createdAt(LocalDateTime.now())
                    .build();
            statusHistoryService.saveStatusHistory(statusHistory);

            // Возвращаем ответ
            DocumentResponse response = convertToDocumentResponse(savedDocument);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при создании документа: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateDocumentStatus(UpdateStatusRequest request, StreamObserver<DocumentResponse> responseObserver) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            // Получаем документ
            Document document = documentService.getDocumentById(request.getDocumentId());

            // Обновляем статус
            document.setStatus(Document.Status.valueOf(request.getStatus()));
            document.setUpdatedAt(LocalDateTime.now());
            Document updatedDocument = documentRepository.save(document);

            // Добавляем запись в историю статусов
            StatusHistory statusHistory = StatusHistory.builder()
                    .document(updatedDocument)
                    .user(currentUser)
                    .status(Document.Status.valueOf(request.getStatus()))
                    .comment(request.getComment())
                    .createdAt(LocalDateTime.now())
                    .build();
            statusHistoryService.saveStatusHistory(statusHistory);

            // Возвращаем ответ
            DocumentResponse response = convertToDocumentResponse(updatedDocument);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при обновлении статуса документа: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void addComment(AddCommentRequest request, StreamObserver<CommentResponse> responseObserver) {
        try {
            // Получаем текущего пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            // Получаем документ
            Document document = documentService.getDocumentById(request.getDocumentId());

            // Создаем комментарий
            Comment comment = Comment.builder()
                    .document(document)
                    .user(currentUser)
                    .text(request.getText())
                    .createdAt(LocalDateTime.now())
                    .build();
            Comment savedComment = commentService.saveComment(comment);

            // Возвращаем ответ
            CommentResponse response = convertToCommentResponse(savedComment);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка при добавлении комментария: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Вспомогательные методы для конвертации сущностей в gRPC ответы

    private DocumentResponse convertToDocumentResponse(Document document) {
        DocumentResponse.Builder responseBuilder = DocumentResponse.newBuilder()
                .setId(document.getId())
                .setTitle(document.getTitle())
                .setDescription(document.getDescription() != null ? document.getDescription() : "")
                .setDepartment(document.getDepartment())
                .setAuthor(convertToUserResponse(document.getAuthor()))
                .setStatus(document.getStatus().name())
                .setFileName(document.getFileName())
                .setFileType(document.getFileType())
                .setFileSize(document.getFileSize())
                .setFileUrl("/api/documents/" + document.getId() + "/file");

        // Добавляем timestamp'ы
        if (document.getCreatedAt() != null) {
            Instant createdInstant = document.getCreatedAt().toInstant(ZoneOffset.UTC);
            responseBuilder.setCreatedAt(Timestamp.newBuilder()
                    .setSeconds(createdInstant.getEpochSecond())
                    .setNanos(createdInstant.getNano())
                    .build());
        }

        if (document.getUpdatedAt() != null) {
            Instant updatedInstant = document.getUpdatedAt().toInstant(ZoneOffset.UTC);
            responseBuilder.setUpdatedAt(Timestamp.newBuilder()
                    .setSeconds(updatedInstant.getEpochSecond())
                    .setNanos(updatedInstant.getNano())
                    .build());
        }

        // Добавляем историю статусов
        List<StatusHistory> statusHistoryList = statusHistoryService.getStatusHistoryByDocument(document);
        for (StatusHistory history : statusHistoryList) {
            responseBuilder.addStatusHistory(convertToStatusHistoryResponse(history));
        }

        // Добавляем комментарии
        List<Comment> comments = commentService.getCommentsByDocument(document);
        for (Comment comment : comments) {
            responseBuilder.addComments(convertToCommentResponse(comment));
        }

        return responseBuilder.build();
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.newBuilder()
                .setId(user.getId())
                .setName(user.getName())
                .setEmail(user.getEmail())
                .setDepartment(user.getDepartment())
                .setRole(user.getRole().name())
                .build();
    }

    private StatusHistoryResponse convertToStatusHistoryResponse(StatusHistory history) {
        StatusHistoryResponse.Builder responseBuilder = StatusHistoryResponse.newBuilder()
                .setId(history.getId())
                .setUser(convertToUserResponse(history.getUser()))
                .setStatus(history.getStatus().name())
                .setComment(history.getComment() != null ? history.getComment() : "");

        if (history.getCreatedAt() != null) {
            Instant dateInstant = history.getCreatedAt().toInstant(ZoneOffset.UTC);
            responseBuilder.setDate(Timestamp.newBuilder()
                    .setSeconds(dateInstant.getEpochSecond())
                    .setNanos(dateInstant.getNano())
                    .build());
        }

        return responseBuilder.build();
    }

    private CommentResponse convertToCommentResponse(Comment comment) {
        CommentResponse.Builder responseBuilder = CommentResponse.newBuilder()
                .setId(comment.getId())
                .setUser(convertToUserResponse(comment.getUser()))
                .setText(comment.getText());

        if (comment.getCreatedAt() != null) {
            Instant dateInstant = comment.getCreatedAt().toInstant(ZoneOffset.UTC);
            responseBuilder.setDate(Timestamp.newBuilder()
                    .setSeconds(dateInstant.getEpochSecond())
                    .setNanos(dateInstant.getNano())
                    .build());
        }

        return responseBuilder.build();
    }
} 