package com.project.emai.automation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        System.out.println("Received login attempt: " + credentials);

        String username = credentials.get("username");
        String password = credentials.get("password");

        System.out.println("Comparing: " + username + " vs " + adminUsername);
        System.out.println("And: " + password + " vs " + adminPassword);

        Map<String, Object> response = new HashMap<>();

        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            response.put("status", "success");
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Invalid credentials");
            return ResponseEntity.status(401).body(response);
        }
    }
}