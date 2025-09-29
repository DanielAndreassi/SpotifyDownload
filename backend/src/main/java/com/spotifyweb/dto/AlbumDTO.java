package com.spotifyweb.dto;

import java.util.List;

public class AlbumDTO {
    private String id;
    private String name;
    private List<String> artists;
    private Integer totalTracks;
    private String imageUrl;
    private String releaseDate;

    public AlbumDTO() {}

    public AlbumDTO(String id, String name, List<String> artists, Integer totalTracks, String imageUrl, String releaseDate) {
        this.id = id;
        this.name = name;
        this.artists = artists;
        this.totalTracks = totalTracks;
        this.imageUrl = imageUrl;
        this.releaseDate = releaseDate;
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

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists;
    }

    public Integer getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(Integer totalTracks) {
        this.totalTracks = totalTracks;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}
