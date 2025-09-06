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

    // ✅ ENHANCED: Much longer rate limiting with exponential backoff
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 10000; // 10 seconds minimum
    private static final long MAX_BACKOFF_MS = 300000; // 5 minutes maximum
    private int consecutiveRateLimitErrors = 0;
    private long lastSuccessfulRequestTime = 0;

    public AIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ✅ ENHANCED: Smart rate limiting with exponential backoff
    public String askQuestionEnhanced(String question, String documentContext) {
        System.out.println("AIService.askQuestionEnhanced called");
        System.out.println("Question: " + question);
        System.out.println("Document context length: " + (documentContext != null ? documentContext.length() : 0));
        System.out.println("Consecutive rate limit errors: " + consecutiveRateLimitErrors);

        if (useMockAI) {
            return generateEnhancedMockResponse(question, documentContext);
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "Gemini AI service is not configured. Please set your Gemini API key in application.properties. Get one from https://aistudio.google.com/";
        }

        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid question.";
        }

        // ✅ ENHANCED: Exponential backoff calculation
        long currentTime = System.currentTimeMillis();
        long requiredDelay = MIN_REQUEST_INTERVAL_MS;

        if (consecutiveRateLimitErrors > 0) {
            // Exponential backoff: 10s, 20s, 40s, 80s, 160s, max 5min
            long backoffMultiplier = Math.min((1L << consecutiveRateLimitErrors), 30);
            requiredDelay = Math.min(MIN_REQUEST_INTERVAL_MS * backoffMultiplier, MAX_BACKOFF_MS);
            System.out.println("⏳ Exponential backoff active: " + requiredDelay + "ms delay");
        }

        // ✅ ENFORCE: Strict rate limiting with helpful messages
        long timeSinceLastRequest = currentTime - lastRequestTime;
        if (timeSinceLastRequest < requiredDelay) {
            long waitTime = requiredDelay - timeSinceLastRequest;
            System.out.println("⏳ Rate limiting: waiting " + waitTime + "ms");

            return String.format(
                    "⏳ **AI Cooling Down** (%d seconds remaining)\n\n" +
                            "Due to high API usage, I need to wait **%d seconds** before processing your next question.\n\n" +
                            "**🔍 Meanwhile, try the search function** - it works perfectly and provides comprehensive results from your documents!\n\n" +
                            "**📊 Your question:** \"%s\"\n" +
                            "**📄 Documents ready:** All documents are loaded and searchable\n\n" +
                            "**💡 Tip:** Search function provides instant results while AI service recovers.",
                    waitTime / 1000,
                    waitTime / 1000,
                    question
            );
        }

        try {
            String result = callGeminiAPIEnhanced(question, documentContext);
            lastRequestTime = System.currentTimeMillis();

            // ✅ CHECK: Did we get rate limited in the response?
            if (result.contains("Rate limit exceeded") || result.contains("high demand") ||
                    result.contains("too many requests") || result.contains("quota exceeded")) {
                consecutiveRateLimitErrors++;
                System.out.println("❌ Rate limit detected in response. Count: " + consecutiveRateLimitErrors);

                long nextAvailableMinutes = (MIN_REQUEST_INTERVAL_MS * Math.min((1L << consecutiveRateLimitErrors), 30)) / 60000;

                return String.format(
                        "⏳ **Gemini API Temporarily Overloaded** (Attempt #%d)\n\n" +
                                "Google's AI service is experiencing high demand right now.\n\n" +
                                "**🔍 Smart Search Alternative:**\n" +
                                "While waiting for AI service to recover, use the search function for immediate, comprehensive results from your documents.\n\n" +
                                "**⏰ Next AI attempt available in:** %d minutes\n" +
                                "**📄 Your documents are fully searchable right now!**\n\n" +
                                "**💡 Search provides:** Instant results, content highlighting, and comprehensive document analysis.",
                        consecutiveRateLimitErrors,
                        Math.max(nextAvailableMinutes, 1)
                );
            }

            // ✅ SUCCESS: Reset error counter
            consecutiveRateLimitErrors = 0;
            lastSuccessfulRequestTime = System.currentTimeMillis();
            System.out.println("✅ Successful AI response received, resetting backoff");
            return result;

        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            e.printStackTrace();
            consecutiveRateLimitErrors++;

            return String.format(
                    "❌ **AI Service Error** (Attempt #%d)\n\n" +
                            "**Technical issue:** %s\n\n" +
                            "**🔍 Search is fully operational** - try using search for immediate results from your documents.\n\n" +
                            "**📄 All your documents are accessible** through the search function while AI service recovers.",
                    consecutiveRateLimitErrors,
                    e.getMessage()
            );
        }
    }

    // ✅ ENHANCED: Summary generation with same rate limiting
    public String generateSummaryEnhanced(String documentContent) {
        System.out.println("AIService.generateSummaryEnhanced called");
        System.out.println("Document content length: " + (documentContent != null ? documentContent.length() : 0));

        if (useMockAI) {
            return generateMockSummary(documentContent);
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "Gemini AI service is not configured. Please set your Gemini API key.";
        }

        // Apply same rate limiting logic as questions
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        long requiredDelay = MIN_REQUEST_INTERVAL_MS;

        if (consecutiveRateLimitErrors > 0) {
            long backoffMultiplier = Math.min((1L << consecutiveRateLimitErrors), 30);
            requiredDelay = Math.min(MIN_REQUEST_INTERVAL_MS * backoffMultiplier, MAX_BACKOFF_MS);
        }

        if (timeSinceLastRequest < requiredDelay) {
            long waitTime = requiredDelay - timeSinceLastRequest;
            return String.format(
                    "⏳ **Summary Generation Delayed**\n\n" +
                            "Please wait **%d seconds** before requesting a summary.\n\n" +
                            "**🔍 Alternative:** Use search to explore specific topics in your documents right now!",
                    waitTime / 1000
            );
        }

        try {
            String result = callGeminiAPIEnhanced("GENERATE_COMPREHENSIVE_SUMMARY", documentContent);
            lastRequestTime = System.currentTimeMillis();

            if (result.contains("Rate limit exceeded")) {
                consecutiveRateLimitErrors++;
                return "⏳ **AI Summary Temporarily Unavailable**\n\nUse search to explore your documents while AI service recovers.";
            }

            consecutiveRateLimitErrors = 0;
            return result;
        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            e.printStackTrace();
            consecutiveRateLimitErrors++;
            return "Sorry, I encountered an error while generating the summary: " + e.getMessage();
        }
    }

    // ✅ Your existing callGeminiAPIEnhanced method remains the same
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
        generationConfig.put("maxOutputTokens", 4096);
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

    // ✅ NEW: Add method to check if AI service is currently available
    public boolean isAIAvailable() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        long requiredDelay = MIN_REQUEST_INTERVAL_MS;

        if (consecutiveRateLimitErrors > 0) {
            long backoffMultiplier = Math.min((1L << consecutiveRateLimitErrors), 30);
            requiredDelay = Math.min(MIN_REQUEST_INTERVAL_MS * backoffMultiplier, MAX_BACKOFF_MS);
        }

        return timeSinceLastRequest >= requiredDelay;
    }

    // ✅ NEW: Get time until AI becomes available (in seconds)
    public long getSecondsUntilAIAvailable() {
        if (isAIAvailable()) return 0;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        long requiredDelay = MIN_REQUEST_INTERVAL_MS;

        if (consecutiveRateLimitErrors > 0) {
            long backoffMultiplier = Math.min((1L << consecutiveRateLimitErrors), 30);
            requiredDelay = Math.min(MIN_REQUEST_INTERVAL_MS * backoffMultiplier, MAX_BACKOFF_MS);
        }

        return (requiredDelay - timeSinceLastRequest) / 1000;
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
        consecutiveRateLimitErrors = 0;
        lastSuccessfulRequestTime = 0;
        System.out.println("AI service state reset successfully");
    }

    public boolean isConfigured() {
        return !useMockAI && geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }

    public String askQuestionWithContext(String question, String documentContent, Map<String, Object> context) {
        try {
            System.out.println("=== AI SERVICE: Processing question with context ===");
            System.out.println("Question: " + question);
            System.out.println("Context keys: " + (context != null ? context.keySet() : "none"));

            // ✅ DECLARE: aiRequestContext variable before using it
            Map<String, Object> aiRequestContext = new HashMap<>();
            if (context != null) {
                aiRequestContext.putAll(context); // Copy all context data
            }

            // ✅ ADD: Default context values if needed
            aiRequestContext.put("timestamp", System.currentTimeMillis());
            aiRequestContext.put("processingId", java.util.UUID.randomUUID().toString());

            // ✅ BUILD: Enhanced prompt with context
            StringBuilder promptBuilder = new StringBuilder();

            // Add system context
            promptBuilder.append("You are a helpful AI assistant analyzing documents. ");
            promptBuilder.append("Provide accurate, detailed answers based on the document content provided.\n\n");

            // Add request context if available
            if (aiRequestContext.containsKey("requestId")) {
                String requestId = String.valueOf(aiRequestContext.get("requestId"));
                String sessionId = String.valueOf(aiRequestContext.get("sessionId"));

                promptBuilder.append("REQUEST CONTEXT:\n");
                promptBuilder.append("- Request ID: ").append(requestId).append("\n");
                promptBuilder.append("- Session ID: ").append(sessionId).append("\n");
                promptBuilder.append("- Processing Time: ").append(aiRequestContext.get("timestamp")).append("\n");

                if (Boolean.TRUE.equals(aiRequestContext.get("forceRefresh"))) {
                    promptBuilder.append("- Analysis Type: FRESH_ANALYSIS_REQUIRED\n");
                }
                promptBuilder.append("\n");
            }

            // Add document content
            promptBuilder.append("DOCUMENT CONTENT:\n");
            promptBuilder.append(documentContent);
            promptBuilder.append("\n\n");

            // Add the question
            promptBuilder.append("QUESTION: ").append(question).append("\n\n");

            // Add instructions for better responses
            promptBuilder.append("INSTRUCTIONS:\n");
            promptBuilder.append("- Answer based solely on the provided document content\n");
            promptBuilder.append("- If information is not in the documents, state that clearly\n");
            promptBuilder.append("- Provide specific references to document sections when possible\n");
            promptBuilder.append("- Give comprehensive, detailed answers when the information is available\n");

            String fullPrompt = promptBuilder.toString();

            // ✅ CALL: Your existing AI method with the enhanced prompt
            String response = askQuestionEnhanced(question, fullPrompt);

            System.out.println("✅ AI response generated successfully with context");
            return response;

        } catch (Exception e) {
            System.err.println("❌ Error in askQuestionWithContext: " + e.getMessage());
            e.printStackTrace();

            // ✅ FALLBACK: Use existing method if context method fails
            try {
                return askQuestionEnhanced(question, documentContent);
            } catch (Exception fallbackError) {
                return "Error processing your question: " + e.getMessage();
            }
        }
    }

}

