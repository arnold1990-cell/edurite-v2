package com.edurite.tutor.controller;

import com.edurite.tutor.dto.TutorDtos.TutorAskRequest;
import com.edurite.tutor.dto.TutorDtos.TutorAskResponse;
import com.edurite.tutor.dto.TutorDtos.TutorSessionRequest;
import com.edurite.tutor.dto.TutorDtos.TutorSessionResponse;
import com.edurite.tutor.service.TutorService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/student/tutor", "/api/student/tutor"})
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/sessions")
    public List<TutorSessionResponse> sessions(Principal principal) {
        return tutorService.listSessions(principal);
    }

    @PostMapping("/sessions")
    public TutorSessionResponse createSession(Principal principal, @Valid @RequestBody TutorSessionRequest request) {
        return tutorService.createSession(principal, request);
    }

    @GetMapping("/sessions/{sessionId}")
    public TutorSessionResponse session(Principal principal, @PathVariable UUID sessionId) {
        return tutorService.getSession(principal, sessionId);
    }

    @PostMapping("/ask")
    public TutorAskResponse ask(Principal principal, @Valid @RequestBody TutorAskRequest request) {
        return tutorService.ask(principal, request);
    }
}

