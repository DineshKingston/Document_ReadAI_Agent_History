package org.example.controller;

import java.util.HashMap;
import java.util.Map;

import org.example.model.User;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // For Lambda compatibility
public class AuthController {

    @Autowired
    private UserService userService;

    // Login using JSON request body: {"username":"...", "password":"..."}
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || password == null) {
                response.put("success", false);
                response.put("error", "Username and password are required");
                return ResponseEntity.badRequest().body(response);
            }

            boolean isValid = userService.validateUser(username, password);
            if (isValid) {
                User user = userService.findByUsername(username);
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("userId", user.getId());
                response.put("username", user.getUsername());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Invalid credentials");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Login error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Register using JSON request body: {"username":"...", "password":"..."}
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> registerRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");

            if (username == null || password == null) {
                response.put("success", false);
                response.put("error", "Username and password are required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userService.userExists(username)) {
                response.put("success", false);
                response.put("error", "Username already exists");
                return ResponseEntity.badRequest().body(response);
            }

            User newUser = userService.createUser(username, password);
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", newUser.getId());
            response.put("username", newUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Registration error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Simple test endpoint
    @GetMapping("/data")
    public String rev() {
        return "Hello";
    }
}
