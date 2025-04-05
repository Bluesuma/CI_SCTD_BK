package ru.centerinvest.sctd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.centerinvest.sctd.model.Comment;
import ru.centerinvest.sctd.model.Document;
import ru.centerinvest.sctd.model.User;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByDocument(Document document);
    List<Comment> findByUser(User user);
    long countByUser(User user);
} 