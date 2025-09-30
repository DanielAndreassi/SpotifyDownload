package com.spotifyweb.service;

import com.spotifyweb.dto.PlaylistDTO;
import com.spotifyweb.dto.AlbumDTO;
import com.spotifyweb.dto.AlbumDetailDTO;
import com.spotifyweb.entity.User;
import com.spotifyweb.repository.UserRepository;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpotifyService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);

    @Autowired
    private UserRepository userRepository;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @Value("${spotify.scopes}")
    private String scopes;

    /**
     * Cria um cliente SpotifyApi autenticado para o usuário usando um HttpManager dedicado,
     * evitando compartilhar a mesma conexão entre threads.
     */
    private SpotifyApi getApiForUser(User user) {
        if (user.getAccessToken() == null) {
            throw new IllegalStateException("User does not have an access token. Please re-authenticate.");
        }

        SpotifyHttpManager httpManager = new SpotifyHttpManager.Builder().build();

        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setHttpManager(httpManager)
                .setRedirectUri(URI.create(this.redirectUri))
                .setAccessToken(user.getAccessToken())
                .setRefreshToken(user.getRefreshToken())
                .build();
        return spotifyApi;
    }

    public String getAuthorizationUrl(String clientId, String clientSecret, String state) {
        // This method is used at the start of the auth flow, so it doesn't need a user-specific API.
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(this.redirectUri))
                .build();
        return spotifyApi.authorizationCodeUri().scope(scopes).state(state).build().execute().toString();
    }

    public List<PlaylistDTO> getUserPlaylists(User user) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        List<PlaylistDTO> playlists = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();

        fetchPlaylistsPage(spotifyApi.getListOfCurrentUsersPlaylists().limit(50).build().execute(), playlists, seenIds);

        int offset = 50;
        while (true) {
            Paging<PlaylistSimplified> page = spotifyApi.getListOfCurrentUsersPlaylists()
                    .limit(50)
                    .offset(offset)
                    .build()
                    .execute();
            if (page.getItems() == null || page.getItems().length == 0) {
                break;
            }
            fetchPlaylistsPage(page, playlists, seenIds);
            if (page.getNext() == null) {
                break;
            }
            offset += page.getLimit();
        }

        // fallback using explicit user id to ensure playlists seguidas appear
        int fallbackOffset = 0;
        while (true) {
            Paging<PlaylistSimplified> page = spotifyApi.getListOfUsersPlaylists(user.getSpotifyId())
                    .limit(50)
                    .offset(fallbackOffset)
                    .build()
                    .execute();
            if (page.getItems() == null || page.getItems().length == 0) {
                break;
            }
            fetchPlaylistsPage(page, playlists, seenIds);
            if (page.getNext() == null) {
                break;
            }
            fallbackOffset += page.getLimit();
        }
        return playlists;
    }

    private void fetchPlaylistsPage(Paging<PlaylistSimplified> page, List<PlaylistDTO> target, java.util.Set<String> seenIds) {
        if (page == null || page.getItems() == null) {
            return;
        }
        for (PlaylistSimplified playlist : page.getItems()) {
            if (playlist == null || playlist.getId() == null || !seenIds.add(playlist.getId())) {
                continue;
            }
            target.add(new PlaylistDTO(
                    playlist.getId(),
                    playlist.getName(),
                    null,
                    playlist.getTracks() != null ? playlist.getTracks().getTotal() : 0,
                    playlist.getImages() != null && playlist.getImages().length > 0 ? playlist.getImages()[0].getUrl() : null,
                    playlist.getOwner() != null ? playlist.getOwner().getDisplayName() : ""
            ));
        }
    }

    public List<Track> getPlaylistTracks(User user, String playlistId) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        List<Track> tracks = new ArrayList<>();
        Paging<PlaylistTrack> playlistTracks = spotifyApi.getPlaylistsItems(playlistId).build().execute();
        for (PlaylistTrack playlistTrack : playlistTracks.getItems()) {
            if (playlistTrack.getTrack() instanceof Track) tracks.add((Track) playlistTrack.getTrack());
        }
        while (playlistTracks.getNext() != null) {
            playlistTracks = spotifyApi.getPlaylistsItems(playlistId).offset(playlistTracks.getOffset() + playlistTracks.getLimit()).build().execute();
            for (PlaylistTrack playlistTrack : playlistTracks.getItems()) {
                if (playlistTrack.getTrack() instanceof Track) tracks.add((Track) playlistTrack.getTrack());
            }
        }
        return tracks;
    }

    public String getPlaylistName(User user, String playlistId) {
        try {
            Playlist playlist = getApiForUser(user).getPlaylist(playlistId).build().execute();
            return playlist.getName();
        } catch (Exception e) {
            logger.warn("Could not fetch playlist name for {}: {}", playlistId, e.getMessage());
            return playlistId;
        }
    }

    public List<Track> getLikedTracks(User user) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        List<Track> tracks = new ArrayList<>();
        Paging<SavedTrack> savedTracks = spotifyApi.getUsersSavedTracks().build().execute();
        for (SavedTrack savedTrack : savedTracks.getItems()) {
            tracks.add(savedTrack.getTrack());
        }
        while (savedTracks.getNext() != null) {
            savedTracks = spotifyApi.getUsersSavedTracks().offset(savedTracks.getOffset() + savedTracks.getLimit()).build().execute();
            for (SavedTrack savedTrack : savedTracks.getItems()) {
                tracks.add(savedTrack.getTrack());
            }
        }
        return tracks;
    }

    public Album getAlbum(User user, String albumId) throws IOException, SpotifyWebApiException, ParseException {
        return getApiForUser(user).getAlbum(albumId).build().execute();
    }

    public List<Track> getAlbumTracks(User user, String albumId) throws IOException, SpotifyWebApiException, ParseException {
        Album album = getAlbum(user, albumId);
        return getAlbumTracks(user, album);
    }

    public List<Track> getAlbumTracks(User user, Album album) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        List<Track> tracks = new ArrayList<>();
        Paging<TrackSimplified> page = spotifyApi.getAlbumsTracks(album.getId()).limit(50).build().execute();
        appendAlbumTracks(album, tracks, page);
        while (page.getNext() != null) {
            page = spotifyApi.getAlbumsTracks(album.getId())
                    .limit(50)
                    .offset(page.getOffset() + page.getLimit())
                    .build()
                    .execute();
            appendAlbumTracks(album, tracks, page);
        }
        return tracks;
    }

    public List<Album> getUserAlbums(User user) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        List<Album> albums = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();

        Paging<SavedAlbum> page = spotifyApi.getCurrentUsersSavedAlbums().limit(50).build().execute();
        appendSavedAlbums(albums, seenIds, page);

        while (page.getNext() != null) {
            page = spotifyApi.getCurrentUsersSavedAlbums()
                    .limit(50)
                    .offset(page.getOffset() + page.getLimit())
                    .build()
                    .execute();
            if (page.getItems() == null || page.getItems().length == 0) {
                break;
            }
            appendSavedAlbums(albums, seenIds, page);
        }
        return albums;
    }

    public AlbumDetailDTO getAlbumDetail(User user, String albumId) throws IOException, SpotifyWebApiException, ParseException {
        Album album = getAlbum(user, albumId);
        List<Track> tracks = getAlbumTracks(user, album);

        AlbumDTO albumDTO = new AlbumDTO(
                album.getId(),
                album.getName() != null ? album.getName() : "Álbum sem nome",
                album.getArtists() != null ? Arrays.stream(album.getArtists()).map(ArtistSimplified::getName).collect(Collectors.toList()) : List.of("Desconhecido"),
                album.getTracks() != null ? album.getTracks().getTotal() : tracks.size(),
                album.getImages() != null && album.getImages().length > 0 ? album.getImages()[0].getUrl() : null,
                album.getReleaseDate()
        );

        List<AlbumDetailDTO.TrackDTO> trackDTOs = tracks.stream()
                .map(t -> new AlbumDetailDTO.TrackDTO(
                        t.getId(),
                        t.getName(),
                        t.getDurationMs(),
                        Arrays.stream(t.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", "))
                ))
                .collect(Collectors.toList());

        return new AlbumDetailDTO(albumDTO, trackDTOs);
    }

    public List<Track> getTopTracks(User user, int limit) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        Paging<Track> topTracks = spotifyApi.getUsersTopTracks().limit(Math.min(limit, 50)).build().execute();
        return Arrays.asList(topTracks.getItems());
    }

    public Paging<PlaylistSimplified> searchPlaylists(User user, String query) throws IOException, SpotifyWebApiException, ParseException {
        return getApiForUser(user).searchPlaylists(query).limit(20).build().execute();
    }

    public Playlist createPlaylist(User user, String name, String description) throws IOException, SpotifyWebApiException, ParseException {
        SpotifyApi spotifyApi = getApiForUser(user);
        return spotifyApi.createPlaylist(user.getSpotifyId(), name).description(description).public_(false).build().execute();
    }

    public void addTracksToPlaylist(User user, String playlistId, String[] trackUris) throws IOException, SpotifyWebApiException, ParseException {
        getApiForUser(user).addItemsToPlaylist(playlistId, trackUris).build().execute();
    }

    public Artist getArtist(User user, String artistId) throws IOException, SpotifyWebApiException, ParseException {
        return getApiForUser(user).getArtist(artistId).build().execute();
    }

    private void appendAlbumTracks(Album album, List<Track> target, Paging<TrackSimplified> page) {
        if (page == null || page.getItems() == null) {
            return;
        }
        for (TrackSimplified simplified : page.getItems()) {
            if (simplified == null) continue;
            Track track = new Track.Builder()
                    .setId(simplified.getId())
                    .setName(simplified.getName())
                    .setArtists(simplified.getArtists())
                    .setDurationMs(simplified.getDurationMs())
                    .setAlbum(new AlbumSimplified.Builder()
                            .setId(album.getId())
                            .setName(album.getName())
                            .setImages(album.getImages())
                            .build())
                    .build();
            target.add(track);
        }
    }

    private void appendSavedAlbums(List<Album> target, java.util.Set<String> seenIds, Paging<SavedAlbum> page) {
        if (page == null || page.getItems() == null) {
            return;
        }
        for (SavedAlbum savedAlbum : page.getItems()) {
            if (savedAlbum == null || savedAlbum.getAlbum() == null) continue;
            Album album = savedAlbum.getAlbum();
            if (album.getId() != null && seenIds.add(album.getId())) {
                target.add(album);
            }
        }
    }
}
