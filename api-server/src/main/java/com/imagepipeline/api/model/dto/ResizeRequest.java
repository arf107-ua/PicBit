package com.imagepipeline.api.model.dto;

import lombok.Data;

@Data
public class ResizeRequest {
    private String imageId;
    private Integer width;
    private Integer height;
}
