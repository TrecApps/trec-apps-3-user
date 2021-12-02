package com.trecapps.users.services;

import com.trecapps.users.models.Login;
import com.trecapps.users.models.LoginToken;
import com.trecapps.users.security.TokenProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.CharBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    WebClient authClient;

    TokenProvider tokenProvider;

    String clientId;

    @Autowired
    public AuthService(TokenProvider tokenProvider1, @Value("${tenent.id}") String tenent1)
    {
        tokenProvider = tokenProvider1;

        authClient = WebClient.builder()
                        .baseUrl("https://login.microsoftonline.com/" + tenent1 + "/oauth2/v2.0/")
                        .defaultHeader("Content-Type:","application/x-www-form-urlencoded")
                        .build();
    }


    public Mono<LoginToken> getTokenDirectly(Login login)
    {
        MultiValueMap<String, CharBuffer> params = new LinkedMultiValueMap<>();
        //params.add("");
        authClient.post().uri("token")
                .body(BodyInserters.fromMultipartData(params))
                .exchangeToMono((ClientResponse response) -> {
                   if(response.statusCode().is2xxSuccessful())
                       return response.bodyToMono(LoginToken.class);
                   return Mono.just(new LoginToken());
                });
    }

}
