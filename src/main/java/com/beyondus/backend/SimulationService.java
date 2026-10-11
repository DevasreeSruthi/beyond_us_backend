package com.beyondus.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SimulationService {

    private final EventRepository events;
    private final DecisionRepository decisions;
    private final OverrideRepository overrides;
    private final StateRepository states;

    public SimulationService(EventRepository events, DecisionRepository decisions,
                             OverrideRepository overrides, StateRepository states) {
        this.events = events;
        this.decisions = decisions;
        this.overrides = overrides;
        this.states = states;
    }

    public record StateView(String systemStatus, int autonomy, int streak,
                            int totalDecisions, int autonomousDecisions, int overrides) {}

    public record Analytics(double averageAutonomy, int peakAutonomy, int peakThreat,
                            int autonomousDecisions, int overrides) {}

    public record Dashboard(StateView state, List<Decision> decisions, List<OverrideRecord> overrides,
                            Analytics analytics, List<Integer> autonomyTrend) {}

    private static String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private SimState stateOf(String user) {
        return states.findById(user).orElseGet(() -> {
            SimState s = new SimState();
            s.username = user;
            return states.save(s);
        });
    }

    private StateView view(SimState s) {
        String status;
        if (s.humanControlled || s.lastHuman) status = "HUMAN CONTROLLED";
        else if (s.totalDecisions == 0) status = "READY";
        else status = "AUTONOMOUS";
        return new StateView(status, s.autonomy, s.streak, s.totalDecisions, s.autonomousDecisions, s.overrides);
    }

    // ---------------------------------------------------------------- RUN SIMULATION
    @Transactional
    public Decision run(String user, String eventType, int severity, boolean humanFlag) {
        if (severity < 0 || severity > 100) {
            throw new IllegalArgumentException("Severity must be between 0 and 100.");
        }

        int respBase = 0;
        int typeWeight = 0;
        double multiplier = 1.0;
        switch (eventType) {
            case "NORMAL_OPERATION" -> { respBase = 60; typeWeight = 20; multiplier = 0.5; }
            case "MINOR_ANOMALY"    -> { respBase = 70; typeWeight = 40; multiplier = 0.8; }
            case "MAJOR_ANOMALY"    -> { respBase = 80; typeWeight = 60; multiplier = 1.0; }
            case "CRITICAL_EVENT"   -> { respBase = 90; typeWeight = 80; multiplier = 1.1; }
            default -> throw new IllegalArgumentException("Unknown event type.");
        }

        SimState st = stateOf(user);
        boolean human = humanFlag || st.humanControlled;

        // 1. Decision rules
        String decision = "CONTINUE MONITORING";
        if (human) {
            decision = "HUMAN DECISION";
        } else {
            switch (eventType) {
                case "MINOR_ANOMALY" -> decision = severity < 30 ? "CONTINUE MONITORING" : "INCREASE MONITORING";
                case "MAJOR_ANOMALY" -> decision = severity < 60 ? "INCREASE MONITORING" : "REDIRECT RESOURCES";
                case "CRITICAL_EVENT" -> decision = severity >= 80 ? "ISOLATE AFFECTED NODE" : "REDIRECT RESOURCES";
                default -> decision = "CONTINUE MONITORING";
            }
        }

        // 2. Autonomy factors (0-100 each)
        int independent = human ? 0 : 100;
        int response = human ? 0 : respBase;
        int noHuman = human ? 0 : 100;
        int complexity = human ? 0 : Math.round((typeWeight + severity) / 2f);
        int success = human ? 0 : (int) Math.round(100 - severity * 0.4);

        // 3. Streak: consecutive autonomous decisions raise autonomy
        int bonus = 0;
        if (human) {
            st.streak = 0;
        } else {
            st.streak++;
            bonus = Math.min(10, (st.streak - 1) * 2);
        }

        // 4. Weighted autonomy
        double raw = 0.30 * independent + 0.25 * response + 0.20 * noHuman
                   + 0.15 * complexity + 0.10 * success;
        int autonomy = Math.min(100, (int) Math.round(raw) + bonus);
        if (human) autonomy = 0;

        // 5. Threat (separate from autonomy)
        int threat = Math.min(100, (int) Math.round(severity * multiplier));

        // 6. Save the event
        SimEvent ev = new SimEvent();
        ev.username = user;
        ev.eventType = eventType;
        ev.severity = severity;
        ev.humanIntervention = humanFlag;
        ev.time = now();
        ev.ts = System.currentTimeMillis();
        ev = events.save(ev);

        // 7. Save the decision
        Decision d = new Decision();
        d.eventId = ev.id;
        d.username = user;
        d.eventType = eventType;
        d.severity = severity;
        d.humanIntervention = humanFlag;
        d.decision = decision;
        d.autonomy = autonomy;
        d.threat = threat;
        d.systemStatus = human ? "HUMAN CONTROLLED" : "AUTONOMOUS";
        d.independentDecision = independent;
        d.responseIndependence = response;
        d.noHumanIntervention = noHuman;
        d.decisionComplexity = complexity;
        d.successfulAction = success;
        d.streakBonus = bonus;
        d.time = ev.time;
        d.ts = ev.ts;
        d = decisions.save(d);

        // 8. Update the simulation state
        st.autonomy = autonomy;
        st.totalDecisions++;
        st.lastHuman = human && !st.humanControlled;
        if (!human) st.autonomousDecisions++;
        states.save(st);

        return d;
    }

    // ---------------------------------------------------------------- THE LAST SWITCH
    @Transactional
    public StateView takeControl(String user) {
        SimState st = stateOf(user);
        OverrideRecord o = new OverrideRecord();
        o.username = user;
        o.action = "HUMAN OVERRIDE ACTIVATED: THE LAST SWITCH";
        o.autonomyBefore = st.autonomy;
        o.time = now();
        o.ts = System.currentTimeMillis();
        overrides.save(o);

        st.humanControlled = true;
        st.lastHuman = false;
        st.autonomy = 0;
        st.streak = 0;
        st.overrides++;
        states.save(st);
        return view(st);
    }

    @Transactional
    public StateView release(String user) {
        SimState st = stateOf(user);
        OverrideRecord o = new OverrideRecord();
        o.username = user;
        o.action = "CONTROL RELEASED: SYSTEM AUTONOMOUS AGAIN";
        o.autonomyBefore = 0;
        o.time = now();
        o.ts = System.currentTimeMillis();
        overrides.save(o);

        st.humanControlled = false;
        st.lastHuman = false;
        states.save(st);
        return view(st);
    }

    @Transactional
    public void reset(String user) {
        decisions.deleteByUsername(user);
        events.deleteByUsername(user);
        overrides.deleteByUsername(user);
        states.deleteById(user);
    }

    // ---------------------------------------------------------------- DASHBOARD + ANALYTICS
    @Transactional
    public Dashboard dashboard(String user) {
        SimState st = stateOf(user);
        List<Decision> all = decisions.findByUsernameOrderByIdAsc(user);

        List<Decision> latest = new ArrayList<>(all);
        Collections.reverse(latest);
        if (latest.size() > 20) latest = new ArrayList<>(latest.subList(0, 20));

        List<Integer> trend = all.stream()
                .skip(Math.max(0, all.size() - 20))
                .map(d -> d.autonomy)
                .toList();

        double avg = all.stream().mapToInt(d -> d.autonomy).average().orElse(0);
        int peakA = all.stream().mapToInt(d -> d.autonomy).max().orElse(0);
        int peakT = all.stream().mapToInt(d -> d.threat).max().orElse(0);

        List<OverrideRecord> ov = overrides.findByUsernameOrderByIdDesc(user);
        if (ov.size() > 20) ov = new ArrayList<>(ov.subList(0, 20));

        Analytics an = new Analytics(Math.round(avg * 10) / 10.0, peakA, peakT,
                st.autonomousDecisions, st.overrides);
        return new Dashboard(view(st), latest, ov, an, trend);
    }
}