package com.imagepipeline.api.repository.pg;

import com.imagepipeline.api.model.pg.ResizeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResizeJobRepository extends JpaRepository<ResizeJob, UUID> {

    List<ResizeJob> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ResizeJob> findByImageIdOrderByCreatedAtDesc(UUID imageId);

    List<ResizeJob> findByStatus(ResizeJob.JobStatus status);
}
