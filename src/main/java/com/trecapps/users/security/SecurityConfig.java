package com.trecapps.users.security;

import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.auth.services.TrecSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@Order(1)
public class SecurityConfig {

    @Autowired
    SecurityConfig(TrecAccountService trecAccountService1, TrecSecurityContext trecSecurityContext1)
    {
        //aadAuthProps.setRedirectUriTemplate("http://localhost:4200/api");
        trecAccountService = trecAccountService1;
        trecSecurityContext = trecSecurityContext1;
    }
    TrecAccountService trecAccountService;
    TrecSecurityContext trecSecurityContext;

    String[] restrictedEndpoints = {
            "/Users/passwordUpdate",
            "/Users/Current",
            "/Users/UserUpdate",
            "/Sessions/**",
            "/Email/**",
            "/Auth/permissions"

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
    public SecurityFilterChain configure(HttpSecurity security) throws Exception
    {
        security.csrf().disable().authorizeHttpRequests()

                .requestMatchers(restrictedEndpoints)
                .authenticated()
                .and()
                .authorizeHttpRequests()
                .requestMatchers(verifiedEndpoints)
                .hasAuthority("TREC_VERIFIED")
                .and()
                .authorizeHttpRequests()
                .anyRequest()
                .permitAll()
                .and()
                .userDetailsService(trecAccountService)
                .securityContext().securityContextRepository(trecSecurityContext).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ;
        return security.build();
    }





}
