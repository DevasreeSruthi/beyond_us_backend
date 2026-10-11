package com.beyondus.backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<SimEvent, Long> {
    void deleteByUsername(String username);
}