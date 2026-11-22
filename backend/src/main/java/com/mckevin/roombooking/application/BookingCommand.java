package com.mckevin.roombooking.application;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingCommand(String roomId, LocalDate date, LocalTime start, LocalTime end, String title, int attendees) {
}
