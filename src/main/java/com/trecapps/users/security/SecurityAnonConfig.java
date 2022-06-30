//package com.trecapps.users.security;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
//@EnableWebSecurity
//@Configuration
//@Order(2)
//public class SecurityAnonConfig  extends WebSecurityConfigurerAdapter {
//    @Override
//    protected void configure(HttpSecurity security) throws Exception {
//        security.csrf().disable()
//                .authorizeRequests()
//                .anyRequest()
//                .permitAll();
//    }
//}
