package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${ai.use.mock:false}")
    private boolean useMockAI;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 2000;

    public AIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // Enhanced method for multi-document questions
    public String askQuestionEnhanced(String question, String documentContext) {
        System.out.println("AIService.askQuestionEnhanced called");
        System.out.println("Question: " + question);
        System.out.println("Document context length: " + (documentContext != null ? documentContext.length() : 0));

        if (useMockAI) {
            return generateEnhancedMockResponse(question, documentContext);
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "Gemini AI service is not configured. Please set your Gemini API key in application.properties. Get one from https://aistudio.google.com/";
        }

        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid question.";
        }

        // Rate limiting check
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
            return "Please wait a moment before asking another question.";
        }

        try {
            String result = callGeminiAPIEnhanced(question, documentContext);
            lastRequestTime = System.currentTimeMillis();
            return result;
        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, I encountered an error while processing your question: " + e.getMessage();
        }
    }

    public String generateSummaryEnhanced(String documentContent) {
        System.out.println("AIService.generateSummaryEnhanced called");
        System.out.println("Document content length: " + (documentContent != null ? documentContent.length() : 0));

        if (useMockAI) {
            return generateMockSummary(documentContent);
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "Gemini AI service is not configured. Please set your Gemini API key.";
        }

        try {
            String result = callGeminiAPIEnhanced("GENERATE_COMPREHENSIVE_SUMMARY", documentContent);
            lastRequestTime = System.currentTimeMillis();
            return result;
        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            e.printStackTrace();
            return "Sorry, I encountered an error while generating the summary: " + e.getMessage();
        }
    }

    private String callGeminiAPIEnhanced(String question, String documentContext) throws Exception {
        // Enhanced prompt for better multi-document analysis
        String prompt;

        if (question.equals("GENERATE_COMPREHENSIVE_SUMMARY") || question.toLowerCase().contains("summary")) {
            prompt = String.format("""
                You are an expert document analyzer tasked with providing comprehensive summaries.
                
                CRITICAL INSTRUCTIONS:
                1. Analyze ALL documents provided below carefully
                2. Create a comprehensive summary that covers information from ALL documents
                3. Organize your summary by document or by topic areas
                4. If documents are related, highlight connections and common themes
                5. If documents cover different topics, provide separate sections for each
                6. Mention the document names when referencing specific information
                
                DOCUMENTS TO ANALYZE:
                %s
                
                Please provide a comprehensive summary covering all the documents above:
                """, documentContext != null ? documentContext : "No documents provided");
        } else {
            prompt = String.format("""
                You are an expert document analyzer answering questions based on multiple documents.
                
                CRITICAL INSTRUCTIONS:
                1. Search through ALL the documents provided below thoroughly
                2. Answer the question using information from ALL relevant documents
                3. When referencing information, mention which specific document it came from
                4. If information is found in multiple documents, mention all relevant sources
                5. If the answer requires combining information from multiple documents, do so clearly
                6. If the information is not found in any document, state this explicitly
                
                DOCUMENTS TO SEARCH:
                %s
                
                QUESTION TO ANSWER: %s
                
                Please provide a comprehensive answer based on ALL the documents above:
                """, documentContext != null ? documentContext : "No documents provided", question);
        }

        // Create Gemini API request
        Map<String, Object> requestBody = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentItem = new HashMap<>();

        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);

        contentItem.put("parts", parts);
        contents.add(contentItem);
        requestBody.put("contents", contents);

        // Enhanced generation config for better multi-document responses
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 4096); // Increased for comprehensive answers
        requestBody.put("generationConfig", generationConfig);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        System.out.println("Sending enhanced request to Gemini API...");

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                jsonBody
        );

        String url = geminiApiUrl + "?key=" + geminiApiKey;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Gemini API response code: " + response.code());

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                System.err.println("Gemini API error response: " + errorBody);

                switch (response.code()) {
                    case 400:
                        return "Invalid request to Gemini API. Please check your configuration.";
                    case 403:
                        return "Access denied to Gemini API. Please check your API key permissions.";
                    case 429:
                        return "Rate limit exceeded. Please try again in a few moments.";
                    default:
                        throw new Exception("Gemini API request failed: HTTP " + response.code() + " - " + response.message());
                }
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new Exception("Empty response from Gemini API");
            }

            String responseBodyString = responseBody.string();
            System.out.println("Gemini API response received (length: " + responseBodyString.length() + ")");

            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBodyString);

            JsonNode candidatesNode = responseJson.get("candidates");
            if (candidatesNode != null && candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode firstCandidate = candidatesNode.get(0);
                JsonNode responseContentNode = firstCandidate.get("content");
                if (responseContentNode != null) {
                    JsonNode responsePartsNode = responseContentNode.get("parts");
                    if (responsePartsNode != null && responsePartsNode.isArray() && responsePartsNode.size() > 0) {
                        JsonNode firstPart = responsePartsNode.get(0);
                        JsonNode textNode = firstPart.get("text");
                        if (textNode != null && !textNode.asText().trim().isEmpty()) {
                            String result = textNode.asText().trim();
                            System.out.println("Successfully extracted enhanced AI response (length: " + result.length() + ")");
                            return result;
                        }
                    }
                }
            }

            if (responseJson.has("error")) {
                JsonNode errorNode = responseJson.get("error");
                String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                throw new Exception("Gemini API returned error: " + errorMessage);
            }

            throw new Exception("No valid response received from Gemini API. Response: " + responseBodyString);
        }
    }

    // Legacy methods for backward compatibility
    public String askQuestion(String question, String documentContext) {
        return askQuestionEnhanced(question, documentContext);
    }

    public String generateSummary(String documentContent) {
        return generateSummaryEnhanced(documentContent);
    }

    private String generateEnhancedMockResponse(String question, String documentContext) {
        int docCount = documentContext.split("=== DOCUMENT").length - 1;
        return String.format("Enhanced Mock Response: Analyzing %d documents for question '%s'. Document content length: %d characters. This would provide comprehensive analysis across all documents.",
                docCount, question, documentContext.length());
    }

    private String generateMockSummary(String documentContent) {
        int docCount = documentContent.split("=== DOCUMENT").length - 1;
        return String.format("Enhanced Mock Summary: Comprehensive summary of %d documents with total content length of %d characters. Each document would be summarized individually and connections between documents would be highlighted.",
                docCount, documentContent.length());
    }

    public void resetState() {
        lastRequestTime = 0;
        System.out.println("AI service state reset successfully");
    }

    public boolean isConfigured() {
        return !useMockAI && geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }
}

