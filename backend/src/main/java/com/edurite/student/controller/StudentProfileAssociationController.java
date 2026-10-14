package com.edurite.student.controller;

import com.edurite.student.dto.StudentProfileAssociationDto;
import com.edurite.student.dto.StudentProfileAssociationDto.Type;
import com.edurite.student.service.StudentProfileAssociationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/student/profile/associations", "/api/student/profile/associations"})
public class StudentProfileAssociationController {
    private final StudentProfileAssociationService service;
    public StudentProfileAssociationController(StudentProfileAssociationService service) { this.service = service; }

    @GetMapping
    public List<StudentProfileAssociationDto> list(Principal principal) { return service.list(principal); }

    @PutMapping("/{type}/{targetId}")
    public List<StudentProfileAssociationDto> link(Principal principal, @PathVariable Type type, @PathVariable UUID targetId) {
        return service.link(principal, type, targetId);
    }

    @DeleteMapping("/{type}/{targetId}")
    public List<StudentProfileAssociationDto> unlink(Principal principal, @PathVariable Type type, @PathVariable UUID targetId) {
        return service.unlink(principal, type, targetId);
    }
}
