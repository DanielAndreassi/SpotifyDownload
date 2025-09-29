package com.spotifyweb.repository;

import com.spotifyweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySpotifyId(String spotifyId);

    Optional<User> findByEmail(String email);

    boolean existsBySpotifyId(String spotifyId);

    boolean existsByEmail(String email);
}