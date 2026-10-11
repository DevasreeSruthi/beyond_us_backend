package com.beyondus.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "decisions")
public class Decision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long eventId;
    public String username;
    public String eventType;
    public int severity;
    public boolean humanIntervention;

    @Column(name = "decision_text")
    public String decision;

    public int autonomy;
    public int threat;
    public String systemStatus;

    public int independentDecision;
    public int responseIndependence;
    public int noHumanIntervention;
    public int decisionComplexity;
    public int successfulAction;
    public int streakBonus;

    @Column(name = "logged_time")
    public String time;
    public long ts;
}