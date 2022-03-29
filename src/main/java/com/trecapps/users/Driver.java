package com.trecapps.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.trecapps.users.*", "com.trecapps.base.InfoResource.models"})
public class Driver {
    public static void main(String[] args)
    {
        SpringApplication.run(Driver.class, args);
    }
}