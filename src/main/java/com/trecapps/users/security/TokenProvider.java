package com.trecapps.users.security;

import com.trecapps.users.models.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.TreeMap;

@Component
public class TokenProvider {

    String token;

    @Value("${tenent.id}")
    String tenantId;

    @Value("${client.id}")
    String clientId;

    @Value("${client.secret}")
    String clientSecret;

    long nextToken;

    RestTemplate graphClient = new RestTemplate();

    private static long THREE_SECONDS = 3000;

    public synchronized void refreshToken()
    {
        if(token != null && nextToken > System.currentTimeMillis())
            return;
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("scope", "https%3A%2F%2Fgraph.microsoft.com%2F.default");
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> params = new TreeMap<>();
        params.put("tenant", tenantId);
        ResponseEntity<TokenResponse> response = graphClient.exchange("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", HttpMethod.POST, new HttpEntity(body, headers), TokenResponse.class, params);

        TokenResponse tokenResponse = response.getBody();

        token = tokenResponse.getAccessToken();
        nextToken = System.currentTimeMillis() + THREE_SECONDS;

    }

    public String getAuthToken()
    {
        if(token == null)
            refreshToken();
        return token;
    }
}
