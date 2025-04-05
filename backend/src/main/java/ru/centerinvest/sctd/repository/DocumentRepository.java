package ru.centerinvest.sctd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.User;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByAuthor(User author);
    List<Document> findByDepartment(String department);
    List<Document> findByStatus(Document.Status status);
    
    @Query("SELECT d FROM Document d WHERE " +
           "d.author = :user OR " +
           "d.department = :department AND :userRole = 'DEPARTMENT_HEAD'")
    List<Document> findAvailableDocuments(User user, String department, String userRole);
} 