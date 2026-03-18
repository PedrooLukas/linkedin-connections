package com.pedro.linkedin.connections.repository;

import com.pedro.linkedin.connections.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
