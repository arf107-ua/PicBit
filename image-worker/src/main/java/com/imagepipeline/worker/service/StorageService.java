package com.imagepipeline.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders; // <--- ESTE ES EL CORRECTO
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
public class StorageService {

    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${minio.bucket-source}") // Reutilizamos el nombre de la variable de configuración
    private String sourceBucket;

    @Value("${minio.bucket-output}")
    private String outputBucket;

    public StorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public InputStream download(String imageKey) {
        log.info("Descargando {} desde Supabase bucket {}", imageKey, sourceBucket);

        // La URL de Supabase para descargar archivos públicos
        String url = supabaseUrl + "/storage/v1/object/public/" + sourceBucket + "/" + imageKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

        if (response.getBody() == null) {
            throw new RuntimeException("No se pudo descargar la imagen: " + imageKey);
        }

        return new ByteArrayInputStream(response.getBody());
    }

    public void upload(String resultKey, byte[] data, String contentType) {
        log.info("Subiendo {} a Supabase bucket {}", resultKey, outputBucket);

        String url = supabaseUrl + "/storage/v1/object/" + outputBucket + "/" + resultKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseKey);
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data, headers), String.class);

        log.info("Subida completada: {}", resultKey);
    }
}