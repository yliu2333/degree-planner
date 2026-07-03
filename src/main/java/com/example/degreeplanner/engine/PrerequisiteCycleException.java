package com.example.degreeplanner.engine;

import java.util.List;

public class PrerequisiteCycleException extends RuntimeException {
    private final List<String> cyclePath;

    public PrerequisiteCycleException(List<String> cyclePath) {
        super("Prerequisite cycle detected: " + String.join(" -> ", cyclePath));
        this.cyclePath = List.copyOf(cyclePath);
    }

    public List<String> getCyclePath() {
        return cyclePath;
    }
}
