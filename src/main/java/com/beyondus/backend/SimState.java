package com.beyondus.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "sim_state")
public class SimState {
    @Id
    public String username;

    public boolean humanControlled;
    public boolean lastHuman;
    public int autonomy;
    public int streak;
    public int totalDecisions;
    public int autonomousDecisions;
    public int overrides;
}