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
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
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


    void applyCookie(TrecAuthentication trecAuthentication, LoginToken token, ServerHttpResponse response){

        String refreshToken = jwtTokenService.generateRefreshToken(trecAuthentication.getAccount());
        token.setRefresh_token(refreshToken);

        this.SetCookie(response, refreshToken);

    }

    void SetCookie(ServerHttpResponse response, String refreshToken){

        ResponseCookie.ResponseCookieBuilder cookBuilder = ResponseCookie.from(this.cookieName, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge((int)TimeUnit.DAYS.toSeconds(7L));
        if(this.domain != null)cookBuilder = cookBuilder.domain(this.domain);

        response.addCookie(cookBuilder.build());
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
