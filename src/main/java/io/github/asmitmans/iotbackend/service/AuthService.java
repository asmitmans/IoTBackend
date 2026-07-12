package io.github.asmitmans.iotbackend.service;

import io.github.asmitmans.iotbackend.dto.auth.LoginRequest;
import io.github.asmitmans.iotbackend.dto.auth.LoginResponse;
import io.github.asmitmans.iotbackend.security.JwtService;
import io.github.asmitmans.iotbackend.security.LoginAttemptService;
import io.github.asmitmans.iotbackend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public LoginResponse login(LoginRequest request) {
        loginAttemptService.checkAllowed(request.username());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(request.username());
            throw e;
        }

        loginAttemptService.recordSuccess(request.username());

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(principal);

        return new LoginResponse(token, principal.getUsername());
    }
}