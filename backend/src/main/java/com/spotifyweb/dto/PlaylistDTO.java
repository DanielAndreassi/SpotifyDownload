package com.spotifyweb.dto;

public class PlaylistDTO {
    private String id;
    private String name;
    private String description;
    private Integer totalTracks;
    private String imageUrl;
    private String owner;

    public PlaylistDTO() {}

    public PlaylistDTO(String id, String name, String description, Integer totalTracks, String imageUrl, String owner) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.totalTracks = totalTracks;
        this.imageUrl = imageUrl;
        this.owner = owner;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}