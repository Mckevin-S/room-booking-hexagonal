package com.mckevin.roombooking.domain.room;

public enum Equipment {
    SCREEN("Écran"),
    VIDEO_CONFERENCE("Visioconférence"),
    WHITEBOARD("Tableau blanc"),
    ACCESSIBLE("Accessible PMR");

    private final String label;

    Equipment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
