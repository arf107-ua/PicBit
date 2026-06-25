package com.imagepipeline.api.service;

import com.imagepipeline.api.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate supabaseRestTemplate;

    /**
     * Sube un archivo a Supabase Storage y devuelve la clave (path) del archivo.
     */
    public String upload(MultipartFile file, String userId) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String storageKey = userId + "/" + UUID.randomUUID() + "." + extension;
        String uploadUrl = buildUploadUrl(storageKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType()));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> response = supabaseRestTemplate.exchange(
            uploadUrl, HttpMethod.POST, entity, String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error subiendo archivo a Supabase Storage: " + response.getStatusCode());
        }

        log.info("Archivo subido correctamente: {}", storageKey);
        return storageKey;
    }

    /**
     * Devuelve la URL pública de un archivo en Supabase Storage.
     */
    public String getPublicUrl(String storageKey) {
        return supabaseConfig.getSupabaseUrl()
            + "/storage/v1/object/public/"
            + supabaseConfig.getStorageBucket()
            + "/" + storageKey;
    }

    /**
     * Elimina un archivo de Supabase Storage.
     */
    public void delete(String storageKey) {
        String deleteUrl = buildUploadUrl(storageKey);
        supabaseRestTemplate.delete(deleteUrl);
        log.info("Archivo eliminado: {}", storageKey);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String buildUploadUrl(String storageKey) {
        return supabaseConfig.getSupabaseUrl()
            + "/storage/v1/object/"
            + supabaseConfig.getStorageBucket()
            + "/" + storageKey;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
