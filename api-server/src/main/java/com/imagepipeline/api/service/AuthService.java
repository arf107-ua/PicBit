package com.imagepipeline.api.service;

import com.imagepipeline.api.model.dto.AuthRequest;
import com.imagepipeline.api.model.dto.AuthResponse;
import com.imagepipeline.api.model.dto.RegisterRequest;
import com.imagepipeline.api.model.pg.User;
import com.imagepipeline.api.repository.pg.UserRepository;
import com.imagepipeline.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El username ya está en uso");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .build();

        User saved = userRepository.save(user);
        log.info("Usuario registrado: {}", saved.getUsername());

        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername());
        // Refresh token simplified — in production use a separate signed token stored in DB
        String refreshToken = jwtUtil.generateToken(saved.getId(), saved.getUsername());

        return new AuthResponse(token, refreshToken, saved.getId().toString(), saved.getUsername());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        log.info("Login correcto: {}", user.getUsername());

        String token        = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(token, refreshToken, user.getId().toString(), user.getUsername());
    }
}
