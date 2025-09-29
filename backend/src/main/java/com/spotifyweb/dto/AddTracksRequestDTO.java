package com.spotifyweb.dto;

public class AddTracksRequestDTO {
    private String[] trackUris;

    // Getters and Setters
    public String[] getTrackUris() { return trackUris; }
    public void setTrackUris(String[] trackUris) { this.trackUris = trackUris; }
}
