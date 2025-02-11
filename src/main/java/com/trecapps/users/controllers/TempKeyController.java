package com.trecapps.users.controllers;

import com.trecapps.auth.common.rotate.KeyRotationUpdater;
import com.trecapps.users.models.ResponseObj;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/key-update")
@ConditionalOnProperty(prefix = "trecauth.rotate", name = "do-rotate", havingValue = "true")
public class TempKeyController {

    @Autowired
    KeyRotationUpdater updater;

    @GetMapping
    Mono<ResponseObj> updateKey(){
        return Mono.just(ResponseObj.getInstance("Success"))
                .doOnNext((ResponseObj ignore) -> {
                    try {
                        updater.execute(null);
                    } catch (JobExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).onErrorResume(
                        (Throwable thrown) -> Mono.just(ResponseObj.getInstance(HttpStatus.FORBIDDEN, thrown.getMessage())));
    }
}
