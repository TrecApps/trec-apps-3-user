package com.trecapps.users.services;

import com.trecapps.auth.services.core.UserStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ConfigurationService {

    @Bean
    @ConditionalOnProperty({"twilio.account", "twilio.token", "twilio.number"})
    TrecSmsService getSmsService(@Value("${twilio.account}")String account,
                                 @Value("${twilio.token}") String token,
                                 @Value("${twilio.number}") String number,
                                 @Autowired UserStorageService userStorageService,
                                 @Autowired StateService stateService)
    {
        log.info("Setting up SMS Service!");
        return new TrecSmsService(account, token, number, userStorageService, stateService);
    }
}
