package com.masterlab.spring_ai_rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.List;

@org.springframework.stereotype.Service
public class DocumentIngestionService {

    // Spring will automatically look inside src/main/resources/documents/
    @Value("${app.document.path}")
    private Resource pdfResource;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStor) {
        this.vectorStore = vectorStor;
    }

    public void ingest() {
        System.out.println("Starting PDF ingestion......");

        // 1. Check if the resource actually exists to avoid unexpected crashes
        if (!pdfResource.exists()) {
            throw new RuntimeException("Could not find the PDF file in the classpath resources!");
        }
        //2. Read the PDF
        PagePdfDocumentReader pdfDocumentReader =
                new PagePdfDocumentReader(
                        pdfResource);

        List<Document> documents = pdfDocumentReader.read();

        System.out.println("PDF loaded. Documents found: " + documents.size());

        //3. Split the documents into smaller chunks
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(200)
                .build();


        List<Document> chunks = textSplitter.split(documents);

        System.out.println("Documents split into chunks: " + chunks.size());


// NEW: Clean the chunks to fix PDF extraction issues
//        (The text stored in PGVector will look like: ARTICLES [ 390. Money received or raised...)
        List<Document> cleanedChunks = chunks.stream()
                .map(chunk -> {
                    String text = chunk.getText();

                    // 1. Remove repeating PDF headers/footers (e.g., "Contents ARTICLES")
                    text = text.replaceAll("(?i)\\(xxvii\\)\\s*Contents\\s*ARTICLES", "");
                    text = text.replaceAll("(?i)______________________________________________", "");

                    // 2. Add spaces between lowercase and uppercase letters (e.g., MoneyReceived -> Money Received)
                    text = text.replaceAll("([a-z])([A-Z])", "$1 $2");

                    // 3. Add spaces around numbers and brackets (e.g., ARTICLES[390. -> ARTICLES [ 390. )
                    text = text.replaceAll("\\[", " [ ");
                    text = text.replaceAll("\\]", " ] ");
                    text = text.replaceAll("(\\d+)\\.", " $1. "); // Space after numbers with dots

                    // 4. Remove weird control characters (like the  character in your output)
                    text = text.replaceAll("[\\p{C}]", " ");

                    // 5. Normalize whitespace (replace multiple spaces/newlines with a single space)
                    text = text.replaceAll("\\s+", " ");

                    // 6. Clean up any double spaces created by the steps above
                    text = text.replaceAll("\\s+", " ").trim();

                    return new Document(text, chunk.getMetadata());
                })
                .toList();


        //4. Generate embeddings and store them in PGVector
        vectorStore.write(cleanedChunks);

        System.out.println("Documents successfully stored in PGVector");


    }

}
