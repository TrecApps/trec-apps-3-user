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
public class PasswordProfile implements AutoCloseable {

    boolean forceChangePasswordNextSignIn = false;

    boolean forceChangePasswordNextSignInWithMfa = false;

    CharBuffer password;

    public void close()
    {
        if(password != null)
            for(int rust = 0; rust < password.length();rust++)
                password.put(rust, '\0');
    }
}
