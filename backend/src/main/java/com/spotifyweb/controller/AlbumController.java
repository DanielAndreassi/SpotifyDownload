package com.spotifyweb.controller;

import com.spotifyweb.dto.AlbumDTO;
import com.spotifyweb.entity.User;
import com.spotifyweb.service.SpotifyService;
import com.spotifyweb.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/albums")
public class AlbumController {

    private static final Logger logger = LoggerFactory.getLogger(AlbumController.class);

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private UserService userService;

    private User getUserFromAuth(Authentication authentication) {
        String spotifyId = authentication.getName();
        return userService.findBySpotifyId(spotifyId)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado: " + spotifyId));
    }

    @GetMapping
    public ResponseEntity<?> getSavedAlbums(Authentication authentication) {
        logger.info("getSavedAlbums chamado para usuário {}", authentication.getName());
        try {
            User user = getUserFromAuth(authentication);
            List<AlbumDTO> albums = spotifyService.getUserAlbums(user).stream()
                    .map(album -> new AlbumDTO(
                            album.getId(),
                            album.getName() != null ? album.getName() : "Álbum sem nome",
                            album.getArtists() != null ?
                                    java.util.Arrays.stream(album.getArtists()).map(artist -> artist.getName()).collect(Collectors.toList()) :
                                    java.util.List.of("Desconhecido"),
                            album.getTracks() != null ? album.getTracks().getTotal() : 0,
                            album.getImages() != null && album.getImages().length > 0 ? album.getImages()[0].getUrl() : null,
                            album.getReleaseDate()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(albums);
        } catch (IllegalStateException e) {
            logger.error("Erro de autenticação em getSavedAlbums", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Falha ao buscar álbuns salvos", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Não foi possível carregar álbuns: " + e.getMessage()));
        }
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<?> getAlbumTracks(@PathVariable String albumId, Authentication authentication) {
        logger.info("getAlbumTracks chamado para usuário {} e álbum {}", authentication.getName(), albumId);
        try {
            User user = getUserFromAuth(authentication);
            return ResponseEntity.ok(spotifyService.getAlbumTracks(user, albumId));
        } catch (IllegalStateException e) {
            logger.error("Erro de autenticação em getAlbumTracks", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Falha ao buscar faixas do álbum {}", albumId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Não foi possível carregar faixas do álbum: " + e.getMessage()));
        }
    }
}
