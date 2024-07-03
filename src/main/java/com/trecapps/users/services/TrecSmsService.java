package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.auth.common.models.primary.TrecAccount;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Slf4j
public class TrecSmsService {

    String account;
    String token;
    String number;

    IUserStorageServiceAsync userStorageService;
    StateService stateService;

    TrecSmsService(String account1,
                   String token1,
                   String number1,
                   IUserStorageServiceAsync userStorageService1,
                   StateService stateService1){
        account = account1;
        token = token1;
        number = number1;

        log.info("Account: {}, Token: {}, number: {}", account, token, number);

        userStorageService = userStorageService1;
        stateService = stateService1;


    }

    public boolean hasBeenSetUp(){
        return account != null && token != null && number != null;
    }

    public Mono<Boolean> validatePhone(TrecAccount account, @NotNull String enteredCode) throws JsonProcessingException {

        return userStorageService.getAccountById(account.getId())
                .map((Optional<TcUser> optUser) -> {
                    if(optUser.isEmpty())
                    {
                        log.info("User Not Found");
                        return false;
                    }
                    TcUser user = optUser.get();
                    Map<String, String> codes = user.getVerificationCodes();
                    if(codes == null || !codes.containsKey("SMS")) {
                        log.info("SMS Code has not been set up!");
                        return false;
                    }
                    // To-Do: Add Expiration Map in future build of TrecAuth and use here
                    if(enteredCode.equals(codes.get("SMS")))
                    {
                        user.setPhoneVerified(true);
                        userStorageService.saveUser(user);
                        return true;
                    }
                    log.info("Codes {} and {} do not match!", enteredCode, codes.get("SMS"));
                    return false;
                });
    }

    public void sendCode(TcUser account) throws JsonProcessingException {

        Mono.just(account)
                .doOnNext((TcUser user) -> {

                    String code = stateService.generateState();

                    Map<String, String> codes = user.getVerificationCodes();
                    if(codes == null)
                        codes = new TreeMap<>();

                    codes.put("SMS", code);
                    user.setVerificationCodes(codes);

                    String phoneNumber = user.getMobilePhone().toString();

                    userStorageService.saveUser(user);

                    log.info("Sending message to {}", phoneNumber);

                    sendCode(code, phoneNumber);
                }).subscribe();
    }

    public void sendCode(String code, String phone){
        Twilio.init(account, token);
        Message message = Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(number),
                String.format("TrecApps Verification code is %s. Do not share this value", code))
                .create(Twilio.getRestClient());


    }
}
