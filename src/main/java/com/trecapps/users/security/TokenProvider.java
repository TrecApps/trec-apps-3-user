package com.trecapps.users.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    @Value("${application.graph.token}")
    String token;

    public String getAuthToken()
    {
        return token;
    }
}
