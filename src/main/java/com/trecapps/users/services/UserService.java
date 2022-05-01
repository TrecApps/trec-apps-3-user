package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trecapps.users.models.PasswordChange;
import com.trecapps.users.models.TcUser;
import com.trecapps.users.models.UserPost;
import com.trecapps.users.security.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

@Service
public class UserService {


    Logger logger = LoggerFactory.getLogger(UserService.class);

    RestTemplate graphClient;

    TokenProvider tokenProvider;
    String baseUrl;
    MultiValueMap<String, String> headers;

    StorageService storageService;

    ObjectMapper mapper;

    @Autowired
    public UserService(TokenProvider tokenProvider, StorageService storageService1)
    {
        mapper = new ObjectMapper();

        baseUrl = "https://graph.microsoft.com/v1.0/";
        headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type","application/x-www-form-urlencoded");

        graphClient = new RestTemplate();
        graphClient.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        this.tokenProvider = tokenProvider;
        storageService = storageService1;
    }

    private static ResponseEntity<String> monotize(ResponseEntity<String> ent)
    {
        return (ent);
    }

    public ResponseEntity<String> createUser(UserPost post, boolean firstCall)
    {
        //graphClient.exchange(baseUrl + "users", HttpMethod.POST, new HttpEntity<>(post, headers), String.class);
        MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>();
        authHeaders.add("Authorization", "Bearer " + tokenProvider.getAuthToken());
        authHeaders.add("Content-Type", "application/json");

        OffsetDateTime birthday = post.getBirthday();
        String results;
        try
        {
            ResponseEntity<String> entity = graphClient.exchange(baseUrl + "users", HttpMethod.POST, new HttpEntity<>(post.GetGraphObject(), authHeaders), String.class);
            switch(entity.getStatusCode()) {
                case CREATED:
                case OK:
                case NO_CONTENT:
                case ACCEPTED:
                    results = mapper.readTree(entity.getBody()).findValue("id").asText();
                    //post.setBirthday(birthday);
                    return monotize(new ResponseEntity<>("Succeeded", HttpStatus.OK));
                case UNAUTHORIZED:
                    if(firstCall)
                    {
                        tokenProvider.refreshToken();
                        return createUser(post, false);
                    }
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

    public boolean isValidPhone(String phone)
    {
        // To-Do: Validate Phone
        return true;
    }

    void saveUser(UserPost user, String id)
    {
        TcUser user1 = new TcUser(user,id);
        storageService.saveUser(user1);
    }

    public ResponseEntity<TcUser> getTcUser(String id)
    {
        try {
            return new ResponseEntity<TcUser>(storageService.retrieveUser(id), HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> updateTcUser(TcUser user)
    {
        try {
            TcUser storedUser = storageService.retrieveUser(user.getId());

            storedUser.setBirthdaySetting(user.getBirthdaySetting());
            storedUser.setAddress(user.getAddress());
            storedUser.setProfilePic(user.getProfilePic());

            if(!storedUser.getEmail().equals(user.getEmail()))
            {
                storedUser.setEmail(user.getEmail());
                storedUser.setEmailVerified(false);
            }

            if(!storedUser.getMobilePhone().equals(user.getMobilePhone()) && isValidPhone(user.getMobilePhone()))
            {
                storedUser.setMobilePhone(user.getMobilePhone());
                storedUser.setPhoneVerified(false);
            }


            storageService.saveUser(storedUser);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<String>("Failed to Register User info with Azure Storage", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Updated!", HttpStatus.OK);

    }

//    private ResponseEntity<String> patchBirthday(UserPost post)
//    {
//        MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>();
//        authHeaders.add("Authorization", "Bearer " + tokenProvider.getAuthToken());
//        authHeaders.add("Content-Type", "application/json");
//
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//
//        body.add("birthday", post.getBirthday().toString());
//
//
//        ResponseEntity entity = graphClient.exchange(baseUrl + "users/" + post.getUserPrincipalName(), HttpMethod.PATCH, new HttpEntity<>(body, authHeaders), String.class);
//
//        return new ResponseEntity<>(entity.getBody().toString(), entity.getStatusCode());
//    }

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
