package com.beyondus.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;

    public AuthController(UserRepository users) {
        this.users = users;
    }

    public record AuthRequest(String username, String password) {}

    private static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest r) {
        if (r.username() == null || r.username().isBlank() || r.password() == null || r.password().length() < 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Enter a username and a password of at least 4 characters."));
        }
        String name = r.username().trim();
        if (users.findByUsername(name).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already taken."));
        }
        User u = new User();
        u.username = name;
        u.passwordHash = hash(r.password());
        users.save(u);
        return ResponseEntity.ok(Map.of("username", name));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest r) {
        if (r.username() == null || r.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter username and password."));
        }
        Optional<User> found = users.findByUsername(r.username().trim());
        if (found.isEmpty() || !found.get().passwordHash.equals(hash(r.password()))) {
            return ResponseEntity.status(401).body(Map.of("error", "Wrong username or password."));
        }
        return ResponseEntity.ok(Map.of("username", found.get().username));
    }
}