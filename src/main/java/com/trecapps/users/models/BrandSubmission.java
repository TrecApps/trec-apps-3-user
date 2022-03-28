package com.trecapps.users.models;

import com.trecapps.base.InfoResource.models.Brand;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class BrandSubmission {
    String name;
}
