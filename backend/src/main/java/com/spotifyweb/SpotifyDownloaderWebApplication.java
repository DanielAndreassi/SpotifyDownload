package com.spotifyweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpotifyDownloaderWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpotifyDownloaderWebApplication.class, args);
    }
}