package com.edurite.whatsapp.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppResponseService {

    public String responseForIntent(String intent) {
        return switch (intent) {
            case "bursaries" -> "EduRite bursary help: log in to your dashboard and open Bursary Finder to search, save, and track applications.";
            case "scholarships" -> "EduRite scholarship help: use Scholarship Applications to track checklists, deadlines, and motivation letters.";
            case "universities" -> "EduRite university help: open Universities or University Applications to compare programmes and track applications.";
            case "careers" -> "EduRite career help: use AI Guidance and Career Roadmaps to explore paths, skills, and study options.";
            default -> "EduRite bot help: ask about bursaries, universities, careers, scholarships, AI guidance, or AI tutor.";
        };
    }
}

