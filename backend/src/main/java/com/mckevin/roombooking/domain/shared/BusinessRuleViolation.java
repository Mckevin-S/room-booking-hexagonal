package com.mckevin.roombooking.domain.shared;

/** Règle métier enfreinte. Le code est stable : l'API et le front s'appuient dessus. */
public class BusinessRuleViolation extends RuntimeException {

    private final String code;

    public BusinessRuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
