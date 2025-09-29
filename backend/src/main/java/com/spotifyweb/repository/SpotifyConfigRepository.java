package com.spotifyweb.repository;

import com.spotifyweb.entity.SpotifyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SpotifyConfigRepository extends JpaRepository<SpotifyConfig, Long> {

    @Query("SELECT s FROM SpotifyConfig s WHERE s.isActive = true")
    Optional<SpotifyConfig> findActiveConfig();

    @Transactional
    @Modifying
    @Query("UPDATE SpotifyConfig s SET s.isActive = false")
    void deactivateAllConfigs();
}