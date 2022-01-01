package com.trecapps.users.services;

import com.trecapps.users.models.Login;
import com.trecapps.users.models.LoginToken;
import com.trecapps.users.security.TokenProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        MultiValueMap<String, CharBuffer> params = new LinkedMultiValueMap<>();
        params.add("client_id", CharBuffer.wrap(clientId));
        params.add("grant_type", CharBuffer.wrap("password"));
        params.add("username", login.getUsername());
        params.add("password", login.getPassword());
        params.add("scope", CharBuffer.wrap("user.read%20openid%20profile%20offline_access"));
        params.add("client_secret", CharBuffer.wrap(clientSecret));

        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);
    }

    public ResponseEntity<LoginToken> getTokenFromMSFlow(String redirectUri, String code)
    {
        MultiValueMap<String, CharBuffer> params = new LinkedMultiValueMap<>();
        params.add("client_id", CharBuffer.wrap(clientId));
        params.add("grant_type", CharBuffer.wrap("authorization_code"));
        params.add("redirect_uri", CharBuffer.wrap(redirectUri));
        params.add("code", CharBuffer.wrap(code));
        params.add("scope", CharBuffer.wrap("user.read%20openid%20profile%20offline_access"));
        params.add("client_secret", CharBuffer.wrap(clientSecret));

        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);

    }

    public ResponseEntity<LoginToken> getTokenFromRefresh(String redirectUri, String refreshToken)
    {
        MultiValueMap<String, CharBuffer> params = new LinkedMultiValueMap<>();
        params.add("client_id", CharBuffer.wrap(clientId));
        params.add("grant_type", CharBuffer.wrap("refresh_token"));
        params.add("redirect_uri", CharBuffer.wrap(redirectUri));
        params.add("refresh_token", CharBuffer.wrap(refreshToken));
        params.add("scope", CharBuffer.wrap("user.read%20openid%20profile%20offline_access"));
        params.add("client_secret", CharBuffer.wrap(clientSecret));
        return authClient.exchange(baseUrl + "token", HttpMethod.POST, new HttpEntity<>(params, headers), LoginToken.class);


    }

}
