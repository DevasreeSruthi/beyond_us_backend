package com.beyondus.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    List<Decision> findByUsernameOrderByIdAsc(String username);
    void deleteByUsername(String username);
}