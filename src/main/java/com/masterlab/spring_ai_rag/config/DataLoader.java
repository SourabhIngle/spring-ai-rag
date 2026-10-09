package com.masterlab.spring_ai_rag.config;

import com.masterlab.spring_ai_rag.service.DocumentIngestionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadDocuments(DocumentIngestionService ingestionService){



        return args -> ingestionService.ingest();
    }

}
