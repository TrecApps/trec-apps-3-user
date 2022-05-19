package com.trecapps.users.services;

import org.springframework.stereotype.Service;

@Service
public class StateService {

    private final String OPTIONS = "abcdefg1234567";

    private final int CODE_LENGTH = 10;

    public String generateState() {
        StringBuilder ret = new StringBuilder();
        for(int rust = 0; rust < 10; rust++)
        {
            double res = Math.random() * OPTIONS.length();
            int condensed = (int)res;
            ret.append(OPTIONS.charAt(condensed));
        }

        return ret.toString();
    }
}
