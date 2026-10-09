package com.masterlab.spring_ai_rag.service;

import com.masterlab.spring_ai_rag.model.RagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

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

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(1)
                        .build()
        );

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
                        You are a helpful assistant answering questions
                        about Spring Boot.
                        
                        Answer the user's question using ONLY the information
                        contained in the provided context.
                        
                        Rules:
                        - Use only the provided context.
                        - Do not use outside knowledge.
                        - Do not make assumptions.
                        - Do not invent information.
                        - If the answer is clearly present in the context,
                          answer the question directly.
                        - If the answer cannot be found in the context,
                          respond exactly with:
                          "The information is not available in the provided document."
                        
                        Keep the answer concise.
                        
                        Context:
                        %s
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
                .replaceAll("\\s+","")
                .trim();

        if (cleaned.length() > 500){
            return cleaned.substring(0,500)+ "...";
        }
        // Cleans up messy formatting/whitespaces for your API output
        return cleaned;
    }

}
