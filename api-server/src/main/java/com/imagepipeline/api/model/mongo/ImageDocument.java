package com.imagepipeline.api.model.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDocument {

    @Id
    private String id;  // Este id va como mongo_id en PostgreSQL

    // Referencia de vuelta a PostgreSQL
    private String pgImageId;

    // Tags de la imagen
    private List<String> tags;

    // Dimensiones originales
    private Dimensions originalDimensions;

    // Dimensiones actuales (puede cambiar tras un resize)
    private Dimensions currentDimensions;

    // Variantes generadas: "thumbnail" → "storage-key", "medium" → "storage-key"
    private Map<String, String> variants;

    // Paleta de colores dominantes en hex
    private List<String> colorPalette;

    // Metadatos EXIF (cámara, GPS, etc.)
    private Map<String, Object> exif;

    // Historial de operaciones realizadas
    private List<ProcessingRecord> processingHistory;

    private LocalDateTime createdAt;

    // ── Clases internas ─────────────────────────────────────────────

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Dimensions {
        private Integer width;
        private Integer height;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessingRecord {
        private String operation;   // "resize", "compress", "watermark"
        private String resultKey;   // clave en Supabase Storage
        private Integer width;
        private Integer height;
        private LocalDateTime processedAt;
    }
}
