package com.spotifyweb.dto;

import jakarta.validation.constraints.NotBlank;

public class DownloadRequestDTO {

    @NotBlank(message = "URL is required")
    private String url;

    private String quality = "320"; // Default quality

    public DownloadRequestDTO() {}

    public DownloadRequestDTO(String url, String quality) {
        this.url = url;
        this.quality = quality;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }
}
