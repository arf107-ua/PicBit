package com.imagepipeline.worker.service;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Handles all communication with MinIO (object storage).
 *
 * Two buckets:
 *   - images-original  → where the api-server stores the uploaded image
 *   - images-processed → where workers save the result
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket-source}")
    private String sourceBucket;

    @Value("${minio.bucket-output}")
    private String outputBucket;

    public StorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // ---------------------------------------------------------------
    // Download original image from source bucket
    // Returns an InputStream — caller is responsible for closing it.
    // ---------------------------------------------------------------

    public InputStream download(String imageKey) {
        try {
            log.info("Downloading {} from bucket {}", imageKey, sourceBucket);

            ensureBucketExists(sourceBucket);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(sourceBucket)
                            .object(imageKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download image: " + imageKey, e);
        }
    }

    // ---------------------------------------------------------------
    // Upload processed image to output bucket
    // ---------------------------------------------------------------

    public void upload(String resultKey, byte[] data, String contentType) {
        try {
            log.info("Uploading {} ({} bytes) to bucket {}", resultKey, data.length, outputBucket);

            ensureBucketExists(outputBucket);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(outputBucket)
                            .object(resultKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Upload complete: {}", resultKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload result: " + resultKey, e);
        }
    }

    // ---------------------------------------------------------------
    // Creates a bucket if it does not exist yet.
    // Called before every operation to avoid startup ordering issues.
    // ---------------------------------------------------------------

    private void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + bucket, e);
        }
    }
}
