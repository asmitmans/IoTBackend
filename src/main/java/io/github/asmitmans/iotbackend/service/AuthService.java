package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.auth.LoginRequest;
import io.github.asmitmans.iotbackend.dto.auth.LoginResponse;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()
                )
        );

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(principal);

        return new LoginResponse(token, principal.getUsername());
    }
}
