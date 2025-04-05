package ru.centerinvest.sctd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.StatusHistory;
import ru.centerinvest.sctd.model.User;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByDocumentOrderByCreatedAtDesc(Document document);
    List<StatusHistory> findByUser(User user);
    long countByUser(User user);
} 