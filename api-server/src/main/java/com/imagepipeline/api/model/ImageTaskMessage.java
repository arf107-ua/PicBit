package com.imagepipeline.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageTaskMessage {
    private String jobId;
    private String storageKey;
    private String userId;
    private String operation;
    private Integer width;
    private Integer height;
}
