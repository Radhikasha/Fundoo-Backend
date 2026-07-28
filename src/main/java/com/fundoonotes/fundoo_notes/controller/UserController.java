package com.fundoonotes.fundoo_notes.controller;

import com.fundoonotes.fundoo_notes.dto.*;
import com.fundoonotes.fundoo_notes.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDTO dto) {
        String message = userService.register(dto);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    // VERIFY EMAIL WITH TOKEN (OLD - keep for backward compatibility)
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam String token) {
        String message = userService.verifyEmail(token);
        return ResponseEntity.ok(new ApiResponse(200, message));
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        ApiResponse response = ApiResponse.builder()
                .status(200)
                .message("Login successful")
                .data(token)
                .build();
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(response);
    }

    // LOGOUT — blacklist the current token
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        String message = userService.logout(token);
        return ResponseEntity.ok(new ApiResponse(200, message));
    }

    // FORGOT PASSWORD — sends a reset link to the user's email
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        String message = userService.forgotPassword(dto.getEmail());
        return ResponseEntity.ok(new ApiResponse(200, message));
    }

    // RESET PASSWORD — resets password using the token from the email link
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        String message = userService.resetPassword(
                dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(new ApiResponse(200, message));
    }
} 