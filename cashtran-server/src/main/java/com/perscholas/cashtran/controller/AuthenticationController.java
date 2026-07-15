package com.perscholas.cashtran.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.perscholas.cashtran.dao.UserDao;
import com.perscholas.cashtran.model.LoginDTO;
import com.perscholas.cashtran.model.RegisterUserDTO;
import com.perscholas.cashtran.model.User;
import com.perscholas.cashtran.security.jwt.TokenProvider;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserDao userDao;

    public AuthenticationController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder, UserDao userDao) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDao = userDao;
    }

    @Operation(summary = "Authenticates the user with the given username and password")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody @Parameter(description = "username and password") LoginDTO loginDto) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.createToken(authentication, false);

        User user = userDao.findByUsername(loginDto.getUsername());

        return new LoginResponse(jwt, user);
    }

    @Operation(summary = "Registers a new user")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public User register(@Valid @RequestBody @Parameter(description = "Registration info") RegisterUserDTO newUser) {
        if (!userDao.create(newUser.getUsername(), newUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User registration failed.");
        }
        return userDao.findByUsername(newUser.getUsername());
    }

    static class LoginResponse {
        private String token;
        private User user;
        LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }
}