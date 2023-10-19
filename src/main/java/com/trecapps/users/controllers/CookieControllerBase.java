package com.trecapps.users.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

public class CookieControllerBase {

    Logger logger = LoggerFactory.getLogger(CookieControllerBase.class);

    String refreshCookie;
    String domain;
    boolean onLocal;

    public CookieControllerBase(
            @Value("${trecauth.refresh.app:TREC_APPS_REFRESH}") String refreshCookie1,
            @Value("${trecauth.refresh.domain:#{NULL}}") String domain1,
            @Value("${trecauth.refresh.on_local:false}") boolean onLocal1){
        refreshCookie = refreshCookie1;
        domain = domain1;
        onLocal = onLocal1;
    }

    void SetCookie(HttpServletResponse response, String refreshToken){
        Cookie cook = new Cookie(refreshCookie, refreshToken);
        cook.setHttpOnly(true);
        cook.setPath("/");
        if(domain != null) {
            logger.info("Setting Cookie domain to {}", domain);
            cook.setDomain(domain);
        }
        if(!onLocal)
            cook.setSecure(true);

        cook.setMaxAge((int)TimeUnit.DAYS.toSeconds(7));

        response.addCookie(cook);
    }

    void RemoveCookie(HttpServletRequest request, HttpServletResponse response){
        for(Cookie cook : request.getCookies())
        {
            if(cook.getName().equals(refreshCookie))
            {
                cook.setValue("");
                cook.setPath("/");
                cook.setMaxAge(0);
                response.addCookie(cook);
                return;
            }
        }
    }


}
