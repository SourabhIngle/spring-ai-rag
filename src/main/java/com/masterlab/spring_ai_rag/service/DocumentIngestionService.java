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
    @Value("classpath:document/Microsoft Word.pdf")
    private Resource pdfResource;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStor){
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

        System.out.println("PDF loaded. Documents found: "+ documents.size());

        //3. Split the documents into smaller chunks
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(200)
                .build();


        List<Document> chunks = textSplitter.split(documents);

        System.out.println("Documents split into chunks: " + chunks.size());

        //4. Generate embeddings and store them in PGVector
        vectorStore.write(chunks);

        System.out.println("Documents successfully stored in PGVector");


    }

}
