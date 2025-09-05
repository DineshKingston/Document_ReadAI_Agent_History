package org.example.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.example.service.AIService;
import org.example.service.DocumentProcessingService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Lambda compatibility - allows all origins
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    // ============================================
    // HEALTH CHECK ENDPOINT (Essential for testing)
    // ============================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== HEALTH CHECK DEBUG ===");

            // Test document service
            int docCount = documentProcessingService.getDocumentCount();
            List<String> docNames = documentProcessingService.getDocumentNames();

            // Test AI service
            boolean aiReady = aiService != null && aiService.isConfigured();

            response.put("success", true);
            response.put("message", "🚀 Multi-Document AI Search System is running on AWS Lambda");
            response.put("documentService", "healthy");
            response.put("aiService", aiReady ? "healthy" : "available but not configured");
            response.put("totalDocuments", docCount);
            response.put("documentNames", docNames);
            response.put("timestamp", System.currentTimeMillis());
            response.put("environment", "AWS Lambda");
            response.put("version", "1.0.0");

            System.out.println("✅ Health check completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Health check failed: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Service unhealthy: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<Void> handleOptionsAI() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    // ============================================
    // AI QUERY ENDPOINT
    // ============================================

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String question = request.get("question");
            System.out.println("=== AI QUERY DEBUG ===");
            System.out.println("Question received: " + question);

            if (question == null || question.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Question is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Get combined content from ALL uploaded documents
            String allDocumentsContent = documentProcessingService.getAllDocumentsContentEnhanced();

            System.out.println("Document count: " + documentProcessingService.getDocumentCount());
            System.out.println("Document names: " + documentProcessingService.getDocumentNames());
            System.out.println("Combined content length: " + (allDocumentsContent != null ? allDocumentsContent.length() : 0));

            if (allDocumentsContent == null || allDocumentsContent.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "No documents uploaded. Please upload documents first using the /api/ai/upload/multiple endpoint.");
                response.put("availableEndpoints", List.of("/api/ai/upload/multiple", "/api/ai/health", "/api/ai/status"));
                return ResponseEntity.badRequest().body(response);
            }

            // Pass combined content to AI service
            String answer = aiService.askQuestionEnhanced(question, allDocumentsContent);

            response.put("success", true);
            response.put("answer", answer);
            response.put("question", question);
            response.put("documentsAnalyzed", documentProcessingService.getDocumentCount());
            response.put("documentNames", documentProcessingService.getDocumentNames());
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ AI query processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error in AI query: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error processing question: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // DOCUMENT SUMMARY ENDPOINT
    // ============================================

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== SUMMARY GENERATION DEBUG ===");

            String allDocumentsContent = documentProcessingService.getAllDocumentsContentEnhanced();
            System.out.println("Document count for summary: " + documentProcessingService.getDocumentCount());
            System.out.println("Content length: " + (allDocumentsContent != null ? allDocumentsContent.length() : 0));

            if (allDocumentsContent == null || allDocumentsContent.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "No documents uploaded. Please upload documents first using the /api/ai/upload/multiple endpoint.");
                response.put("availableEndpoints", List.of("/api/ai/upload/multiple", "/api/ai/health", "/api/ai/status"));
                return ResponseEntity.badRequest().body(response);
            }

            String summary = aiService.generateSummaryEnhanced(allDocumentsContent);

            response.put("success", true);
            response.put("summary", summary);
            response.put("documentsAnalyzed", documentProcessingService.getDocumentCount());
            response.put("documentNames", documentProcessingService.getDocumentNames());
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Summary generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error generating summary: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error generating summary: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // SINGLE FILE UPLOAD ENDPOINT
    // ============================================

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== SINGLE FILE UPLOAD DEBUG ===");
            System.out.println("File: " + file.getOriginalFilename() + " (size: " + file.getSize() + " bytes)");

            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty: " + file.getOriginalFilename());
                return ResponseEntity.badRequest().body(response);
            }

            String result = documentProcessingService.processDocument(file);

            response.put("success", true);
            response.put("message", "File uploaded successfully: " + file.getOriginalFilename());
            response.put("filename", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("totalDocuments", documentProcessingService.getDocumentCount());
            response.put("documentNames", documentProcessingService.getDocumentNames());
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Single file upload successful: " + file.getOriginalFilename());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error in single file upload: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error uploading file: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // MULTIPLE FILES UPLOAD ENDPOINT
    // ============================================

    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("=== MULTI-FILE UPLOAD DEBUG ===");
            System.out.println("Received " + files.length + " files for upload");

            if (files.length == 0) {
                response.put("success", false);
                response.put("error", "No files provided");
                return ResponseEntity.badRequest().body(response);
            }

            // ✅ ENHANCED: Clear and reinitialize document storage
            documentProcessingService.clearAllDocuments();

            int successCount = 0;
            List<String> successFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();
            long totalSize = 0;

            for (MultipartFile file : files) {
                try {
                    System.out.println("=== PROCESSING FILE: " + file.getOriginalFilename() + " ===");
                    System.out.println("File size: " + file.getSize() + " bytes");
                    System.out.println("Content type: " + file.getContentType());

                    if (file.isEmpty()) {
                        String error = file.getOriginalFilename() + " (file is empty)";
                        failedFiles.add(error);
                        System.out.println("❌ Skipping empty file: " + file.getOriginalFilename());
                        continue;
                    }

                    // ✅ ENHANCED: Validate content before processing
                    if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                        String error = file.getOriginalFilename() + " (file too large: " + file.getSize() + " bytes)";
                        failedFiles.add(error);
                        System.out.println("❌ File too large: " + file.getOriginalFilename());
                        continue;
                    }

                    String result = documentProcessingService.processDocument(file);

                    // ✅ CRITICAL: Verify document was actually stored
                    int documentsAfterProcessing = documentProcessingService.getDocumentCount();
                    if (documentsAfterProcessing > successCount) {
                        successCount++;
                        successFiles.add(file.getOriginalFilename());
                        totalSize += file.getSize();
                        System.out.println("✅ Successfully processed and stored: " + file.getOriginalFilename());
                    } else {
                        String error = file.getOriginalFilename() + " (processing completed but document not stored)";
                        failedFiles.add(error);
                        System.out.println("❌ Processing completed but storage failed: " + file.getOriginalFilename());
                    }

                } catch (Exception e) {
                    String error = file.getOriginalFilename() + " (" + e.getMessage() + ")";
                    failedFiles.add(error);
                    System.err.println("❌ Failed to process: " + file.getOriginalFilename() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // ✅ ENHANCED: Final verification
            int finalDocumentCount = documentProcessingService.getDocumentCount();
            List<String> finalDocumentNames = documentProcessingService.getDocumentNames();

            System.out.println("=== FINAL RESULTS ===");
            System.out.println("Success count: " + successCount);
            System.out.println("Final document count: " + finalDocumentCount);
            System.out.println("Document names: " + finalDocumentNames);

            boolean overallSuccess = finalDocumentCount > 0;

            response.put("success", overallSuccess);
            response.put("message", "Processed " + successCount + " out of " + files.length + " files");
            response.put("totalDocuments", finalDocumentCount);
            response.put("successCount", successCount);
            response.put("successFiles", successFiles);
            response.put("failedFiles", failedFiles);
            response.put("failCount", failedFiles.size());
            response.put("documentNames", finalDocumentNames);
            response.put("totalUploadSize", totalSize);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Critical error in uploadMultipleFiles: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("error", "Critical processing error: " + e.getMessage());
            response.put("totalDocuments", 0);
            response.put("successCount", 0);
            response.put("failCount", files.length);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(response);
        }
    }


    // ============================================
    // STATUS ENDPOINT
    // ============================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            int documentCount = documentProcessingService.getDocumentCount();
            List<String> documentNames = documentProcessingService.getDocumentNames();
            boolean aiConfigured = aiService != null && aiService.isConfigured();

            response.put("success", true);
            response.put("totalDocuments", documentCount);
            response.put("documentNames", documentNames);
            response.put("ready", documentCount > 0);
            response.put("aiConfigured", aiConfigured);
            response.put("timestamp", System.currentTimeMillis());
            response.put("environment", "AWS Lambda");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error getting status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // CLEAR DOCUMENTS ENDPOINT
    // ============================================

    @DeleteMapping("/documents")
    public ResponseEntity<Map<String, Object>> clearAllDocuments() {
        Map<String, Object> response = new HashMap<>();
        try {
            int documentCount = documentProcessingService.getDocumentCount();
            documentProcessingService.clearAllDocuments();

            if (aiService != null) {
                aiService.resetState();
            }

            response.put("success", true);
            response.put("message", "All documents cleared successfully.");
            response.put("clearedCount", documentCount);
            response.put("totalDocuments", 0);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error clearing documents: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================
    // API INFORMATION ENDPOINT
    // ============================================

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("systemName", "Multi-Document AI Search System");
        response.put("version", "1.0.0");
        response.put("environment", "AWS Lambda");
        response.put("description", "AI-powered document analysis system supporting PDF, DOCX, DOC, and TXT files");

        response.put("endpoints", Map.of(
                "GET /api/ai/health", "System health check",
                "GET /api/ai/status", "Current system status",
                "POST /api/ai/upload", "Upload single document",
                "POST /api/ai/upload/multiple", "Upload multiple documents",
                "POST /api/ai/ask", "Query documents with AI",
                "GET /api/ai/summary", "Get document summary",
                "DELETE /api/ai/documents", "Clear all documents"
        ));

        response.put("supportedFileTypes", List.of("PDF", "DOCX", "DOC", "TXT"));
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
