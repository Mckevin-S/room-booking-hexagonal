package com.mckevin.roombooking.application.port;

import com.mckevin.roombooking.domain.room.Room;
import com.mckevin.roombooking.domain.room.RoomId;
import com.mckevin.roombooking.domain.site.SiteId;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Optional<Room> find(RoomId id);

    List<Room> ofSite(SiteId site);

    void save(Room room);
}
