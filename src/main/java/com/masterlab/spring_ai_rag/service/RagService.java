package com.masterlab.spring_ai_rag.service;

import com.masterlab.spring_ai_rag.model.RagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore,
                      ChatClient.Builder chatClientBuilder) {

        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();

    }

    public RagResponse ask(String question) {
        // 1. Retrieve the most relevant chunks from PGvector

        List<Document> rawDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(10)
                        .build()
        );

        // 2. Deduplicate chunks (because PDF pages often repeat headers/footers)
        List<Document> documents = rawDocuments.stream()
                .collect(Collectors.toMap(
                        doc -> doc.getText().trim(), // Key by text content
                        doc -> doc,
                        (existing, replacement) -> existing // Keep the first occurrence
                ))
                .values().stream()
                .limit(3) // Now take the top 3 UNIQUE chunks
                .toList();

        System.out.println("=======================================================");
        System.out.println("QUESTION: " + question);
        System.out.println("RETRIEVED DOCUMENTS: " + documents.size());
        System.out.println("=======================================================");


        //2. Build the full context for the LLM
        String context = documents.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n\n" + b);


        System.out.println("RETRIEVED CONTEXT: ");
        System.out.println(context);
        System.out.println("=======================================================");


        //Ask the LLM using the retrieved context
        String answer = chatClient
                .prompt()
                .system("""
                        You are a professional AI assistant. Answer the user's question based ONLY on the provided context.
                        
                        CRITICAL INSTRUCTIONS FOR DOCUMENT ANALYSIS:
                        - The context is extracted from a PDF and may contain severe formatting errors, missing spaces, or Table of Contents fragments.
                        - If the user asks for a specific Article, Section, or Clause number (e.g., "Article 390"), scan the context carefully for that exact number, even if it is stuck to other words (e.g., "[390.Money").
                        - If the context clearly states an Article is "Omitted", tell the user it was omitted.
                        - Do not use outside knowledge. Do not make assumptions.
                        - If the answer cannot be found in the context, respond exactly with:
                          "The information is not available in the provided document."
                        
                        Keep the answer concise and professional.
                        
                        Context:
                        <context>
                        %s
                        </context>
                        """.formatted(context))
                .user(question)
                .call()
                .content();


        //4. Clean source ONLY for the API response
        List<RagResponse.Source> sources = documents.stream()
                .map(Document::getText)
                .map(this::cleanSourceText)
                .map(RagResponse.Source::new)
                .toList();

        //5. Return the RAG response
        return new RagResponse(
                question,
                answer,
                sources
        );


    }
    private String cleanSourceText(String text) {
        String cleaned = text
                .replaceAll("\\s+"," ")
                .trim();

        if (cleaned.length() > 1000){
            return cleaned.substring(0,1000)+ "...";
        }
        // Cleans up messy formatting/whitespaces for your API output
        return cleaned;
    }


}
