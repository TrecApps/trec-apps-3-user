package com.trecapps.users.controllers;

import com.trecapps.auth.models.TrecAuthentication;
import com.trecapps.pictures.models.PictureData;
import com.trecapps.pictures.services.PictureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@RequestMapping("/brandProfile")
@RestController
public class BrandProfileController {
    private static final List<String> imageTypes = Arrays.asList(
            "apng",
            "avif",
            "gif",
            "jpeg",
            "jpg",
            "png",
            "svg",
            "webp"
    );

    @Autowired
    PictureManager pictureManager;

    @PostMapping(value = "/set", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<String> setPic(RequestEntity<String> request, @RequestParam("brandId")String brandId)
    {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if(null == contentType)
            return new ResponseEntity<>("Requires Content-Type header to be provided!", HttpStatus.BAD_REQUEST);
        String type = null;
        contentType = contentType.toLowerCase(Locale.ROOT);
        for(String imageType: imageTypes)
        {
            if(contentType.contains(imageType))
            {
                type = imageType;
                break;
            }
        }

        if(null == type)
            return new ResponseEntity<>("Requires Content-Type header to have 'apng' 'avif' 'gif' 'jpeg' 'png' 'svg' 'webp' ''!", HttpStatus.BAD_REQUEST);

        TrecAuthentication trecAuth = (TrecAuthentication) SecurityContextHolder.getContext().getAuthentication();
        String id = pictureManager.addNewPicture(
                trecAuth.getAccount().getId(),                              // Id if the user adding the picture
                request.getHeaders().getFirst("Picture-Name"),   // Name fo he picture (if provided)
                type,                                                       // Type of image we're dealing with
                request.getBody(),                                          // The image data in base64 format
                true);                                              // It's the profile pic so might as well be public


        String[] results = pictureManager.setBrandProfile(brandId, trecAuth.getAccount().getId(), id).split("[:]");
        return new ResponseEntity<>(results[1], HttpStatus.valueOf(results[0]));
    }

    @GetMapping("/file/{fileName}")
    ResponseEntity<byte[]> getProfilePicture(@PathVariable("fileName")String fileName)
    {
        String[] filePieces = fileName.split("[.]");
        String name = filePieces[0];
        for(int rust = 1; rust < (filePieces.length - 1); rust++)
            name = "." + filePieces[rust];

        byte[] data = pictureManager.getBrandProfilePic(name, filePieces[filePieces.length -1]);
        if(data == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add("Content-type", String.format("image/%s", filePieces[filePieces.length - 1].toLowerCase(Locale.ROOT)));
        return new ResponseEntity<byte[]>(data, header, HttpStatus.OK);
    }

    @GetMapping("/imageType/{userId}")
    ResponseEntity<String> getProfileExtension(@PathVariable String userId)
    {
        String extension = pictureManager.getBrandProfilePicName(userId);
        if (extension == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add("Content-type", "text/plain");
        return new ResponseEntity<>(extension, header, HttpStatus.OK);
    }

    @GetMapping("/profilePic/{brandId}")
    ResponseEntity<byte[]> getProfilePictureDirectly(@PathVariable("brandId")String brandId)
    {
        String type = pictureManager.getPicName(brandId);
        if(type != null)
        {
            byte[] ret = pictureManager.getBrandProfilePic(brandId,type);
            if(ret != null) {
                MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
                header.add("Content-type", String.format("image/%s", type.toLowerCase(Locale.ROOT)));
                return new ResponseEntity<>(ret, header, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{userId}")
    ResponseEntity<PictureData> getProfile(@PathVariable("userId")String userId)
    {
        PictureData data = pictureManager.getBrandProfile(userId);
        return new ResponseEntity<>(data, data.getError() == null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }
}
