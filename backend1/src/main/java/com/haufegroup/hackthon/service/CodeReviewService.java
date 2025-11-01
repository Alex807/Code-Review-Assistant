package com.haufegroup.hackthon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufegroup.hackthon.dto.CodeReviewRequest;
import com.haufegroup.hackthon.dto.CodeReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MODEL = "deepseek-coder:1.3b";

    // Optimized limits for small model
    private static final int MAX_CODE_LENGTH = 2000; // Reduced for 700MB model
    private static final int TIMEOUT_SECONDS = 60; // Increased timeout

    public CodeReviewService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:11434")
                .build();
    }

    public CodeReviewResponse reviewCode(CodeReviewRequest request) {

        if (request.language().equals("plaintext")) {
            return new CodeReviewResponse("Please write code into a Programming Language");
        }

        try {
            String userPrompt = buildUserPrompt(request);
            log.info("Sending review request (lang: {}, code_length: {})",
                    request.language(), request.code().length());

            Map<String, Object> payload = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                                            You are a code reviewer. Analize only bugs and security based on good practices:
                                            
                                            BUGS:
                                            - [Line X] description
                                            
                                            SECURITY:
                                            - [Line X] description
                                            
                                            If a section has no issues, write "None".
                                            DO NOT include explanations, code snippets, or extra text.
                                            BE CONCISE. Maximum 3 items per section.
                                            """
                            ),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "stream", false,
                    "options", Map.of(
                            "temperature", 0.3,        // Lower = more focused
                            "top_p", 0.8,              // Reduce randomness
                            "num_predict", 300,        // Limit response tokens
                            "stop", List.of("```", "END", "---") // Stop sequences
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.debug("Ollama payload: {}", jsonPayload);

            JsonNode response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(jsonPayload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();

            if (response == null || !response.has("message")) {
                throw new RuntimeException("Invalid response from Ollama");
            }

            String review = response
                    .path("message")
                    .path("content")
                    .asText("No review generated.");

            // Clean up response - remove any extra text
            review = cleanResponse(review);

            log.info("Review completed successfully");
            return new CodeReviewResponse(review);

        } catch (Exception e) {
            log.error("Review failed: {}", e.getMessage(), e);
            return new CodeReviewResponse("Backend Error: " + e.getMessage());
        }
    }

    private String buildUserPrompt(CodeReviewRequest request) {
        String lang = request.language() != null && !request.language().isBlank()
                ? request.language()
                : "unknown";

        // Aggressive truncation for small model
        String codeSnippet = request.code().length() > MAX_CODE_LENGTH
                ? request.code().substring(0, MAX_CODE_LENGTH) + "\n... (truncated)"
                : request.code();

        // Minimal prompt to save tokens
        return String.format("Language: %s\n\nCode:\n%s\n\nReview:", lang, codeSnippet);
    }

    private String cleanResponse(String response) {
        // Remove common extra text patterns
        response = response.trim();

        // Remove code blocks if model includes them
        response = response.replaceAll("```[\\w]*\\n", "");
        response = response.replaceAll("```", "");

        // Remove common preambles
        response = response.replaceAll("(?i)^(here is|here's|the review is|review:)\\s*", "");

        // Remove trailing explanations after "---" or similar
        if (response.contains("---")) {
            response = response.substring(0, response.indexOf("---"));
        }

        return response.trim();
    }
}