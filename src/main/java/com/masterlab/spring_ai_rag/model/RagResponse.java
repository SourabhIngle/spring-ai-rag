package com.masterlab.spring_ai_rag.model;

import java.util.List;

public record RagResponse(String question, String answer, List<Source> sources) {

    public record Source(String content) {

    }


}
