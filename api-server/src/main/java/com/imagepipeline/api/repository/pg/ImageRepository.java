package com.imagepipeline.api.repository.pg;

import com.imagepipeline.api.model.pg.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    // Imágenes de un usuario concreto
    List<Image> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Imágenes públicas para el feed global
    List<Image> findByIsPublicTrueOrderByCreatedAtDesc();

    // Por mongo_id (para cruzar datos con MongoDB)
    Optional<Image> findByMongoId(String mongoId);

    // Feed personalizado: imágenes de usuarios que sigue el usuario actual
    @Query("""
        SELECT i FROM Image i
        WHERE i.user.id IN (
            SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId
        )
        AND i.isPublic = true
        ORDER BY i.createdAt DESC
    """)
    List<Image> findFeedForUser(@Param("userId") UUID userId);

    // Imágenes de un board concreto
    List<Image> findByBoardIdOrderByCreatedAtDesc(UUID boardId);
}
