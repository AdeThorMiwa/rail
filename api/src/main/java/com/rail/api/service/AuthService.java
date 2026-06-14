package com.rail.api.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.rail.api.dto.AuthResponse;
import com.rail.api.dto.GoogleAuthRequest;
import com.rail.api.dto.LoginRequest;
import com.rail.api.dto.RefreshRequest;
import com.rail.api.dto.RegisterRequest;
import com.rail.api.entity.AuthProvider;
import com.rail.api.entity.User;
import com.rail.api.repository.UserRepository;
import com.rail.api.security.GoogleTokenVerifier;
import com.rail.api.security.RefreshTokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final OnboardingService onboardingService;
    private final ChatSetupService chatSetupService;

    @Value("${rail.jwt.refresh-expiration-days}")
    private int refreshExpirationDays;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered"
            );
        }

        return registerUser(
            req.email(),
            req.displayName(),
            AuthProvider.EMAIL,
            passwordEncoder.encode(req.password())
        );
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository
            .findByEmail(req.email())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
                )
            );

        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Use Google sign-in for this account"
            );
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
            );
        }

        return buildAuthResponse(user);
    }

    public AuthResponse googleAuth(GoogleAuthRequest req) {
        GoogleIdToken.Payload payload = googleTokenVerifier
            .verify(req.idToken())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Google token"
                )
            );

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String displayName = resolveGoogleDisplayName(payload, email);

        var existing = userRepository.findByGoogleId(googleId);
        if (existing.isPresent()) {
            return buildAuthResponse(existing.get());
        }

        userRepository.findByEmail(email).ifPresent(byEmail -> {
            if (byEmail.getAuthProvider() == AuthProvider.EMAIL) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists. Please use email/password login."
                );
            }
        });

        try {
            return registerGoogleUser(email, displayName, googleId);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByGoogleId(googleId)
                .map(this::buildAuthResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Account conflict"));
        }
    }

    public AuthResponse refresh(RefreshRequest req) {
        String tokenHash = hash(req.refreshToken());
        UUID userPid = refreshTokenStore
            .findUserPid(tokenHash)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token"
                )
            );

        User user = userRepository
            .findByPid(userPid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
                )
            );

        refreshTokenStore.delete(tokenHash);
        return buildAuthResponse(user);
    }

    private AuthResponse registerUser(
        String email,
        String displayName,
        AuthProvider provider,
        String passwordHash
    ) {
        User user = User.builder()
            .email(email)
            .displayName(displayName)
            .passwordHash(passwordHash)
            .authProvider(provider)
            .build();

        User saved = userRepository.save(user);
        chatSetupService.initialize(saved);
        return buildAuthResponse(saved);
    }

    private AuthResponse registerGoogleUser(
        String email,
        String displayName,
        String googleId
    ) {
        User user = User.builder()
            .email(email)
            .displayName(displayName)
            .googleId(googleId)
            .authProvider(AuthProvider.GOOGLE)
            .build();

        User saved = userRepository.save(user);
        chatSetupService.initialize(saved);
        return buildAuthResponse(saved);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.delete(hash(refreshToken));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generate(
            user.getPid(),
            user.getEmail()
        );
        String refreshToken = generateRefreshToken();
        refreshTokenStore.save(
            user.getPid(),
            hash(refreshToken),
            Duration.ofDays(refreshExpirationDays)
        );
        return new AuthResponse(
            accessToken,
            refreshToken,
            new AuthResponse.UserInfo(
                user.getPid(),
                user.getEmail(),
                user.getDisplayName()
            ),
            onboardingService.profileExists(user)
        );
    }

    private String resolveGoogleDisplayName(
        GoogleIdToken.Payload payload,
        String email
    ) {
        String name = (String) payload.get("name");
        if (name != null && !name.isBlank()) return name;
        String givenName = (String) payload.get("given_name");
        if (givenName != null && !givenName.isBlank()) return givenName;
        return email.split("@")[0];
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256").digest(
                token.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
