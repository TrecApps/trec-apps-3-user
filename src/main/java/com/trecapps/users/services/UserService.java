package com.trecapps.users.services;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    WebClient graphClient;

    TokenProvider tokenProvider;

    @Autowired
    public UserService(TokenProvider tokenProvider)
    {
        graphClient = WebClient.builder().baseUrl("https://graph.microsoft.com/v1.0/")
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.tokenProvider = tokenProvider;
    }

    private static Mono<ResponseEntity<String>> monotize(ResponseEntity<String> ent)
    {
        return Mono.just(ent);
    }

    public Mono<ResponseEntity<String>> createUser(UserPost post)
    {
        return graphClient.post().uri("/users")
                .header("Authorization", tokenProvider.getAuthToken())
                .bodyValue(post)
                .exchangeToMono((ClientResponse response) ->
                {
                    switch(response.statusCode())
                    {
                        case CREATED:
                        case OK:
                        case NO_CONTENT:
                        case ACCEPTED:
                            return monotize(new ResponseEntity<String>("Success",HttpStatus.OK));
                        case UNAUTHORIZED:
                        case FORBIDDEN:
                            return monotize(new ResponseEntity<String>("Error Connecting to Azure Active Directory", HttpStatus.INTERNAL_SERVER_ERROR));
                        case NOT_FOUND:
                        case INTERNAL_SERVER_ERROR:
                        case BAD_GATEWAY:
                            return monotize(new ResponseEntity<String>("Error Connecting to Azure Active Directory", HttpStatus.BAD_GATEWAY));
                        case BAD_REQUEST:
                            return monotize(new ResponseEntity<String>("Error In your submission!", HttpStatus.BAD_REQUEST));
                        default:
                            return monotize(new ResponseEntity<String>("Unknown Error Occurred", HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }

    public Mono<ResponseEntity<String>> updatePassword(PasswordChange change)
    {
        return graphClient.post().uri("/me/changePassword")
                .header("Authorization", tokenProvider.getAuthToken())
                .bodyValue(change)
                .exchangeToMono((ClientResponse response) ->
                {
                    switch(response.statusCode())
                    {
                        case CREATED:
                        case OK:
                        case NO_CONTENT:
                        case ACCEPTED:
                            return monotize(new ResponseEntity<String>("Success",HttpStatus.OK));
                        case UNAUTHORIZED:
                        case FORBIDDEN:
                            return monotize(new ResponseEntity<String>("Error Connecting to Azure Active Directory", HttpStatus.INTERNAL_SERVER_ERROR));
                        case NOT_FOUND:
                        case INTERNAL_SERVER_ERROR:
                        case BAD_GATEWAY:
                            return monotize(new ResponseEntity<String>("Error Connecting to Azure Active Directory", HttpStatus.BAD_GATEWAY));
                        case BAD_REQUEST:
                            return monotize(new ResponseEntity<String>("Error In your submission!", HttpStatus.BAD_REQUEST));
                        default:
                            return monotize(new ResponseEntity<String>("Unknown Error Occurred", HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
    }
}
