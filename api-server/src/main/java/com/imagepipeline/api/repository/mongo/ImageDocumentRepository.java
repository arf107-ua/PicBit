package com.imagepipeline.api.repository.mongo;

import com.imagepipeline.api.model.mongo.ImageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageDocumentRepository extends MongoRepository<ImageDocument, String> {

    // Buscar por el ID de PostgreSQL
    Optional<ImageDocument> findByPgImageId(String pgImageId);

    // Buscar por tag
    List<ImageDocument> findByTagsContaining(String tag);

    // Buscar por múltiples tags
    List<ImageDocument> findByTagsIn(List<String> tags);
}
