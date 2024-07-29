package com.trecapps.users.services;

import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
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
                                 @Autowired IUserStorageServiceAsync userStorageService,
                                 @Autowired StateService stateService)
    {
        log.info("Setting up SMS Service!");
        return new TrecSmsService(account, token, number, userStorageService, stateService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "trecauth.mfa", name = "enabled", havingValue = "true")
    SecretGenerator getSecretGenerator(){
        return new DefaultSecretGenerator();
    }


    @Bean
    @ConditionalOnProperty(prefix = "trecauth.mfa", name = "enabled", havingValue = "true")
    QrGenerator getQRGenerator(){
        return new ZxingPngQrGenerator();
    }


    @Bean
    @ConditionalOnProperty(prefix = "trecauth.mfa", name = "enabled", havingValue = "true")
    CodeVerifier getCodeVerifier(){
        return new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    }
}
