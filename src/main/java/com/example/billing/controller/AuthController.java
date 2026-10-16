package com.example.billing.controller;

import com.example.billing.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresInMs) {}

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        // Throws BadCredentialsException (-> 401) if the password is wrong
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        String token = jwtService.generateToken((UserDetails) auth.getPrincipal());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}
