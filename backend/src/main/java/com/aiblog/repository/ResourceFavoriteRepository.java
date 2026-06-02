package com.aiblog.repository;

import com.aiblog.entity.ResourceFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceFavoriteRepository extends JpaRepository<ResourceFavorite, Long> {
    Optional<ResourceFavorite> findByUserIdAndRefTypeAndRefId(Long userId, ResourceFavorite.RefType refType, Long refId);
    boolean existsByUserIdAndRefTypeAndRefId(Long userId, ResourceFavorite.RefType refType, Long refId);
    long countByRefTypeAndRefId(ResourceFavorite.RefType refType, Long refId);
    Page<ResourceFavorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query(value = """
            insert ignore into resource_favorite (user_id, ref_type, ref_id, created_at)
            values (:userId, :refType, :refId, current_timestamp(6))
            """, nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId,
                     @Param("refType") String refType,
                     @Param("refId") Long refId);

    @Modifying
    @Query("delete from ResourceFavorite f where f.userId = :userId and f.refType = :refType and f.refId = :refId")
    int deleteByUserIdAndRefTypeAndRefId(@Param("userId") Long userId,
                                         @Param("refType") ResourceFavorite.RefType refType,
                                         @Param("refId") Long refId);
}
