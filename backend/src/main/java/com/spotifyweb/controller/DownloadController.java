package com.spotifyweb.controller;

import com.spotifyweb.dto.DownloadRequestDTO;
import com.spotifyweb.entity.User;
import com.spotifyweb.service.DownloadService;
import com.spotifyweb.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/download", "/downloads"})
public class DownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private UserService userService;

    private User getUserFromAuth(Authentication authentication) {
        String spotifyId = authentication.getName();
        return userService.findBySpotifyId(spotifyId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + spotifyId));
    }

    @PostMapping("/playlist")
    public ResponseEntity<?> downloadPlaylist(@RequestBody DownloadRequestDTO request, Authentication authentication) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }
        try {
            User user = getUserFromAuth(authentication);
            String jobId = downloadService.downloadPlaylist(request, user);
            return ResponseEntity.ok(Map.of(
                    "message", "Download da playlist iniciado",
                    "jobId", jobId
            ));
        } catch (Exception e) {
            logger.error("Failed to start playlist download", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start download: " + e.getMessage()));
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startDownloadByPlaylistId(@RequestBody Map<String, String> request, Authentication authentication) {
        String playlistId = request.get("playlistId");
        if (playlistId == null || playlistId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Playlist ID is required"));
        }
        try {
            User user = getUserFromAuth(authentication);
            String jobId = downloadService.downloadPlaylistById(playlistId, user);
            return ResponseEntity.ok(Map.of(
                    "message", "Download started for playlist",
                    "jobId", jobId
            ));
        } catch (Exception e) {
            logger.error("Failed to start download for playlist {}", playlistId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start download: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getDownloadStatus(@PathVariable String jobId, Authentication authentication) {
        try {
            User user = getUserFromAuth(authentication);
            return downloadService.getDownloadProgress(jobId, user.getId())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Download job not found")));
        } catch (Exception e) {
            logger.error("Failed to fetch download status for job {}", jobId, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch download status: " + e.getMessage()));
        }
    }

    @PostMapping("/album")
    public ResponseEntity<?> downloadAlbum(@RequestBody DownloadRequestDTO request, Authentication authentication) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }
        try {
            User user = getUserFromAuth(authentication);
            String jobId = downloadService.downloadAlbum(request, user);
            return ResponseEntity.ok(Map.of(
                    "message", "Download do álbum iniciado",
                    "jobId", jobId
            ));
        } catch (Exception e) {
            logger.error("Failed to start album download", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start download: " + e.getMessage()));
        }
    }

    @PostMapping("/track")
    public ResponseEntity<?> downloadTrack(@RequestBody DownloadRequestDTO request, Authentication authentication) {
        return ResponseEntity.status(501).body(Map.of("error", "Download individual de faixas ainda não está disponível."));
    }
}
