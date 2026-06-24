package com.edurite.whatsapp.controller;

import com.edurite.whatsapp.dto.WhatsAppDtos.WhatsAppTextResponse;
import com.edurite.whatsapp.service.WhatsAppMessageParserService;
import com.edurite.whatsapp.service.WhatsAppResponseService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/webhooks/whatsapp", "/api/webhooks/whatsapp"})
public class WhatsAppWebhookController {

    private final WhatsAppMessageParserService parserService;
    private final WhatsAppResponseService responseService;
    private final boolean enabled;
    private final String verifyToken;

    public WhatsAppWebhookController(
            WhatsAppMessageParserService parserService,
            WhatsAppResponseService responseService,
            @Value("${whatsapp.bot.enabled:false}") boolean enabled,
            @Value("${whatsapp.verify-token:}") String verifyToken
    ) {
        this.parserService = parserService;
        this.responseService = responseService;
        this.enabled = enabled;
        this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (!enabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("WhatsApp bot disabled");
        }
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge == null ? "" : challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid verification token");
    }

    @PostMapping
    public ResponseEntity<WhatsAppTextResponse> receive(@RequestBody(required = false) Map<String, Object> payload) {
        if (!enabled) {
            return ResponseEntity.ok(new WhatsAppTextResponse("disabled", "WhatsApp bot is disabled."));
        }
        var parsed = parserService.parse(payload == null ? Map.of() : payload);
        String response = responseService.responseForIntent(parsed.intent());
        return ResponseEntity.ok(new WhatsAppTextResponse(parsed.intent(), response));
    }
}

