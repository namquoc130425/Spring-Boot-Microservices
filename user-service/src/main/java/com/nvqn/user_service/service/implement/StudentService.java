package com.nvqn.user_service.service.implement;

import com.nvqn.user_service.entity.Student;
import com.nvqn.user_service.repository.StudentRepository;
import com.nvqn.user_service.service.interfaces.IStudent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentService implements IStudent {
    StudentRepository studentRepository;
    @Override
    public List<Student> findAll() {
        return studentRepository.findAll();
    }
}
