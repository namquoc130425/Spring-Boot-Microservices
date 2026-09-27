package com.nvqn.user_service.repository;

import com.nvqn.user_service.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
}
