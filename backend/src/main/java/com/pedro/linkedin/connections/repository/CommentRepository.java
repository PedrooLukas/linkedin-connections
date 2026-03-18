package com.pedro.linkedin.connections.repository;

import com.pedro.linkedin.connections.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
