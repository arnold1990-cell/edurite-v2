package com.edurite.whatsapp.service;

import com.edurite.whatsapp.dto.WhatsAppDtos.WhatsAppParsedMessage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppMessageParserService {

    public WhatsAppParsedMessage parse(Map<String, Object> payload) {
        String text = firstString(payload, List.of("message", "text", "body"));
        String sender = firstString(payload, List.of("from", "sender", "phone"));
        if (text.isBlank()) {
            text = extractNestedText(payload);
        }
        return new WhatsAppParsedMessage(sender, text, intent(text));
    }

    public String intent(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.contains("bursary")) return "bursaries";
        if (normalized.contains("scholarship")) return "scholarships";
        if (normalized.contains("university") || normalized.contains("universities")) return "universities";
        if (normalized.contains("career")) return "careers";
        return "help";
    }

    private String firstString(Map<String, Object> payload, List<String> keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractNestedText(Map<String, Object> payload) {
        try {
            Object entry = ((List<Object>) payload.getOrDefault("entry", List.of())).get(0);
            Object change = ((List<Object>) ((Map<String, Object>) entry).getOrDefault("changes", List.of())).get(0);
            Object value = ((Map<String, Object>) change).get("value");
            Object message = ((List<Object>) ((Map<String, Object>) value).getOrDefault("messages", List.of())).get(0);
            Object text = ((Map<String, Object>) message).get("text");
            Object body = ((Map<String, Object>) text).get("body");
            return body instanceof String bodyText ? bodyText.trim() : "";
        } catch (RuntimeException ex) {
            return "";
        }
    }
}

