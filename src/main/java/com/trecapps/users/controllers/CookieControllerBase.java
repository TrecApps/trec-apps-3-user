package com.trecapps.users.controllers;

import com.trecapps.auth.webflux.controllers.CookieBase;
import com.trecapps.auth.common.models.LoginToken;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import reactor.netty.http.server.HttpServerResponse;

import java.util.concurrent.TimeUnit;

public class CookieControllerBase {

    Logger logger = LoggerFactory.getLogger(CookieControllerBase.class);

    CookieBase cookieBase;
    JwtTokenServiceAsync jwtTokenService;

    @Value("${trecauth.refresh.domain}")
    String domain;

    @Value("${trecauth.refresh.cookie-name:TREC_APPS_REFRESH}")
    String cookieName;

    CookieControllerBase(CookieBase cb, JwtTokenServiceAsync jwtTokenService1, String domain, String cookieName)
    {
        this.cookieName = cookieName;
        this.domain = domain;
        cookieBase = cb;
        jwtTokenService = jwtTokenService1;
    }


    void applyCookie(TrecAuthentication trecAuthentication, LoginToken token, HttpServerResponse response){

        String refreshToken = jwtTokenService.generateRefreshToken(trecAuthentication.getAccount());
        token.setRefresh_token(refreshToken);

        this.SetCookie(response, refreshToken);

    }

    void SetCookie(HttpServerResponse response, String refreshToken){

        io.netty.handler.codec.http.cookie.Cookie cook = new DefaultCookie(this.cookieName, refreshToken);
        cook.setHttpOnly(true);
        cook.setPath("/");
        if (this.domain != null) {
            this.logger.info("Setting Cookie domain to {}", this.domain);
            cook.setDomain(this.domain);
        }

        cook.setSecure(true);
        cook.setMaxAge((long)((int)TimeUnit.DAYS.toSeconds(7L)));
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
