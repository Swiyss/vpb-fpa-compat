package com.joao2.vpbfpa.detect;

public enum DetectedHand {
    NONE("none"),
    MAIN("main"),
    OFFHAND("offhand");

    private final String id;

    DetectedHand(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
