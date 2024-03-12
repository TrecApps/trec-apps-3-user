package com.trecapps.users.controllers;

import com.trecapps.auth.controllers.CookieBase;
import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TokenTime;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.services.JwtTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CookieControllerBase {

    Logger logger = LoggerFactory.getLogger(CookieControllerBase.class);

    CookieBase cookieBase;
    JwtTokenService jwtTokenService;

    CookieControllerBase(CookieBase cb, JwtTokenService jwtTokenService1)
    {
        cookieBase = cb;
        jwtTokenService = jwtTokenService1;
    }


    void applyCookie(TrecAuthentication trecAuthentication, LoginToken token, TokenTime time, HttpServletResponse response){
        Map<String, String> apps= trecAuthentication.getClaims();

        apps.put("app_" + cookieBase.getCookieAppName(), time.getSession());

        String refreshToken = jwtTokenService.generateRefreshToken(trecAuthentication.getAccount(), apps);
        token.setRefresh_token(refreshToken);

        this.SetCookie(response, refreshToken);

    }

    void SetCookie(HttpServletResponse response, String refreshToken){
        Cookie cook = new Cookie(cookieBase.getCookieName(), refreshToken);
        cook.setHttpOnly(true);
        cook.setPath("/");
        cook.setDomain(cookieBase.getDomain());
        cook.setSecure(true);

        cook.setMaxAge((int)TimeUnit.DAYS.toSeconds(7));

        response.addCookie(cook);
    }

    void RemoveCookie(HttpServletRequest request, HttpServletResponse response){
        for(Cookie cook : request.getCookies())
        {
            if(cook.getName().equals(cookieBase.getCookieName()))
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
