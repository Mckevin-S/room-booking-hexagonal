package com.mckevin.roombooking.domain.employee;

public record EmployeeId(long value) {

    public static EmployeeId of(long value) {
        return new EmployeeId(value);
    }
}
