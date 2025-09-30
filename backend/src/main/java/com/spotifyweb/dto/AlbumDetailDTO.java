package com.spotifyweb.dto;

import java.util.List;

public class AlbumDetailDTO {
    private AlbumDTO album;
    private List<TrackDTO> tracks;

    public AlbumDetailDTO() {}

    public AlbumDetailDTO(AlbumDTO album, List<TrackDTO> tracks) {
        this.album = album;
        this.tracks = tracks;
    }

    public AlbumDTO getAlbum() {
        return album;
    }

    public void setAlbum(AlbumDTO album) {
        this.album = album;
    }

    public List<TrackDTO> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackDTO> tracks) {
        this.tracks = tracks;
    }

    public static class TrackDTO {
        private String id;
        private String name;
        private long durationMs;
        private String artists;

        public TrackDTO() {}

        public TrackDTO(String id, String name, long durationMs, String artists) {
            this.id = id;
            this.name = name;
            this.durationMs = durationMs;
            this.artists = artists;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public String getArtists() {
            return artists;
        }

        public void setArtists(String artists) {
            this.artists = artists;
        }
    }
}
