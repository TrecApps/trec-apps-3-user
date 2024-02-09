package com.trecapps.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "com.trecapps.users.*",                     // Scan this app
        "com.trecapps.base.InfoResource.models",    // usable models
        "com.trecapps.auth.*",                      // Authentication library
        "com.trecapps.pictures.*"})                   // picture management
@EntityScan({"com.trecapps.auth.models.primary.*",
                "com.trecapps.auth.models.secondary.*",
        "com.trecapps.pictures.models.*"})
public class Driver {
    public static void main(String[] args)
    {
        //ApplicationInsights.attach();
        SpringApplication.run(Driver.class, args);
    }
}