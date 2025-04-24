package com.trecapps.users.controllers;

import com.trecapps.auth.common.rotate.KeyRotationUpdater;
import com.trecapps.users.models.ResponseObj;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

//@RestController
//@RequestMapping("/key-update")
//@ConditionalOnProperty(prefix = "trecauth.rotate", name = "do-rotate", havingValue = "true")
public class TempKeyController {

    //@Autowired
    SchedulerFactoryBean factoryBean;


    //@GetMapping
    Mono<ResponseObj> updateKey(){


        return Mono.just(ResponseObj.getInstance("Success"))
                .doOnNext((ResponseObj ignore) -> {
                    try {
                        Scheduler scheduler = factoryBean.getScheduler();
                        for (String groupName : scheduler.getJobGroupNames()) {
                            // get jobkey
                            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                                String jobName = jobKey.getName();
                                if(jobName.equals("Qrtz_Rotate_Detail"))
                                    scheduler.triggerJob(jobKey);
                            }
                        }
                        //updater.execute(null);
                    } catch (SchedulerException e) {
                        throw new RuntimeException(e);
                    }
                }).onErrorResume(
                        (Throwable thrown) -> Mono.just(ResponseObj.getInstance(HttpStatus.FORBIDDEN, thrown.getMessage())));
    }
}
