package com.edurite.whatsapp.dto;

public final class WhatsAppDtos {
    private WhatsAppDtos() {
    }

    public record WhatsAppParsedMessage(String sender, String text, String intent) {
    }

    public record WhatsAppTextResponse(String intent, String response) {
    }
}

