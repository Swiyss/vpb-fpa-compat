package com.joao2.vpbfpa.detect;

public record DetectionResult(
        boolean active,
        DetectedHand hand,
        String itemId,
        DetectionMethod method,
        boolean fallbackUsed
) {
    public static DetectionResult inactive() {
        return new DetectionResult(false, DetectedHand.NONE, "none", DetectionMethod.NONE, false);
    }

    public String summary() {
        return "active=" + active
                + ", hand=" + hand.id()
                + ", item=" + itemId
                + ", method=" + method.id()
                + ", fallback=" + fallbackUsed;
    }
}
