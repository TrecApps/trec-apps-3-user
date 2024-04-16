package com.trecapps.users.controllers;

import com.trecapps.auth.models.LoginToken;
import com.trecapps.auth.models.TcBrands;
import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.auth.models.secondary.BrandEntry;
import com.trecapps.auth.services.login.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/Brands")
public class BrandController {

    @Autowired
    BrandService brandService;

    @Value("${trecauth.app}") String defaultApp;


    @GetMapping("/{id}")
    ResponseEntity<TcBrands> getBrand(@PathVariable("id") String uuid)
    {
        TcBrands brand = null;
        try {
            brand = brandService.getBrandById(uuid, ((TrecAuthentication) SecurityContextHolder.getContext().getAuthentication()).getAccount());
        } catch (NullPointerException | ClassCastException e)
        {
            brand = brandService.getBrandById(uuid, null);
        }

        return new ResponseEntity<>(brand, brand == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

    @GetMapping("/list")
    ResponseEntity<List<BrandEntry>> getBrands()
    {
        return new ResponseEntity<>(brandService.getBrandList(((TrecAuthentication)
                SecurityContextHolder.getContext().getAuthentication()).getAccount()), HttpStatus.OK);
    }

    @PostMapping(value = "/New", consumes = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> submitNewBrand(RequestEntity<String> name)
    {
        String result = brandService.createNewBrand(((TrecAuthentication)
                SecurityContextHolder.getContext().getAuthentication()).getAccount(), name.getBody());

        String[] parts = result.split("[:]");

        return new ResponseEntity<>(parts[1], HttpStatus.valueOf(Integer.parseInt(parts[0])));
    }

    @PutMapping(value = "/NewOwner/{id}", consumes = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<Void> assignNewOwner(RequestEntity<String> userId, @PathVariable("id") String uuid)
    {
        boolean result = brandService.assignOwner(((TrecAuthentication)
            SecurityContextHolder.getContext().getAuthentication()).getAccount(), userId.getBody(), uuid);

        return new ResponseEntity<Void>(result ? HttpStatus.NO_CONTENT: HttpStatus.FORBIDDEN);
    }

    @GetMapping(value = "/login/{id}")
    ResponseEntity<LoginToken> loginAs(
            @PathVariable("id") String uuid,
            @RequestParam(value = "app", defaultValue = "") String app,
            HttpServletRequest request)
    {
        if("".equals(app))
            app = defaultApp;
        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        LoginToken ret = brandService.LoginAsBrand(
                trecAuth,
                uuid,
                request.getHeader("User-Agent"),
                trecAuth.getSessionId(),
                trecAuth.getLoginToken().getExpires_in() > 0, app);

        if(null == ret)
            return new ResponseEntity<LoginToken>(HttpStatus.FORBIDDEN);

        ret.setToken_type("User");

        return new ResponseEntity<LoginToken>(ret, HttpStatus.OK);
    }
}
