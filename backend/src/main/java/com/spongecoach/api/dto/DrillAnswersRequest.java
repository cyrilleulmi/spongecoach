package com.spongecoach.api.dto;

import java.util.List;

/**
 * Answers to a Drill's open Clarifying questions. With {@code guess}, the interpreter decides
 * whatever is still open itself ("Rate einfach"); {@code answers} may then be empty.
 */
public record DrillAnswersRequest(List<Answer> answers, boolean guess, String message) {

    public record Answer(String questionId, String text) {
    }
}
