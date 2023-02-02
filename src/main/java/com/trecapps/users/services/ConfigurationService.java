package com.trecapps.users.services;

import com.trecapps.auth.services.UserStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationService {

    @Bean
    @ConditionalOnProperty({"twilio.account", "twilio.token", "twilio.number"})
    TrecSmsService getSmsService(@Value("twilio.account")String account,
                                 @Value("twilio.token") String token,
                                 @Value("twilio.number") String number,
                                 @Autowired UserStorageService userStorageService,
                                 @Autowired StateService stateService)
    {
        return new TrecSmsService(account, token, number, userStorageService, stateService);
    }
}
