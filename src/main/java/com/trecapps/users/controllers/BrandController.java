package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.LoginToken;
import com.trecapps.auth.common.models.TcBrands;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.common.models.secondary.BrandEntry;
import com.trecapps.auth.webflux.services.BrandServiceAsync;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.users.models.*;
import lombok.extern.slf4j.Slf4j;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/Brands")
public class BrandController {

    @Autowired
    BrandServiceAsync brandService;

    @Autowired
    IUserStorageServiceAsync userStorageService;

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

    @PostMapping(value = "/New")
    Mono<ResponseEntity<ResponseObj>> submitNewBrand(@RequestBody NewBrand newBrand, Authentication authentication)
    {
        return Mono.just(((TrecAuthentication) authentication).getUser())
                .flatMap((TcUser user) -> {
                    TcBrands newBrandObj = new TcBrands();
                    newBrandObj.setName(newBrand.getName());
                    newBrandObj.setId(UUID.randomUUID().toString());
                    newBrandObj.getOwners().add(user.getId());

                    user.getBrands().add(newBrandObj.getId());
                    if(newBrand.isMakeDedicated()){
                        if(user.getDedicatedBrandAccount() == null){
                            user.setDedicatedBrandAccount(newBrandObj.getId());
                        } else throw new ObjectResponseException(HttpStatus.CONFLICT, "You already have a dedicated Brand account!");
                    }

                    return this.userStorageService.saveBrandMono(newBrandObj)
                            .thenReturn(ResponseObj.getInstance("Success", newBrandObj.getId()))
                            .flatMap((ResponseObj obj) ->{
                                return userStorageService.saveUserMono(user).thenReturn(obj);
                            }).onErrorResume(Throwable.class, (Throwable t) -> {
                                log.error("Failed to save Brand {} for User {}", newBrandObj.getId(), user.getId());
                                log.error("Error was ", t);
                                return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Create new Brand Account!"));
                            });
                })
                .onErrorResume((ObjectResponseException.class), (ObjectResponseException e) -> Mono.just(e.toResponseObject()))
                .map(ResponseObj::toEntity);
//                .flatMap((TcUser user) -> {
//                    return Flux.fromIterable(user.getBrands())
//                            .flatMap((String brandId) -> this.userStorageService.getBrandById(brandId))
//                            .collectList()
//                            .map((List<Optional<TcBrands>> brands) -> {
//                                return brands.stream()
//                                        .filter(Optional::isPresent)
//                                        .map(Optional::get)
//                                        .toList();
//                            });
//                })
//                .doOnNext((List<TcBrands> brands) -> {
//                    if(newBrand.isMakeDedicated()){
//
//                    }
//                })
//        return Mono.just(new AuthenticationBody<>((TrecAuthentication) authentication, name.getBody()))
//                .flatMap((AuthenticationBody<String> ab) -> brandService.createNewBrand(ab.getAuthentication().getAccount(), ab.getData()))
//                .map((String result) -> {
//                    String[] parts = result.split(":");
//
//                    return new ResponseEntity<>(parts[1], HttpStatus.valueOf(Integer.parseInt(parts[0])));
//                });
    }

    @PatchMapping("/name")
    Mono<ResponseEntity<ResponseObj>> updateName(Authentication authentication, @RequestBody Login details) {
        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;

        return this.userStorageService
                .getBrandById(details.getUsername())
                .map((Optional<TcBrands> oBrands) -> {
                    if(oBrands.isEmpty()) throw new ObjectResponseException(HttpStatus.NOT_FOUND, "Brand Not Found");
                    TcBrands brands = oBrands.get();

                    if(brands.getOwners().contains(trecAuthentication.getUser().getId()))
                        throw new ObjectResponseException(HttpStatus.FORBIDDEN, "You Do not own this brand Account!");
                    return brands;
                })
                .flatMap((TcBrands brands) -> {
                    brands.setName(details.getPassword());

                    return this.userStorageService
                            .saveBrandMono(brands)
                            .thenReturn(ResponseObj.getInstance("Success!"))
                            .map(ResponseObj::toEntity);
                })
                .onErrorResume(ObjectResponseException.class, (ObjectResponseException e) -> Mono.just(e.toResponseEntity()))
                .onErrorResume(Throwable.class, (Throwable t) -> {
                    log.error("Failed Name Update Operation! ", t);
                    return Mono.just(ResponseObj.getInstance(HttpStatus.INTERNAL_SERVER_ERROR, "Error Updating Name").toEntity());
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

        return brandService.loginAsBrand(
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
