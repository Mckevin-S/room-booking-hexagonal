package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.adapter.in.web.ApiDtos.OutOfServiceRequest;
import com.mckevin.roombooking.application.RoomService;
import com.mckevin.roombooking.application.Views.OutOfServiceView;
import com.mckevin.roombooking.application.Views.RoomView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Gestion des salles (office manager)")
class RoomController {

    private final RoomService rooms;
    private final CurrentActor currentActor;

    RoomController(RoomService rooms, CurrentActor currentActor) {
        this.rooms = rooms;
        this.currentActor = currentActor;
    }

    @PutMapping("/{id}/out-of-service")
    @Operation(summary = "Mettre une salle hors service ; les réservations à venir sont annulées")
    OutOfServiceView takeOutOfService(@PathVariable String id, @Valid @RequestBody OutOfServiceRequest request) {
        return rooms.takeOutOfService(currentActor.get(), id, request.reason());
    }

    @DeleteMapping("/{id}/out-of-service")
    @Operation(summary = "Remettre une salle en service")
    RoomView putBackInService(@PathVariable String id) {
        return rooms.putBackInService(currentActor.get(), id);
    }
}
