package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.adapter.in.web.ApiDtos.BookingRequest;
import com.mckevin.roombooking.adapter.in.web.ApiDtos.ReasonRequest;
import com.mckevin.roombooking.application.BookingCommand;
import com.mckevin.roombooking.application.BookingService;
import com.mckevin.roombooking.application.Views.BookingView;
import com.mckevin.roombooking.domain.booking.BookingId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Réservations")
class BookingController {

    private final BookingService bookings;
    private final CurrentActor currentActor;

    BookingController(BookingService bookings, CurrentActor currentActor) {
        this.bookings = bookings;
        this.currentActor = currentActor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Réserver une salle")
    BookingView book(@Valid @RequestBody BookingRequest request) {
        return bookings.book(currentActor.get(), new BookingCommand(request.roomId(), request.date(), request.start(),
                request.end(), request.title(), request.attendees()));
    }

    @GetMapping("/mine")
    @Operation(summary = "Mes réservations à venir (ou passées avec past=true)")
    List<BookingView> mine(@RequestParam(defaultValue = "false") boolean past) {
        return bookings.mine(currentActor.get(), past);
    }

    @PostMapping("/{id}/check-in")
    @Operation(summary = "Confirmer sa présence (de 10 min avant à 15 min après le début)")
    BookingView checkIn(@PathVariable String id) {
        return bookings.checkIn(currentActor.get(), BookingId.of(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler une réservation (motif obligatoire pour un office manager)")
    BookingView cancel(@PathVariable String id, @Valid @RequestBody(required = false) ReasonRequest request) {
        return bookings.cancel(currentActor.get(), BookingId.of(id), request == null ? null : request.reason());
    }
}
