package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.*;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import com.trecapps.auth.webflux.services.MfaServiceAsync;
import com.trecapps.users.models.MfaSubmission;
import com.trecapps.users.models.ResponseObj;
import com.trecapps.users.services.TrecEmailService;
import com.trecapps.users.services.TrecSmsService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/mfa")
@ConditionalOnProperty(prefix = "trecauth.mfa", name = "enabled", havingValue = "true")
@Slf4j
public class MfaController {

    MfaServiceAsync mfaService;

    TrecEmailService emailService;

    IUserStorageServiceAsync userStorageServiceAsync;

    TrecSmsService smsService;

    JwtTokenServiceAsync jwtTokenServiceAsync;

    List<String> applist;

    String app;

    @Autowired
    MfaController(
            MfaServiceAsync mfaService,
            TrecEmailService emailService,
            IUserStorageServiceAsync userStorageServiceAsync,
            @Autowired(required = false)TrecSmsService smsService,
            JwtTokenServiceAsync jwtTokenServiceAsync,
            @Value("${trecapps.applist}") String listStr,
            @Value("${trecauth.app}") String app1
    ){
        this.jwtTokenServiceAsync = jwtTokenServiceAsync;
        this.smsService = smsService;
        this.mfaService = mfaService;
        this.emailService = emailService;
        this.userStorageServiceAsync = userStorageServiceAsync;

        String[] appArray = listStr.split(";");
        applist = List.of(appArray);
        this.app = app1;
        log.info("MFA Controller created!");
    }


    Mono<TrecAuthentication> convertAuth(Authentication authentication){
        return Mono.just(authentication)
                .map((Authentication auth) -> (TrecAuthentication) auth);
    }

    @GetMapping("/register")
    Mono<MfaRegistrationData> getToken(Authentication authentication, ServerHttpResponse response)
    {
        return convertAuth(authentication)
                .map((TrecAuthentication tAuth) -> {
                    String code = mfaService.setUpKey(tAuth.getUser());
                    assert(code != null);
                    try {
                        return mfaService.getQRCode(tAuth.getUser(), code);
                    } catch (QrGenerationException e) {
                        log.error("Error generating QR Code!", e);
                        response.setRawStatusCode(500);
                        return new MfaRegistrationData("", "");
                    }
                });

    }

    @GetMapping("/EnablePhone")
    Mono<Void> enablePhone(Authentication authentication, ServerHttpResponse response){
        return convertAuth(authentication)
                .doOnNext((TrecAuthentication tAuth) -> {
                    boolean worked = mfaService.enablePhoneVerification(tAuth.getUser());
                    response.setRawStatusCode(worked ? 202 : 400);
                }).then();
    }

    @GetMapping("/EnableEmail")
    Mono<Void> enableEmail(Authentication authentication, ServerHttpResponse response){
        return convertAuth(authentication)
                .doOnNext((TrecAuthentication tAuth) -> {
                    boolean worked = mfaService.enableEmailVerification(tAuth.getUser());
                    response.setRawStatusCode(worked ? 202 : 400);
                }).then();
    }

    @GetMapping("/options")
    Mono<List<String>> getOptions(Authentication authentication)
    {
        return convertAuth(authentication)
                .map((TrecAuthentication auth) -> mfaService.getAvailableMFAOptions(auth.getUser()));
    }

    boolean assertMfaRequired(TcUser user){
        for(MfaReq req: user.getMfaRequirements()){
            if(app.equals(req.getApp()))
                return false;
        }

        MfaReq req = new MfaReq();
        req.setApp(app);
        req.setRequireMfa(true);
        user.getMfaRequirements().add(req);
        return true;
    }

    Mono<Void> setCodeOnUser(TcUser user, boolean isEmail, String code){
        List<MfaMechanism> mechanisms = user.getMfaMechanisms();

        Optional<MfaMechanism> oMechanism = user.getMechanism(isEmail ? "Email" : "Phone");
        MfaMechanism mechanism = null;
        if(oMechanism.isEmpty())return Mono.empty();

        mechanism = oMechanism.get();

        mechanism.setCode(code);
        mechanism.setExpires(OffsetDateTime.now().plusMinutes(5));

        assertMfaRequired(user);

        return Mono.just(user)
                .flatMap((TcUser u) -> userStorageServiceAsync.saveUserMono(u));
    }

