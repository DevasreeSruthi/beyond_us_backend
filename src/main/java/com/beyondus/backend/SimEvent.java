package com.beyondus.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "events")
public class SimEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String username;
    public String eventType;
    public int severity;
    public boolean humanIntervention;

    @Column(name = "logged_time")
    public String time;
    public long ts;
}