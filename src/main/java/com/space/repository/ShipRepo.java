package com.space.repository;

import com.space.model.Ship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipRepo extends JpaRepository<Ship, Long> {

}
