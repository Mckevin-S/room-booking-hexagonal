package com.mckevin.roombooking.adapter.out.persistence;

import com.mckevin.roombooking.application.port.RoomRepository;
import com.mckevin.roombooking.domain.room.Capacity;
import com.mckevin.roombooking.domain.room.Equipment;
import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.SiteId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class RoomRepositoryAdapter implements RoomRepository {

    private final RoomJpaRepository rooms;

    RoomRepositoryAdapter(RoomJpaRepository rooms) {
        this.rooms = rooms;
    }

    @Override
    public Optional<Room> find(RoomId id) {
        return rooms.findById(id.value()).map(RoomRepositoryAdapter::toDomain);
    }

    @Override
    public List<Room> ofSite(SiteId site) {
        return rooms.findBySiteId(site.value()).stream().map(RoomRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(Room room) {
        RoomEntity entity = rooms.findById(room.id().value()).orElseGet(RoomEntity::new);
        entity.id = room.id().value();
        entity.siteId = room.siteId().value();
        entity.name = room.name();
        entity.floor = room.floor();
        entity.seats = room.capacity().seats();
        entity.equipments = room.equipments().stream().map(Enum::name).collect(Collectors.joining(","));
        entity.outOfServiceReason = room.outOfServiceReason();
        rooms.save(entity);
    }

    static Room toDomain(RoomEntity e) {
        EnumSet<Equipment> equipments = EnumSet.noneOf(Equipment.class);
        Arrays.stream(e.equipments.split(",")).map(String::strip).filter(s -> !s.isEmpty())
                .map(Equipment::valueOf).forEach(equipments::add);
        return new Room(RoomId.of(e.id), SiteId.of(e.siteId), e.name, e.floor, Capacity.of(e.seats), equipments,
                e.outOfServiceReason);
    }
}
