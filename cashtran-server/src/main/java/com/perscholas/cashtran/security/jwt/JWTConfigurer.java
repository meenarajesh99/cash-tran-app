package com.perscholas.cashtran.security.jwt;

import jakarta.servlet.Filter;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JWTConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private TokenProvider tokenProvider;

    public JWTConfigurer(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void configure(HttpSecurity http) {
        JwtAuthenticationFilter customFilter = new JwtAuthenticationFilter(tokenProvider);
        http.addFilterBefore((Filter) customFilter, (Class<? extends Filter>) UsernamePasswordAuthenticationFilter.class);
    }
}

