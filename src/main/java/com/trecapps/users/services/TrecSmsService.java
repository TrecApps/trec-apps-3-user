package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.models.TcUser;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.UserStorageService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.TreeMap;

public class TrecSmsService {

    String account;
    String token;
    String number;

    UserStorageService userStorageService;
    StateService stateService;

    TrecSmsService(String account1,
                   String token1,
                   String number1,
                   UserStorageService userStorageService1,
                   StateService stateService1){
        account = account1;
        token = token1;
        number = number1;

        userStorageService = userStorageService1;
        stateService = stateService1;

        Twilio.init(account, token);
    }

    public boolean validatePhone(TrecAccount account, @NotNull String enteredCode) throws JsonProcessingException {
        TcUser user = userStorageService.retrieveUser(account.getId());

        Map<String, String> codes = user.getVerificationCodes();
        if(codes == null || codes.containsKey("SMS"))
            return false;

        // To-Do: Add Expiration Map in future build of TrecAuth and use here
        if(enteredCode.equals(codes.get("SMS")))
        {
            user.setPhoneVerified(true);
            userStorageService.saveUser(user);
            return true;
        }
        return false;
    }

    public void sendCode(TrecAccount account) throws JsonProcessingException {
        TcUser user = userStorageService.retrieveUser(account.getId());

        String code = stateService.generateState();

        Map<String, String> codes = user.getVerificationCodes();
        if(codes == null)
            codes = new TreeMap<>();

        codes.put("SMS", code);
        user.setVerificationCodes(codes);

        userStorageService.saveUser(user);

        sendCode(code, user.getMobilePhone());
    }

    public void sendCode(String code, String phone){
        Message message = Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(number),
                String.format("TrecApps Verification code is %s. Do not share this value", code))
                .create(Twilio.getRestClient());


    }
}
