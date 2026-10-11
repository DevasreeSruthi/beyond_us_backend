package com.beyondus.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    public record RunRequest(String username, String eventType, Integer severity, boolean humanIntervention) {}
    public record UserRequest(String username) {}

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @PostMapping("/simulation/run")
    public ResponseEntity<?> run(@RequestBody RunRequest r) {
        if (blank(r.username()) || blank(r.eventType()) || r.severity() == null) {
            return bad("Missing input.");
        }
        try {
            return ResponseEntity.ok(service.run(r.username(), r.eventType(), r.severity(), r.humanIntervention()));
        } catch (IllegalArgumentException e) {
            return bad(e.getMessage());
        }
    }

    @PostMapping("/simulation/override")
    public ResponseEntity<?> override(@RequestBody UserRequest r) {
        if (blank(r.username())) return bad("Missing username.");
        return ResponseEntity.ok(service.takeControl(r.username()));
    }

    @PostMapping("/simulation/release")
    public ResponseEntity<?> release(@RequestBody UserRequest r) {
        if (blank(r.username())) return bad("Missing username.");
        return ResponseEntity.ok(service.release(r.username()));
    }

    @PostMapping("/simulation/reset")
    public ResponseEntity<?> reset(@RequestBody UserRequest r) {
        if (blank(r.username())) return bad("Missing username.");
        service.reset(r.username());
        return ResponseEntity.ok(Map.of("status", "reset"));
    }

    @GetMapping("/dashboard/{username}")
    public ResponseEntity<?> dashboard(@PathVariable String username) {
        return ResponseEntity.ok(service.dashboard(username));
    }
}