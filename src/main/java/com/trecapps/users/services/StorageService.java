package com.trecapps.users.services;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trecapps.users.models.TcBrands;
import com.trecapps.users.models.TcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class StorageService {
    BlobServiceClient client;

    ObjectMapper objectMapper;

    @Autowired
    StorageService(BlobServiceClient client1)
    {
        client = client1;
        objectMapper = new ObjectMapper();
    }


    public TcUser retrieveUser(String id) throws JsonProcessingException {
        BlobContainerClient containerClient = client.getBlobContainerClient("TrecApps-Users");

        BlobClient client = containerClient.getBlobClient("user-" + id);

        BinaryData bData = client.downloadContent();

        String data = new String(bData.toBytes(), StandardCharsets.UTF_8);

        return objectMapper.readValue(data, TcUser.class);
    }

    public TcBrands retrieveBrand(UUID id) throws JsonProcessingException
    {
        BlobContainerClient containerClient = client.getBlobContainerClient("TrecApps-Users");

        BlobClient client = containerClient.getBlobClient("brand-" + id);

        BinaryData bData = client.downloadContent();

        String data = new String(bData.toBytes(), StandardCharsets.UTF_8);

        return objectMapper.readValue(data, TcBrands.class);
    }

    public void saveUser(TcUser user)
    {
        BlobContainerClient containerClient = client.getBlobContainerClient("TrecApps-Users");

        BlobClient client = containerClient.getBlobClient("user-" + user.getId());

        client.upload(BinaryData.fromObject(user));
    }

    public void saveBrand(TcBrands brand)
    {
        BlobContainerClient containerClient = client.getBlobContainerClient("TrecApps-Users");

        BlobClient client = containerClient.getBlobClient("brand-" + brand.getId());

        client.upload(BinaryData.fromObject(brand));
    }
}
