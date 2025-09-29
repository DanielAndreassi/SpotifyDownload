package com.spotifyweb.controller;

import com.spotifyweb.dto.SpotifyConfigDTO;
import com.spotifyweb.security.JwtUtil;
import com.spotifyweb.service.SpotifyService;
import com.spotifyweb.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class
AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // In-memory storage for state-credential mapping. Not suitable for production with multiple instances.
    private final Map<String, SpotifyConfigDTO> stateStorage = new ConcurrentHashMap<>();

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${frontend.redirect-url}")
    private String frontendRedirectUrl;

    @Value("${spotify.redirect-uri}")
    private String spotifyRedirectUri;

    @PostMapping("/authorize")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(@Valid @RequestBody SpotifyConfigDTO configDTO) {
        try {
            String state = UUID.randomUUID().toString();
            stateStorage.put(state, configDTO);

            String authUrl = spotifyService.getAuthorizationUrl(configDTO.getClientId(), configDTO.getClientSecret(), state);
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            response.put("message", "Please visit the authorization URL to authenticate");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate authorization URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/callback")
    public void handleCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
        logger.info("Callback received with code: {} and state: {}", code != null ? "present" : "null", state);

        SpotifyConfigDTO config = stateStorage.remove(state);

        if (config == null) {
            logger.error("Invalid or expired state parameter during callback. State: {}", state);
            response.sendRedirect(frontendRedirectUrl + "?error=invalid_state");
            return;
        }

        try {
            logger.info("Attempting to authenticate with Spotify using clientId: {}", config.getClientId());

            // Create Spotify API instance
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(config.getClientId())
                    .setClientSecret(config.getClientSecret())
                    .setRedirectUri(java.net.URI.create(spotifyRedirectUri))
                    .build();

            // Get access token
            AuthorizationCodeCredentials authorizationCodeCredentials = spotifyApi.authorizationCode(code).build().execute();
            String accessToken = authorizationCodeCredentials.getAccessToken();
            String refreshToken = authorizationCodeCredentials.getRefreshToken();

            // Set access token to get user info
            spotifyApi.setAccessToken(accessToken);
            User spotifyUser = spotifyApi.getCurrentUsersProfile().build().execute();

            // Create or update user in database
            String profileImageUrl = spotifyUser.getImages() != null && spotifyUser.getImages().length > 0
                    ? spotifyUser.getImages()[0].getUrl() : null;

            com.spotifyweb.entity.User user = userService.createOrUpdateUser(
                    spotifyUser.getId(),
                    spotifyUser.getDisplayName(),
                    spotifyUser.getEmail(),
                    accessToken,
                    refreshToken,
                    profileImageUrl
            );

            // Generate JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("displayName", user.getDisplayName());
            claims.put("email", user.getEmail());

            String jwtToken = jwtUtil.generateToken(user.getSpotifyId(), claims);

            // Set JWT as cookie
            Cookie jwtCookie = new Cookie("jwt_token", jwtToken);
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 hours
            jwtCookie.setPath("/");
            jwtCookie.setHttpOnly(false); // Allow JavaScript access
            jwtCookie.setSecure(false); // Allow HTTP for development
            response.addCookie(jwtCookie);

            // Also set it as a header for immediate availability
            response.setHeader("Authorization", "Bearer " + jwtToken);

            logger.info("Authentication successful for user: {}. Redirecting to frontend: {}", user.getDisplayName(), frontendRedirectUrl);
            response.sendRedirect(frontendRedirectUrl + "?token=" + jwtToken);
        } catch (Exception e) {
            logger.error("Error during authentication with code: {}", e.getMessage(), e);
            response.sendRedirect(frontendRedirectUrl + "?error=auth_failed");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        logger.info("Checking authentication status via SecurityContext...");
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            // The user is authenticated by JwtAuthenticationFilter
            String spotifyId = authentication.getName();
            
            // Fetch our custom user entity from the database
            return userService.findBySpotifyId(spotifyId).map(user -> {
                response.put("authenticated", true);
                response.put("user", user.getDisplayName());
                response.put("email", user.getEmail());
                response.put("spotifyId", user.getSpotifyId());
                response.put("userId", user.getId());
                logger.info("User is authenticated: {}", user.getDisplayName());
                return ResponseEntity.ok(response);
            }).orElseGet(() -> {
                // This case should be rare if JWT is valid but user is not in DB
                response.put("authenticated", false);
                response.put("error", "User not found in database");
                return ResponseEntity.status(404).body(response);
            });
        } else {
            // User is not authenticated
            response.put("authenticated", false);
            response.put("error", "No valid authentication token found");
            logger.warn("getAuthStatus check failed: No valid authentication found.");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Clear JWT cookie
            Cookie jwtCookie = new Cookie("jwt_token", "");
            jwtCookie.setMaxAge(0);
            jwtCookie.setPath("/");
            response.addCookie(jwtCookie);

            Map<String, String> result = new HashMap<>();
            result.put("message", "Logout successful");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> getDebugInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("stateStorageSize", stateStorage.size());
            response.put("jwtSecret", "configured");
            logger.info("Debug - State storage size: {}", stateStorage.size());
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
