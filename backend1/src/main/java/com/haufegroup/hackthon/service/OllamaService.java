package com.haufegroup.hackthon.service;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public OllamaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeCode(String code, String language) {
        try {
            String prompt = buildReviewPrompt(code, language);

            Map<String, Object> request = new HashMap<>();
            request.put("model", "llama2"); // Change to your model name
            request.put("prompt", prompt);
            request.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    OLLAMA_URL,
                    entity,
                    Map.class
            );

            return (String) response.get("response");

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze code: " + e.getMessage(), e);
        }
    }

    private String buildReviewPrompt(String code, String language) {
        return String.format("""
        You are an expert code reviewer. Analyze the following %s code and provide a detailed review.
        
        Focus on:
        1. Bugs and potential errors
        2. Security vulnerabilities
        3. Code smells and anti-patterns
        4. Performance issues
        5. Best practices violations
        
        For each issue found, provide:
        - Severity (Critical/High/Medium/Low)
        - Line number (if applicable)
        - Description of the issue
        - Suggested fix
        
        Format your response as JSON with this structure:
        {
          "issues": [
            {
              "severity": "High",
              "line": 10,
              "type": "Security",
              "description": "SQL injection vulnerability",
              "suggestion": "Use prepared statements"
            }
          ],
          "summary": "Overall code quality assessment"
        }
        
        Code to review:
        ```%s
        %s
        ```
        
        Provide only the JSON response, no additional text.
        """, language, language, code);
    }
}