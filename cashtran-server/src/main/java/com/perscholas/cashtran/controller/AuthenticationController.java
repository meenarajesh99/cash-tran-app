package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.dao.UserDao;
import com.perscholas.cashtran.model.LoginDTO;
import com.perscholas.cashtran.model.RegisterUserDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.model.LoginResponse;
import com.perscholas.cashtran.model.UserResponse;

import com.perscholas.cashtran.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserDao userDao;
    private final AuthService authService;

    public AuthenticationController(
            AuthService authService,
            UserDao userDao) {
        this.authService = authService;
        this.userDao = userDao;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginDTO loginDTO) {
        String token = authService.login(loginDTO);
        User user = userDao.findByUsername(loginDTO.getUsername());
        UserResponse userResponse = new UserResponse(user);
        return ResponseEntity.ok(new LoginResponse(token, userResponse));
    }

    @Operation(
            summary = "Register a new user"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterUserDTO newUser) {

        boolean created =
                userDao.create(
                        newUser.getUsername(),
                        newUser.getPassword()
                );

        if (!created) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        User user =
                userDao.findByUsername(
                        newUser.getUsername()
                );

        return new UserResponse(user);
    }
}