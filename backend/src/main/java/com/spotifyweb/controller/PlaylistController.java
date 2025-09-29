package com.spotifyweb.controller;

import com.spotifyweb.dto.AddTracksRequestDTO;
import com.spotifyweb.dto.CreatePlaylistRequestDTO;
import com.spotifyweb.dto.PlaylistDTO;
import com.spotifyweb.entity.User;
import com.spotifyweb.service.SpotifyService;
import com.spotifyweb.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/playlists")
public class PlaylistController {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private UserService userService;

    /**
     * Helper method to retrieve the custom User entity from the Authentication principal.
     */
    private User getUserFromAuth(Authentication authentication) {
        String spotifyId = authentication.getName();
        return userService.findBySpotifyId(spotifyId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + spotifyId));
    }

    @GetMapping
    public ResponseEntity<?> getUserPlaylists(Authentication authentication) {
        logger.info("getUserPlaylists called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            List<PlaylistDTO> playlists = spotifyService.getUserPlaylists(user);
            logger.info("Successfully fetched {} playlists for user {}", playlists.size(), user.getDisplayName());
            return ResponseEntity.ok(playlists);
        } catch (IllegalStateException e) {
            logger.error("Authentication error in getUserPlaylists", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch user playlists", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch playlists: " + e.getMessage()));
        }
    }

    @GetMapping("/liked-tracks")
    public ResponseEntity<?> getLikedTracksInfo(Authentication authentication) {
        logger.info("getLikedTracksInfo called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            var likedTracks = spotifyService.getLikedTracks(user);
            PlaylistDTO likedPlaylist = new PlaylistDTO(
                    "liked-tracks",
                    "Músicas curtidas",
                    "Coleção com todas as músicas que você favoritou no Spotify.",
                    likedTracks.size(),
                    null,
                    user.getDisplayName()
            );
            return ResponseEntity.ok(likedPlaylist);
        } catch (IllegalStateException e) {
            logger.error("Authentication error in getLikedTracksInfo", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch liked tracks info", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch liked tracks: " + e.getMessage()));
        }
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<?> getTopTracksInfo(@RequestParam(defaultValue = "20") int limit, Authentication authentication) {
        logger.info("getTopTracksInfo called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            var topTracks = spotifyService.getTopTracks(user, Math.min(limit, 50));
            PlaylistDTO topTracksPlaylist = new PlaylistDTO(
                    "top-tracks",
                    "Top músicas",
                    "As músicas que você mais ouviu nos últimos tempos.",
                    topTracks.size(),
                    null,
                    user.getDisplayName()
            );
            return ResponseEntity.ok(topTracksPlaylist);
        } catch (IllegalStateException e) {
            logger.error("Authentication error in getTopTracksInfo", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch top tracks info", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch top tracks: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPlaylists(@RequestParam String query, Authentication authentication) {
        logger.info("searchPlaylists called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            var spotifyPlaylists = spotifyService.searchPlaylists(user, query);
            List<PlaylistDTO> playlists = new ArrayList<>();
            for (var playlist : spotifyPlaylists.getItems()) {
                if (playlist == null) continue;
                playlists.add(new PlaylistDTO(
                        playlist.getId(),
                        playlist.getName() != null ? playlist.getName() : "Playlist sem nome",
                        null,
                        playlist.getTracks() != null ? playlist.getTracks().getTotal() : 0,
                        playlist.getImages() != null && playlist.getImages().length > 0 ? playlist.getImages()[0].getUrl() : null,
                        playlist.getOwner() != null ? playlist.getOwner().getDisplayName() : ""
                ));
            }
            return ResponseEntity.ok(playlists);
        } catch (IllegalStateException e) {
            logger.error("Authentication error in searchPlaylists", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to search playlists for query: {}", query, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Não foi possível buscar playlists: " + e.getMessage()));
        }
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<?> getPlaylistTracks(@PathVariable String playlistId, Authentication authentication) {
        logger.info("getPlaylistTracks called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            if ("liked-tracks".equals(playlistId)) {
                return ResponseEntity.ok(spotifyService.getLikedTracks(user));
            }
            if ("top-tracks".equals(playlistId)) {
                return ResponseEntity.ok(spotifyService.getTopTracks(user, 50));
            }
            return ResponseEntity.ok(spotifyService.getPlaylistTracks(user, playlistId));
        } catch (IllegalStateException e) {
            logger.error("Authentication error in getPlaylistTracks", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch tracks for playlistId: {}", playlistId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch playlist tracks: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPlaylist(@RequestBody CreatePlaylistRequestDTO request, Authentication authentication) {
        logger.info("createPlaylist called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            var newPlaylist = spotifyService.createPlaylist(user, request.getName(), request.getDescription());
            return ResponseEntity.ok(newPlaylist);
        } catch (IllegalStateException e) {
            logger.error("Authentication error in createPlaylist", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create playlist with name: {}", request.getName(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create playlist: " + e.getMessage()));
        }
    }

    @PostMapping("/{playlistId}/add-tracks")
    public ResponseEntity<?> addTracksToPlaylist(@PathVariable String playlistId, @RequestBody AddTracksRequestDTO request, Authentication authentication) {
        logger.info("addTracksToPlaylist called for user: {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            spotifyService.addTracksToPlaylist(user, playlistId, request.getTrackUris());
            return ResponseEntity.ok(Map.of("message", "Tracks added successfully"));
        } catch (IllegalStateException e) {
            logger.error("Authentication error in addTracksToPlaylist", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add tracks to playlistId: {}", playlistId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add tracks: " + e.getMessage()));
        }
    }
}
