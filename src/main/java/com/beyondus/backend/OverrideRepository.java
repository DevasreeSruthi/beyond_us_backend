package com.beyondus.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OverrideRepository extends JpaRepository<OverrideRecord, Long> {
    List<OverrideRecord> findByUsernameOrderByIdDesc(String username);
    void deleteByUsername(String username);
}