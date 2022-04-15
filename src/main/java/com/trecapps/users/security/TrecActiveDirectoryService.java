package com.trecapps.users.security;

import com.azure.spring.aad.webapp.AADOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

//@Component
public class TrecActiveDirectoryService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    AADOAuth2UserService aadoAuth2UserService;


    //@Autowired
    public TrecActiveDirectoryService(AADOAuth2UserService aadoAuth2UserService1)
    {
        aadoAuth2UserService = aadoAuth2UserService1;
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser user = aadoAuth2UserService.loadUser(userRequest);

        return user;
    }
}
