package com.joao2.vpbfpa.detect;

public enum DetectionMethod {
    NONE("none"),
    CLASS("class"),
    REGISTRY_FALLBACK("registry_fallback");

    private final String id;

    DetectionMethod(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
