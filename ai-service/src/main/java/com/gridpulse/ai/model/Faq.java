package com.gridpulse.ai.model;

// one entry of resources/faqs.json, only read on startup
public record Faq(Long id, String category, String question, String answer) {
}
