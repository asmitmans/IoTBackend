package io.github.asmitmans.iotbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    public String generateToken(UserPrincipal principal) {
        long now = System.currentTimeMillis();
        String authorities = principal.getAuthorities().stream()
                                      .map(GrantedAuthority::getAuthority)
                                      .collect(Collectors.joining(","));

        return Jwts.builder()
                   .subject(principal.getUsername())
                   .claim("companyId", principal.getCompanyId())
                   .claim("authorities", authorities)
                   .issuedAt(new Date(now))
                   .expiration(new Date(now + props.expirationMs()))
                   .signWith(key())
                   .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractCompanyId(String token) {
        return parseClaims(token).get("companyId", Long.class);
    }

    public String extractAuthorities(String token) {
        return parseClaims(token).get("authorities", String.class);
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(key())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }
}