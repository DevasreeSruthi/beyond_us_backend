package com.beyondus.backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<SimState, String> {
}