    @GetMapping("/select/{option:Email|Phone|Token}")
    Mono<Void> selectOption(Authentication authentication, @PathVariable("option")String option, ServerHttpResponse response){
        return convertAuth(authentication)
                .flatMap((TrecAuthentication auth) -> {
                    response.setStatusCode(HttpStatusCode.valueOf(202));
                    return switch (option) {
                        case "Email" -> emailService.sendValidationEmail(auth.getUser())
                                .flatMap((String code) -> setCodeOnUser(auth.getUser(), true, code));
                        case "Phone" -> {
                            if (smsService == null) {
                                response.setStatusCode(HttpStatusCode.valueOf(415));
                                yield Mono.empty();
                            }
                            yield smsService.sendCode(auth.getUser())
                                    .flatMap((String code) -> setCodeOnUser(auth.getUser(), false, code));
                        }
                        default -> Mono.empty();
                    };
                });
    }

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    Mono<String> postCode(Authentication authentication,
                          @RequestBody MfaSubmission submission,
                          ServerHttpRequest request,
                          ServerHttpResponse response){
        return convertAuth(authentication)
                .map((TrecAuthentication auth) -> {
                    boolean valid = false;

                    TcUser user = auth.getUser();

                    if("Token".equals(submission.getType())){
                        valid = mfaService.verifyTotp(submission.getCode(), user);
                    } else {
                        Optional<MfaMechanism> oMechanism = user.getMechanism(submission.getType());
                        if(oMechanism.isEmpty())
                            response.setStatusCode(HttpStatusCode.valueOf(400));
                        else {
                            MfaMechanism mechanism = oMechanism.get();
                            valid = submission.getCode().equals(mechanism.getCode())
                                && OffsetDateTime.now().isBefore(mechanism.getExpires());
                        }
                    }

                    if(!valid)
                    {
                        response.setStatusCode(HttpStatusCode.valueOf(400));
                        return "";
                    }
                    auth.getLoginToken();
                    String ret = jwtTokenServiceAsync
                            .addMfa(request.getHeaders().getFirst("Authorization"))
                            .getToken();


                    if(assertMfaRequired(user)){
                        this.userStorageServiceAsync.saveUserMono(user).subscribe();
                    }
                    return ret;

                });
    }


    @GetMapping("/appList")
    Mono<List<String>> getAppList(){
        return Mono.just(this.applist);
    }

    @DeleteMapping("/Token")
    Mono<ResponseEntity<ResponseObj>> removeToken(Authentication auth){
        return convertAuth(auth)
                .flatMap((TrecAuthentication tAuth) -> {
                    TcUser user = tAuth.getUser();

                    user.setMfaMechanisms(
                            user.getMfaMechanisms()
                                    .stream()
                                    .filter((MfaMechanism m) -> !"Token".equals(m.getSource()))
                                    .toList()
                    );
                    return userStorageServiceAsync.saveUserMono(user);
                })
                .thenReturn(ResponseObj.getInstance("Success"))
                .map(ResponseObj::toEntity);
    }

    @PostMapping("/app")
    Mono<ResponseEntity<ResponseObj>> setMfaRequirement(@RequestBody MfaReq req, Authentication authentication){

        if(app.equals(req.getApp()) && !req.isRequireMfa())
            return Mono.just(ResponseObj.getInstance(HttpStatus.FORBIDDEN, "The User Management app requires MFA if enabled"))
                    .map(ResponseObj::toEntity);

        TrecAuthentication trecAuthentication = (TrecAuthentication) authentication;



        TcUser user = trecAuthentication.getUser();

        boolean looking = true;
        for(MfaReq req1 : user.getMfaRequirements()){
            if(req1.getApp().equals(req.getApp())){
                looking = false;
                req1.setRequireMfa(req.isRequireMfa());
                break;
            }
        }

        if(looking)
            user.getMfaRequirements().add(req);

        return this.userStorageServiceAsync.saveUserMono(user).thenReturn(ResponseObj.getInstance("Success"))
                .map(ResponseObj::toEntity);
    }

}
