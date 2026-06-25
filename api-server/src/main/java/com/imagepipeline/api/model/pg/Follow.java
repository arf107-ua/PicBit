package com.imagepipeline.api.model.pg;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingId")
    @JoinColumn(name = "following_id")
    private User following;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Composite PK ────────────────────────────────────────────────

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowId implements Serializable {
        @Column(name = "follower_id")
        private UUID followerId;

        @Column(name = "following_id")
        private UUID followingId;
    }
}
