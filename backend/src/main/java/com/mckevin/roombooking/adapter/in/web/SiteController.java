package com.mckevin.roombooking.adapter.in.web;

import com.mckevin.roombooking.application.AvailabilityService;
import com.mckevin.roombooking.application.RoomService;
import com.mckevin.roombooking.application.Views.PlanningView;
import com.mckevin.roombooking.application.Views.RoomView;
import com.mckevin.roombooking.application.Views.SearchView;
import com.mckevin.roombooking.application.Views.SiteView;
import com.mckevin.roombooking.domain.room.Equipment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
@Tag(name = "Sites et disponibilités")
class SiteController {

    private final AvailabilityService availability;
    private final RoomService rooms;
    private final CurrentActor currentActor;

    SiteController(AvailabilityService availability, RoomService rooms, CurrentActor currentActor) {
        this.availability = availability;
        this.rooms = rooms;
        this.currentActor = currentActor;
    }

    @GetMapping
    @Operation(summary = "Sites de l'entreprise")
    List<SiteView> sites() {
        return availability.sites();
    }

    @GetMapping("/{siteId}/rooms")
    @Operation(summary = "Salles du site, de la plus petite à la plus grande")
    List<RoomView> rooms(@PathVariable String siteId) {
        return rooms.rooms(siteId);
    }

    @GetMapping("/{siteId}/availability")
    @Operation(summary = "Salles libres pour une réunion, ou créneaux proches si aucune ne l'est")
    SearchView availability(@PathVariable String siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
            @RequestParam(defaultValue = "1") int attendees,
            @RequestParam(required = false) Set<Equipment> equipments) {
        return availability.search(siteId, date, start, end, attendees,
                equipments == null ? EnumSet.noneOf(Equipment.class) : equipments);
    }

    @GetMapping("/{siteId}/planning")
    @Operation(summary = "Réservations de la journée, salle par salle")
    PlanningView planning(@PathVariable String siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availability.planning(currentActor.get(), siteId, date);
    }
}
