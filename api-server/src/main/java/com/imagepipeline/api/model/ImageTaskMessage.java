package com.imagepipeline.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageTaskMessage {
    private String jobId;
    private String storageKey;   // clave en Supabase Storage
    private String userId;
    private String operation;    // "resize", "compress", "watermark"
    private Integer width;
    private Integer height;
}
