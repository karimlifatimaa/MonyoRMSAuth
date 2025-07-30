package com.example.monyormsauth.auth.controller;

import com.example.monyormsauth.auth.dto.*;
import com.example.monyormsauth.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam("username") String username) {
        authService.logout(username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully.");
    }


//    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<String> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        authService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok("User role updated successfully.");
    }
    @GetMapping("/users/{id}/exists")
    public ResponseEntity<Boolean> doesUserExist(@PathVariable Long id) {
        boolean exists = authService.doesUserExist(id);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/users/{id}/role")
    public ResponseEntity<String> getUserRole(@PathVariable Long id) {
        String role = authService.getUserRole(id);
        return ResponseEntity.ok(role);
    }
}
