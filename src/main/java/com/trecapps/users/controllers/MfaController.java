package com.trecapps.users.controllers;

import com.trecapps.auth.common.models.MfaMechanism;
import com.trecapps.auth.common.models.MfaRegistrationData;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.auth.common.models.TrecAuthentication;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.trecapps.auth.webflux.services.JwtTokenServiceAsync;
import com.trecapps.auth.webflux.services.MfaServiceAsync;
import com.trecapps.users.models.MfaSubmission;
import com.trecapps.users.services.TrecEmailService;
import com.trecapps.users.services.TrecSmsService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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

    @Autowired
    MfaServiceAsync mfaService;

    @Autowired
    TrecEmailService emailService;

    @Autowired
    IUserStorageServiceAsync userStorageServiceAsync;

    @Autowired(required = false)
    TrecSmsService smsService;

    @Autowired
    JwtTokenServiceAsync jwtTokenServiceAsync;


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
                        return mfaService.getQRCode(tAuth.getUser());
                    } catch (QrGenerationException e) {
                        log.error("Error generating QR Code!", e);
                        response.setRawStatusCode(500);
                        return new MfaRegistrationData("", "");
                    }
                });

    }

    @GetMapping("/options")
    Mono<List<String>> getOptions(Authentication authentication)
    {
        return convertAuth(authentication)
                .map((TrecAuthentication auth) -> mfaService.getAvailableMFAOptions(auth.getUser()));
    }

    Mono<Void> setCodeOnUser(TcUser user, boolean isEmail, String code){
        List<MfaMechanism> mechanisms = user.getMfaMechanisms();

        Optional<MfaMechanism> oMechanism = user.getMechanism(isEmail ? "Email" : "Phone");
        MfaMechanism mechanism = null;
        if(oMechanism.isEmpty()){
            mechanism = new MfaMechanism();
            mechanism.setSource(isEmail ? "Email" : "Phone");
            mechanisms.add(mechanism);
        }
        else mechanism = oMechanism.get();

        mechanism.setCode(code);
        mechanism.setExpires(OffsetDateTime.now().plusMinutes(5));
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
                    return jwtTokenServiceAsync.addMfa(request.getHeaders().getFirst("Authorization")).getToken();



                });
    }

}
