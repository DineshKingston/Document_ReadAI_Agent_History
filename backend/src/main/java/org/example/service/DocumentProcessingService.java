package org.example.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentProcessingService {

    private final Map<String, DocumentInfo> documentStorage = new ConcurrentHashMap<>();

    public String processDocument(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            System.out.println("=== PROCESSING FILE: " + filename + " ===");
            System.out.println("Service instance: " + this);
            System.out.println("Document storage state: " + documentStorage);
            System.out.println("Current document count: " + documentStorage.size());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty: " + filename);
            }

            // Ensure storage is initialized
            if (documentStorage == null) {
                System.err.println("❌ Document storage is null! This should never happen.");
                throw new RuntimeException("Document storage not initialized");
            }

            String content = extractTextFromFile(file);

            // Enhanced debugging for all file types
            System.out.println("File: " + filename);
            System.out.println("Size: " + file.getSize() + " bytes");
            System.out.println("Extracted content length: " + content.length() + " characters");

            if (content.trim().isEmpty()) {
                throw new IllegalArgumentException("No text content extracted from file: " + filename);
            }

            String documentId = UUID.randomUUID().toString();
            DocumentInfo docInfo = new DocumentInfo(documentId, filename, content, LocalDateTime.now(), file.getSize());

            // Store the document with enhanced error handling
            try {
                documentStorage.put(documentId, docInfo);
                System.out.println("✅ Successfully stored: " + filename + " (ID: " + documentId + ")");
                System.out.println("Total documents in storage: " + documentStorage.size());

                // Verify storage worked
                if (documentStorage.containsKey(documentId)) {
                    System.out.println("✅ Storage verification passed");
                } else {
                    System.err.println("❌ Storage verification failed!");
                }

            } catch (Exception e) {
                System.err.println("❌ Error storing document: " + e.getMessage());
                throw new RuntimeException("Failed to store document: " + e.getMessage());
            }

            return "Document processed successfully: " + filename;

        } catch (Exception e) {
            System.err.println("❌ Failed to process document: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private String extractTextFromFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String lowerFilename = filename.toLowerCase();

        try (InputStream inputStream = file.getInputStream()) {
            if (lowerFilename.endsWith(".pdf")) {
                return extractFromPDF(file, inputStream);
            } else if (lowerFilename.endsWith(".docx")) {
                return extractFromDOCX(file, inputStream);
            } else if (lowerFilename.endsWith(".doc")) {
                return extractFromDOC(file, inputStream);
            } else if (lowerFilename.endsWith(".txt")) {
                return extractFromTXT(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + filename +
                        ". Supported formats: PDF, DOCX, DOC, TXT");
            }
        }
    }

    /**
     * PDFBox 3.x compatible PDF extraction method
     * Uses Loader.loadPDF() instead of PDDocument.load()
     */
    private String extractFromPDF(MultipartFile file, InputStream inputStream) throws Exception {
        try {
            // PDFBox 3.x: Use Loader.loadPDF() instead of PDDocument.load()
            byte[] pdfBytes = inputStream.readAllBytes();
            PDDocument document = Loader.loadPDF(pdfBytes);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            System.out.println("📄 PDF Processing Details:");
            System.out.println("- File: " + file.getOriginalFilename());
            System.out.println("- Pages: " + document.getNumberOfPages());
            System.out.println("- Encrypted: " + document.isEncrypted());
            System.out.println("- Extracted characters: " + text.length());

            if (text.trim().length() < 50) {
                System.out.println("⚠️ WARNING: Very short text extracted. PDF might be image-based.");
                System.out.println("Raw extracted text: '" + text + "'");
            }

            // Important: Close the document to free memory
            document.close();

            return text;
        } catch (Exception e) {
            System.err.println("❌ Error extracting from PDF: " + e.getMessage());
            throw new Exception("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private String extractFromDOCX(MultipartFile file, InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            System.out.println("📝 DOCX extracted " + text.length() + " characters from: " + file.getOriginalFilename());
            return text;
        }
    }

    private String extractFromDOC(MultipartFile file, InputStream inputStream) throws Exception {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {

            String text = extractor.getText();
            System.out.println("📝 DOC extracted " + text.length() + " characters from: " + file.getOriginalFilename());
            return text;
        }
    }

    private String extractFromTXT(MultipartFile file) throws Exception {
        String text = new String(file.getBytes(), "UTF-8");
        System.out.println("📄 TXT extracted " + text.length() + " characters from: " + file.getOriginalFilename());
        return text;
    }

    /**
     * Enhanced method to get combined content from all documents
     */
    public String getAllDocumentsContentEnhanced() {
        System.out.println("=== GET ALL DOCUMENTS DEBUG ===");
        System.out.println("Storage state: " + documentStorage);
        System.out.println("Document count: " + documentStorage.size());

        if (documentStorage.isEmpty()) {
            System.out.println("⚠️ No documents in storage");
            return null;
        }

        StringBuilder combinedContent = new StringBuilder();
        combinedContent.append("=== MULTI-DOCUMENT ANALYSIS ===\n");
        combinedContent.append("Total Documents: ").append(documentStorage.size()).append("\n\n");

        int docCount = 1;
        for (DocumentInfo doc : documentStorage.values()) {
            System.out.println("=== COMBINING DOCUMENT " + docCount + ": " + doc.getFilename() + " ===");
            String docHeader = String.format("=== DOCUMENT %d: %s ===\n", docCount, doc.getFilename());
            String docFooter = String.format("\n=== END OF DOCUMENT %d ===\n\n", docCount);

            combinedContent.append(docHeader);
            combinedContent.append(doc.getContent());
            combinedContent.append(docFooter);
            docCount++;
        }

        String finalContent = combinedContent.toString();
        System.out.println("=== FINAL COMBINED CONTENT ===");
        System.out.println("Total documents: " + documentStorage.size());
        System.out.println("Combined length: " + finalContent.length() + " characters");

        return finalContent;
    }

    // Legacy method for backward compatibility
    public String getAllDocumentsContent() {
        return getAllDocumentsContentEnhanced();
    }

    public synchronized void clearAllDocuments() {
        try {
            System.out.println("=== CLEAR DOCUMENTS DEBUG ===");
            System.out.println("Service instance: " + this);
            System.out.println("Documents before clear: " + documentStorage.size());
            System.out.println("Storage object: " + documentStorage);

            if (documentStorage != null) {
                documentStorage.clear();
                System.out.println("✅ Clear completed successfully");
            } else {
                System.err.println("❌ Document storage is null during clear!");
            }

            System.out.println("Documents after clear: " + documentStorage.size());

            // Verify clear worked
            if (documentStorage.size() == 0) {
                System.out.println("✅ Clear verification passed");
            } else {
                System.err.println("❌ Clear verification failed! Still have " + documentStorage.size() + " documents");
            }

        } catch (Exception e) {
            System.err.println("❌ Error clearing documents: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to clear documents: " + e.getMessage());
        }
    }

    public int getDocumentCount() {
        try {
            int count = documentStorage.size();
            System.out.println("Document count requested: " + count);
            return count;
        } catch (Exception e) {
            System.err.println("Error getting document count: " + e.getMessage());
            return 0;
        }
    }

    public List<String> getDocumentNames() {
        try {
            List<String> names = documentStorage.values().stream()
                    .map(DocumentInfo::getFilename)
                    .toList();
            System.out.println("Document names requested: " + names);
            return names;
        } catch (Exception e) {
            System.err.println("Error getting document names: " + e.getMessage());
            return List.of();
        }
    }

    // MongoDB Document Model for DocumentInfo
    @Document(collection = "documents")
    public static class DocumentInfo {
        @Id
        private String id;
        private String filename;
        private String content;
        private LocalDateTime uploadTime;
        private Long fileSize;
        private String fileType;

        public DocumentInfo() {}

        public DocumentInfo(String id, String filename, String content, LocalDateTime uploadTime, Long fileSize) {
            this.id = id;
            this.filename = filename;
            this.content = content;
            this.uploadTime = uploadTime;
            this.fileSize = fileSize;
            this.fileType = getFileExtension(filename);
        }

        private String getFileExtension(String filename) {
            if (filename != null && filename.contains(".")) {
                return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
            }
            return "UNKNOWN";
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public LocalDateTime getUploadTime() { return uploadTime; }
        public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
    }
}

