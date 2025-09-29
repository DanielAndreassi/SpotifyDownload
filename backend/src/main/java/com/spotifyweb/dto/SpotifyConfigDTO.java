package com.spotifyweb.dto;

import jakarta.validation.constraints.NotBlank;

public class SpotifyConfigDTO {

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Client Secret is required")
    private String clientSecret;

    public SpotifyConfigDTO() {}

    public SpotifyConfigDTO(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}