package com.authentication.auth.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AIAnalysisResponse {

    private final String emotion;
    private final String solution;

    @JsonCreator
    public AIAnalysisResponse(@JsonProperty("emotion") String emotion,
                              @JsonProperty("solution") String solution) {
        this.emotion = emotion;
        this.solution = solution;
    }
}
