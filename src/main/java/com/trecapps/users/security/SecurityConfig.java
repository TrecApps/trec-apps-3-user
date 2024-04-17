package com.trecapps.users.security;

import com.trecapps.auth.services.login.TrecAccountService;
import com.trecapps.auth.services.web.TrecSecurityContextServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebSecurity
@Configuration
@Order(1)
public class SecurityConfig {

    @Autowired
    SecurityConfig(TrecAccountService trecAccountService1, TrecSecurityContextServlet trecSecurityContext1)
    {
        //aadAuthProps.setRedirectUriTemplate("http://localhost:4200/api");
        trecAccountService = trecAccountService1;
        trecSecurityContext = trecSecurityContext1;
    }
    TrecAccountService trecAccountService;
    TrecSecurityContextServlet trecSecurityContext;

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

        security = security.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((req) ->
                    req
                            .requestMatchers(restrictedEndpoints).authenticated()
                            .requestMatchers(verifiedEndpoints).hasAuthority("TREC_VERIFIED")
                            .anyRequest().permitAll()
                )
                .userDetailsService(trecAccountService)
                .securityContext((cust)->
                    cust.securityContextRepository(trecSecurityContext)
                            .requireExplicitSave(true)
                )
                .sessionManagement((cust)-> cust.sessionCreationPolicy(SessionCreationPolicy.NEVER))

                ;
        return security.build();
    }



//    SecurityWebFilterChain getChain(ServerHttpSecurity http){
//        //http.authenticationManager()
//        http.securityContextRepository()
//    }



}
