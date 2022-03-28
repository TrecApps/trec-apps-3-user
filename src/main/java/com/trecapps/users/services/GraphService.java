package com.trecapps.users.services;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserChangePasswordParameterSet;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.trecapps.users.models.PasswordChange;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    @Value("${tenent.id}")
    String tenentId;

    @Value("${client.id}")
    String clientId;

    Logger logger = LoggerFactory.getLogger(GraphService.class);

    private GraphServiceClient<Request> graphClient = null;
    private TokenCredentialAuthProvider authProvider = null;

    private void initializeClient()
    {
        if(graphClient != null)
            return;
        final DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenentId)
                .challengeConsumer(challenge -> logger.info(challenge.getMessage()))
                .build();

        authProvider = new TokenCredentialAuthProvider(credential);

        graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    boolean updatePassword(PasswordChange passwordChange, String userId, String auth)
    {
        initializeClient();

        try {
            graphClient.users(userId).changePassword(UserChangePasswordParameterSet.newBuilder()
                    .withCurrentPassword(String.valueOf(passwordChange.getCurrentPassword()))
                    .withNewPassword(String.valueOf(passwordChange.getNewPassword())).build()).buildRequest(new HeaderOption("Authorization", auth)).post();
            return true;
        }catch(ClientException ex)
        {
            logger.error("Client Exception in Changing Password", ex);
            return false;
        }
    }


    User getCurrentUser(String userId, String auth)
    {
        return graphClient.users(userId).buildRequest(new HeaderOption("Authorization", auth)).get();
    }
}
