package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.LoginToken;
import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.common.models.secondary.BrandEntry;
import com.trecapps.auth.webflux.services.BrandServiceAsync;
import com.trecapps.users.models.AuthenticationBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/Brands")
public class BrandController {

    @Autowired
    BrandServiceAsync brandService;

    @Value("${trecauth.app}") String defaultApp;


    @GetMapping("/{id}")
    Mono<ResponseEntity<TcBrands>> getBrand(@PathVariable("id") String uuid)
    {
        TcBrands brand = null;
        SecurityContext context = SecurityContextHolder.getContext();
        TrecAuthentication trecAuth = (context.getAuthentication() instanceof TrecAuthentication) ? (TrecAuthentication) context.getAuthentication() : null;

        return brandService.getBrandById(uuid, trecAuth == null ? null : trecAuth.getAccount())
                .map((Optional<TcBrands> opt) -> {
                    return opt.map(ResponseEntity::ok).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
                });
    }

    @GetMapping("/list")
    Mono<ResponseEntity<List<BrandEntry>>> getBrands(Authentication authentication)
    {
        return Mono.just((TrecAuthentication) authentication)
                .flatMap((TrecAuthentication trecAuth) -> brandService.getBrandList(trecAuth.getAccount()))
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/New", consumes = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<String>> submitNewBrand(RequestEntity<String> name, Authentication authentication)
    {
        return Mono.just(new AuthenticationBody<>((TrecAuthentication) authentication, name.getBody()))
                .flatMap((AuthenticationBody<String> ab) -> brandService.createNewBrand(ab.getAuthentication().getAccount(), ab.getData()))
                .map((String result) -> {
                    String[] parts = result.split(":");

                    return new ResponseEntity<>(parts[1], HttpStatus.valueOf(Integer.parseInt(parts[0])));
                });
    }

    @PutMapping(value = "/NewOwner/{id}", consumes = MediaType.TEXT_PLAIN_VALUE)
    Mono<ResponseEntity<Void>> assignNewOwner(RequestEntity<String> userId, @PathVariable("id") String uuid, Authentication authentication)
    {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;
        return brandService.assignOwner(trecAuthentication.getAccount(), userId.getBody(), uuid)
                .map((Boolean result) -> new ResponseEntity<Void>(result ? HttpStatus.NO_CONTENT: HttpStatus.FORBIDDEN));
    }

    @GetMapping(value = "/login/{id}")
    Mono<ResponseEntity<LoginToken>> loginAs(
            @PathVariable("id") String uuid,
            @RequestParam(value = "app", defaultValue = "") String app,
            HttpServerRequest request)
    {
        if("".equals(app))
            app = defaultApp;
        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();

        return brandService.LoginAsBrand(
                trecAuth,
                uuid,
                request.requestHeaders().get("User-Agent"),
                trecAuth.getSessionId(),
                trecAuth.getLoginToken().getExpires_in() > 0, app
        ).map((Optional<LoginToken> ret) ->{
            if(ret.isEmpty())
                return new ResponseEntity<LoginToken>(HttpStatus.FORBIDDEN);
            ret.get().setToken_type("User");

            return new ResponseEntity<LoginToken>(ret.get(), HttpStatus.OK);
        });


    }
}
