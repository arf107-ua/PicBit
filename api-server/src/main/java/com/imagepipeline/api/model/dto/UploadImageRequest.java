package com.imagepipeline.api.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class UploadImageRequest {
    private String title;
    private String description;
    private List<String> tags;
    private Boolean isPublic = true;
    private String boardId;
}
