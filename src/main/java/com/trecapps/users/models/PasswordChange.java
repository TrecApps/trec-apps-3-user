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
public class PasswordChange implements AutoCloseable{

    CharBuffer currentPassword,newPassword;

    public void close()
    {
        if(currentPassword != null)
            for(int rust = 0; rust < currentPassword.length();rust++)
                currentPassword.put(rust, '\0');

        if(newPassword != null)
            for(int rust = 0; rust < newPassword.length();rust++)
                newPassword.put(rust, '\0');
    }
}
