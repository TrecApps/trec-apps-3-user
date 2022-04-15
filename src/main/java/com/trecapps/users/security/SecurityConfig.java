package com.trecapps.users.security;

import com.azure.spring.aad.webapp.AADOAuth2UserService;
import com.azure.spring.autoconfigure.aad.AADAuthenticationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    SecurityConfig(AADAuthenticationProperties aadAuthProps)
    {
        aadoAuth2UserService = new AADOAuth2UserService(aadAuthProps);
    }
    AADOAuth2UserService aadoAuth2UserService;

    String[] restrictedEndpoints = {
            "/Users/passwordUpdate"
    };

    @Override
    protected void configure(HttpSecurity security) throws Exception
    {
        security
                .authorizeRequests()
                .antMatchers(restrictedEndpoints)
                .authenticated()
                .and()
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .oauth2Login()
                .userInfoEndpoint()
                .oidcUserService(getTrecDirectoryService());
    }



    @Bean
    protected TrecActiveDirectoryService getTrecDirectoryService()
    {
        return new TrecActiveDirectoryService(aadoAuth2UserService);
    }

}
