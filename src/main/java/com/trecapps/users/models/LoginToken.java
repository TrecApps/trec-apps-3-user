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
public class LoginToken implements AutoCloseable{

    CharBuffer access_token, token_type, refresh_token, id_token;

    int expires_in;



    public void close()
    {
        if(access_token != null)
        {
            for(int rust = 0; rust < access_token.length(); rust++)
                access_token.put(rust, '\0');
            access_token = null;
        }
        if(token_type != null)
        {
            for(int rust = 0; rust < token_type.length(); rust++)
                token_type.put(rust, '\0');
            token_type = null;
        }
        if(refresh_token != null)
        {
            for(int rust = 0; rust < refresh_token.length(); rust++)
                refresh_token.put(rust, '\0');
            refresh_token = null;
        }
        if(id_token != null)
        {
            for(int rust = 0; rust < id_token.length(); rust++)
                id_token.put(rust, '\0');
            id_token = null;
        }
    }
}
