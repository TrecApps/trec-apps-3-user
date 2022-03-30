package com.trecapps.users.services;

import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.security.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class UserService {


    Logger logger = LoggerFactory.getLogger(UserService.class);

    RestTemplate graphClient;

    TokenProvider tokenProvider;
    String baseUrl;
    MultiValueMap<String, String> headers;

    @Autowired
    public UserService(TokenProvider tokenProvider)
    {
        baseUrl = "https://graph.microsoft.com/v1.0/";
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type","application/x-www-form-urlencoded");

        graphClient = new RestTemplate();

        this.tokenProvider = tokenProvider;
    }

    private static ResponseEntity<String> monotize(ResponseEntity<String> ent)
    {
        return (ent);
    }

    public ResponseEntity<String> createUser(UserPost post)
    {
        //graphClient.exchange(baseUrl + "users", HttpMethod.POST, new HttpEntity<>(post, headers), String.class);
        MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>();
        authHeaders.add("Authorization", tokenProvider.getAuthToken());
        authHeaders.add("Content-Type", "application/json");

        try
        {
            ResponseEntity entity = graphClient.exchange(baseUrl + "users", HttpMethod.POST, new HttpEntity<>(post, authHeaders), String.class);
            switch(entity.getStatusCode()) {
                case CREATED:
                case OK:
                case NO_CONTENT:
                case ACCEPTED:
                    return monotize(new ResponseEntity<String>("Success", HttpStatus.OK));
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
                    logger.info("Entity contents: {}", entity.getBody());
                    return monotize(new ResponseEntity<String>("Unknown Error Occurred", HttpStatus.INTERNAL_SERVER_ERROR));
            }
        }
        catch( Exception e)
        {
            logger.error("Exception occurred", e);
            logger.info("Exception occurred: {}", e.getMessage());
            e.printStackTrace();
            return monotize(new ResponseEntity<String>("Unknown Exception Occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }


    }

    public ResponseEntity<String> updatePassword(PasswordChange change, String auth)
    {
        MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>(headers);
        authHeaders.add("Authorization", auth);

        switch(graphClient.exchange(baseUrl + "me/changePassword", HttpMethod.POST, new HttpEntity<>(change, authHeaders),String.class).getStatusCode()) {
            case CREATED:
            case OK:
            case NO_CONTENT:
            case ACCEPTED:
                return monotize(new ResponseEntity<String>("Success", HttpStatus.OK));
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
    }
}
