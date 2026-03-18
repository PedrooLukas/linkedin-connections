package com.pedro.linkedin.connections.repository;

import com.pedro.linkedin.connections.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
}
