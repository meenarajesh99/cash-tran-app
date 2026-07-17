package com.perscholas.cashtran.service;

import com.perscholas.cashtran.security.jwt.TokenProvider;
import com.perscholas.cashtran.dto.LoginDTO;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    public AuthService(
            AuthenticationManager authenticationManager,
            TokenProvider tokenProvider) {

        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public String login(LoginDTO loginDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginDTO.getUsername(),
                                loginDTO.getPassword()
                        )
                );


        return tokenProvider.createToken(authentication);
    }
}