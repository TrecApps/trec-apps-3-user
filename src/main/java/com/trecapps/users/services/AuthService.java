package com.trecapps.users.services;

import com.trecapps.users.models.Login;
import com.trecapps.users.models.LoginToken;
import com.trecapps.users.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    RestTemplate authClient;

    TokenProvider tokenProvider;

    String clientId, clientSecret;

    String baseUrl;
    MultiValueMap<String, String> headers;

    @Autowired
    public AuthService(TokenProvider tokenProvider1,
                       @Value("${tenent.id}") String tenant1,
                       @Value("${client.id}") String clientId1,
                       @Value("${client.secret}") String clientSecret1)
    {
        tokenProvider = tokenProvider1;

        authClient = new RestTemplate();
        baseUrl = "https://login.microsoftonline.com/" + tenant1 + "/oauth2/v2.0/";
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type:","application/x-www-form-urlencoded");

        clientId = clientId1;
        clientSecret = clientSecret1;
    }


    public ResponseEntity<LoginToken> getTokenDirectly(Login login)
    {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("grant_type", "password");
        params.add("username", login.getUsername());
        params.add("password", login.getPassword());
        params.add("scope", "user.read%20openid%20profile%20offline_access");
        params.add("client_secret", clientSecret);

        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);
    }

    public ResponseEntity<LoginToken> getTokenFromMSFlow(String redirectUri, String code)
    {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("scope", "user.read%20openid%20profile%20offline_access");
        params.add("client_secret", clientSecret);

        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);

    }

    public ResponseEntity<LoginToken> getTokenFromRefresh(String redirectUri, String refreshToken)
    {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("grant_type", "refresh_token");
        params.add("redirect_uri", redirectUri);
        params.add("refresh_token", refreshToken);
        params.add("scope", "user.read%20openid%20profile%20offline_access");
        params.add("client_secret", clientSecret);
        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);


    }

}
