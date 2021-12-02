package com.trecapps.users.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.nio.CharBuffer;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Login implements AutoCloseable {

    CharBuffer username, password;

    public void close()
    {
        if(username != null)
            for(int rust = 0; rust < username.length();rust++)
                username.put(rust, '\0');

        if(password != null)
            for(int rust = 0; rust < password.length();rust++)
                password.put(rust, '\0');
    }
}
