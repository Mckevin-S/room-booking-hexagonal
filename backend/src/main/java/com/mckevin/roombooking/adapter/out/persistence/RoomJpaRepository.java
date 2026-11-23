package com.mckevin.roombooking.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {

    List<RoomEntity> findBySiteId(String siteId);
}
