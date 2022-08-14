package com.trecapps.users.security;

import com.trecapps.auth.services.TrecAccountService;
import com.trecapps.auth.services.TrecSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@EnableWebSecurity
@Configuration
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

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
            "/Brands/list",
            "/Brands/New",
            "/Brands/NewOwner/**",
            "/Brands/login"
    };

    @Override
    protected void configure(HttpSecurity security) throws Exception
    {
        security.csrf().disable()
                .authorizeRequests()
                .antMatchers(restrictedEndpoints)
                .authenticated()
                .and()
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .userDetailsService(trecAccountService)
                .securityContext().securityContextRepository(trecSecurityContext).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ;
    }





}
