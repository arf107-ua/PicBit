package com.imagepipeline.api.model.dto;

import lombok.Data;

@Data
public class WsEvent {
    private String type;
    private Object payload;

    public WsEvent(String type, Object payload) {
        this.type    = type;
        this.payload = payload;
    }
}
