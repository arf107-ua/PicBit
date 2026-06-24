package com.imagepipeline.worker.model;

import java.io.Serializable;

/**
 * Message received from RabbitMQ.
 * The api-server sends one of these for each task (RESIZE, COMPRESS, WATERMARK).
 *
 * Example JSON:
 * {
 *   "imageKey": "550e8400-e29b-41d4-a716-446655440000.jpg",
 *   "task": "RESIZE",
 *   "targetWidth": 200,
 *   "targetHeight": 200
 * }
 */
public class ImageTaskMessage implements Serializable {

    private String imageKey;    // filename in MinIO source bucket
    private String task;        // RESIZE | COMPRESS | WATERMARK
    private int targetWidth;    // used by RESIZE
    private int targetHeight;   // used by RESIZE

    public ImageTaskMessage() {}

    public ImageTaskMessage(String imageKey, String task, int targetWidth, int targetHeight) {
        this.imageKey = imageKey;
        this.task = task;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public int getTargetWidth() { return targetWidth; }
    public void setTargetWidth(int targetWidth) { this.targetWidth = targetWidth; }

    public int getTargetHeight() { return targetHeight; }
    public void setTargetHeight(int targetHeight) { this.targetHeight = targetHeight; }

    @Override
    public String toString() {
        return "ImageTaskMessage{imageKey='" + imageKey + "', task='" + task + "'}";
    }
}
