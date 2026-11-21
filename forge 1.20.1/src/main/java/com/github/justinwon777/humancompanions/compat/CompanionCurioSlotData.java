package com.github.justinwon777.humancompanions.compat;

public record CompanionCurioSlotData(String identifier, int index) {
    public static final int MAX_SLOTS = 256;
    public static final int MAX_IDENTIFIER_LENGTH = 128;

    public CompanionCurioSlotData {
        if (identifier == null || identifier.isEmpty()
                || identifier.length() > MAX_IDENTIFIER_LENGTH
                || index < 0 || index > 4095) {
            throw new IllegalArgumentException(
                    "Invalid companion Curios slot address");
        }
    }
}