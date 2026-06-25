package com.imagepipeline.api.repository.pg;

import com.imagepipeline.api.model.pg.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Follow.FollowId> {

    List<Follow> findByFollowerId(UUID followerId);

    List<Follow> findByFollowingId(UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    long countByFollowingId(UUID followingId);  // follower count

    long countByFollowerId(UUID followerId);    // following count
}
