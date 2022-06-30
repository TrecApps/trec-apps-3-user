package com.trecapps.users.security;

import com.trecapps.auth.services.TrecAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Configuration
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    SecurityConfig(TrecAccountService trecAccountService1)
    {
        //aadAuthProps.setRedirectUriTemplate("http://localhost:4200/api");
        trecAccountService = trecAccountService1;

    }
    TrecAccountService trecAccountService;

    String[] restrictedEndpoints = {
            "/Users/passwordUpdate",
            "/Users/Current",
            "/Users/UserUpdate",
            "/Sessions/**"
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
                ;
    }





}
