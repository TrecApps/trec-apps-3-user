package com.trecapps.users;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.trecapps.users.*",                     // Scan this app
        "com.trecapps.base.InfoResource.models",    // usable models
        "com.trecapps.auth.common.*",               // Authentication library
        "com.trecapps.auth.webflux.*"
}
)
@EnableWebFlux
public class Driver {
    public static void main(String[] args)
    {
        ApplicationInsights.attach();
        SpringApplication.run(Driver.class, args);
    }
}