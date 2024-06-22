package com.trecapps.users.security;

import com.trecapps.auth.webflux.services.TrecAccountServiceAsync;
import com.trecapps.auth.webflux.services.TrecAuthManagerReactive;
import com.trecapps.auth.webflux.services.TrecSecurityContextReactive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Configuration
@Order(1)
public class SecurityConfig {

    @Autowired
    SecurityConfig(TrecAccountServiceAsync trecAccountService1,
                   TrecSecurityContextReactive trecSecurityContext1,
                   TrecAuthManagerReactive trecAuthManagerReactive)
    {
        //aadAuthProps.setRedirectUriTemplate("http://localhost:4200/api");
        trecAccountService = trecAccountService1;
        trecSecurityContext = trecSecurityContext1;
    }
    TrecAccountServiceAsync trecAccountService;
    TrecSecurityContextReactive trecSecurityContext;
    TrecAuthManagerReactive trecAuthManagerReactive;

    String[] restrictedEndpoints = {
            "/Users/passwordUpdate",
            "/Users/Current",
            "/Users/UserUpdate",
            "/Sessions/**",
            "/Email/**",
            "/Auth/permissions",
            "/refresh_token"

    };

    String[] verifiedEndpoints = {
            "/Brands/list",
            "/Brands/New",
            "/Brands/NewOwner/**",
            "/Brands/login",
            "/profile/set/**",
            "/brandProfile/set/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(restrictedEndpoints).authenticated()
                        .pathMatchers(verifiedEndpoints).hasAuthority("TREC_VERIFIED")
                        .anyExchange().permitAll())
                .authenticationManager(trecAuthManagerReactive)
                .securityContextRepository(trecSecurityContext)

                .build();
    }

}
