package com.masterlab.spring_ai_rag.controller;

import com.masterlab.spring_ai_rag.model.RagResponse;
import com.masterlab.spring_ai_rag.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService){
        this.ragService = ragService;

    }

    @GetMapping
    public RagResponse ask(@RequestParam String question){
            return ragService.ask(question);
    }

}
