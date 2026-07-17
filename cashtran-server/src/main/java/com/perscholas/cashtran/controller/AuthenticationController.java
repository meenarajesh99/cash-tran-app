package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.repository.UserRepository;
import com.perscholas.cashtran.dto.LoginDTO;
import com.perscholas.cashtran.dto.RegisterUserDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.dto.LoginResponseDTO;
import com.perscholas.cashtran.dto.UserResponseDTO;

import com.perscholas.cashtran.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthenticationController(
            AuthService authService,
            UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody LoginDTO loginDTO) {
        String token = authService.login(loginDTO);
        User user = userRepository.findByUsername(loginDTO.getUsername());
        UserResponseDTO userResponse = new UserResponseDTO(user);
        return ResponseEntity.ok(new LoginResponseDTO(token, userResponse));
    }

    @Operation(
            summary = "Register a new user"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public UserResponseDTO register(
            @Valid @RequestBody RegisterUserDTO newUser) {

        boolean created =
                userRepository.create(
                        newUser.getUsername(),
                        newUser.getPassword()
                );

        if (!created) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        User user =
                userRepository.findByUsername(
                        newUser.getUsername()
                );

        return new UserResponseDTO(user);
    }
}