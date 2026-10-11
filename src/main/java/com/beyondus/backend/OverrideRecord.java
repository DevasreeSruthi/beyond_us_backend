package com.beyondus.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "overrides")
public class OverrideRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String username;
    public String action;
    public int autonomyBefore;

    @Column(name = "logged_time")
    public String time;
    public long ts;
}