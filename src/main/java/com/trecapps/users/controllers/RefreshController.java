//package com.trecapps.users.controllers;
//
//import com.trecapps.auth.models.LoginToken;
//import com.trecapps.auth.models.TrecAuthentication;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/refresh_token")
//public class RefreshController extends CookieControllerBase {
//
//    public RefreshController(@Value("${trecauth.refresh.app:TREC_APPS_REFRESH}") String refreshCookie1,
//                             @Value("${trecauth.refresh.domain:#{NULL}}") String domain1,
//                             @Value("${trecauth.refresh.on_local:false}") boolean onLocal1) {
//        super(refreshCookie1, domain1, onLocal1);
//    }
//
//    @GetMapping
//    public ResponseEntity<LoginToken> checkRefresh(HttpServletResponse response){
//        SecurityContext context = SecurityContextHolder.getContext();
//        Authentication authentication = context == null ? null : context.getAuthentication();
//
//        if(authentication instanceof TrecAuthentication){
//            TrecAuthentication tAuth = (TrecAuthentication) authentication;
//
//            this.SetCookie(response, tAuth.getLoginToken().getRefresh_token());
//            return new ResponseEntity<>(tAuth.getLoginToken(), HttpStatus.OK);
//        }
//        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//    }
//}
