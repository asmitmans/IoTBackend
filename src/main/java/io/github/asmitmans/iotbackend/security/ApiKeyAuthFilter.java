package io.github.asmitmans.iotbackend.security;

import io.github.asmitmans.iotbackend.repository.CompanyRepository;
import io.github.asmitmans.iotbackend.repository.DeviceRepository;
import io.github.asmitmans.iotbackend.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;
    private final ApiKeyService apiKeyService;
    private final CacheManager cacheManager;

    public ApiKeyAuthFilter(DeviceRepository deviceRepository,
                            CompanyRepository companyRepository,
                            ApiKeyService apiKeyService,
                            CacheManager cacheManager) {
        this.deviceRepository = deviceRepository;
        this.companyRepository = companyRepository;
        this.apiKeyService = apiKeyService;
        this.cacheManager = cacheManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("ApiKey ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = header.substring(7);

        if (apiKey.startsWith("iotdev_")) {
            authenticateDevice(apiKey);
        } else if (apiKey.startsWith("iotcmp_")) {
            authenticateCompany(apiKey);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateDevice(String apiKey) {
        Cache cache = cacheManager.getCache("deviceAuth");
        Cache.ValueWrapper cached = cache.get(apiKey);

        if (cached != null) {
            setAuthentication(cached.get(), "ROLE_DEVICE");
            return;
        }

        String prefix = apiKeyService.extractPrefix(apiKey);
        deviceRepository.findByApiKeyPrefix(prefix).stream()
                        .filter(d -> d.getStatus().equals("ACTIVE"))
                        .filter(d -> apiKeyService.verify(apiKey, d.getApiKeyHash()))
                        .findFirst()
                        .ifPresent(d -> {
                            cache.put(apiKey, d);
                            setAuthentication(d, "ROLE_DEVICE");
                        });
    }

    private void authenticateCompany(String apiKey) {
        Cache cache = cacheManager.getCache("companyAuth");
        Cache.ValueWrapper cached = cache.get(apiKey);

        if (cached != null) {
            setAuthentication(cached.get(), "ROLE_COMPANY");
            return;
        }

        String prefix = apiKeyService.extractPrefix(apiKey);
        companyRepository.findByApiKeyPrefix(prefix).stream()
                         .filter(c -> apiKeyService.verify(apiKey, c.getApiKeyHash()))
                         .findFirst()
                         .ifPresent(c -> {
                             cache.put(apiKey, c);
                             setAuthentication(c, "ROLE_COMPANY");
                         });
    }

    private void setAuthentication(Object principal, String role) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}