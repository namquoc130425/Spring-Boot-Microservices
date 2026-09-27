package com.nvqn.user_service.controller;

import com.nvqn.user_service.entity.Student;
import com.nvqn.user_service.service.interfaces.IStudent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestController {

    @Autowired
    IStudent studentservice;


    @GetMapping
    public ResponseEntity<List<Student>> getAllStudent() {
        return ResponseEntity.ok(studentservice.findAll());
    }
}
