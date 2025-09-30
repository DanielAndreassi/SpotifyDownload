package com.spotifyweb.service;

import com.spotifyweb.dto.DownloadRequestDTO;
import com.spotifyweb.dto.DownloadProgressDTO;
import com.spotifyweb.entity.DownloadJob;
import com.spotifyweb.entity.User;
import com.spotifyweb.repository.DownloadJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DownloadService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private DownloadJobRepository downloadJobRepository;

    @Value("${download.base-path}")
    private String downloadPath;

    public String downloadPlaylist(DownloadRequestDTO request, User user) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL é obrigatório");
        }
        String playlistId = extractSpotifyId(request.getUrl());
        return downloadPlaylistById(playlistId, user);
    }

    public String downloadPlaylistById(String playlistId, User user) {
        try {
            List<Track> tracks = spotifyService.getPlaylistTracks(user, playlistId);
            String playlistName = spotifyService.getPlaylistName(user, playlistId);

            String jobId = createJob(playlistId, playlistName, tracks.size(), user.getId());
            executor.submit(() -> executeTracksDownload(jobId, playlistId, playlistName, tracks));
            return jobId;
        } catch (Exception e) {
            logger.error("Erro ao agendar download da playlist {}", playlistId, e);
            throw new RuntimeException("Falha ao iniciar download da playlist", e);
        }
    }

    public String downloadAlbum(DownloadRequestDTO request, User user) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL é obrigatório");
        }
        String albumId = extractSpotifyId(request.getUrl());
        return downloadAlbumById(albumId, user);
    }

    public String downloadAlbumById(String albumId, User user) {
        try {
            Album album = spotifyService.getAlbum(user, albumId);
            List<Track> tracks = spotifyService.getAlbumTracks(user, album);

            String jobId = createJob(albumId, album.getName(), tracks.size(), user.getId());
            executor.submit(() -> executeTracksDownload(jobId, albumId, album.getName(), tracks));
            return jobId;
        } catch (Exception e) {
            logger.error("Erro ao agendar download do álbum {}", albumId, e);
            throw new RuntimeException("Falha ao iniciar download do álbum", e);
        }
    }

    public Optional<DownloadProgressDTO> getDownloadProgress(String jobId, Long userId) {
        return downloadJobRepository.findByJobIdAndUserId(jobId, userId)
                .map(DownloadProgressDTO::new);
    }

    private String createJob(String targetId, String targetName, int totalTracks, Long userId) {
        String jobId = UUID.randomUUID().toString();
        DownloadJob job = new DownloadJob(jobId, targetName, targetId, totalTracks, userId);
        job.setStatus(DownloadJob.JobStatus.PENDING);
        job.setCompletedTracks(0);
        job.setFailedTracks(0);
        downloadJobRepository.save(job);
        return jobId;
    }

    private void executeTracksDownload(String jobId, String targetId, String targetName, List<Track> tracks) {
        DownloadJob job = downloadJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("Download job não encontrado: " + jobId));

        job.setStatus(DownloadJob.JobStatus.IN_PROGRESS);
        downloadJobRepository.save(job);

        String folderName = sanitizeFileName(targetName != null ? targetName : targetId);
        String baseDirPath = Paths.get(downloadPath, folderName).toString();
        File baseDir = new File(baseDirPath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            logger.warn("Não foi possível criar diretório {}", baseDirPath);
        }

        try {
            for (Track track : tracks) {
                processTrack(job, track, baseDirPath);
            }

            job.setCompletedAt(LocalDateTime.now());
            if (job.getFailedTracks() != null && job.getFailedTracks() > 0) {
                job.setStatus(DownloadJob.JobStatus.FAILED);
            } else {
                job.setStatus(DownloadJob.JobStatus.COMPLETED);
            }
            job.setTrack(null);
            downloadJobRepository.save(job);
            logger.info("Finalizado download para {}", targetId);
        } catch (Exception e) {
            logger.error("Erro inesperado ao baixar {}", targetId, e);
            job.setStatus(DownloadJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            downloadJobRepository.save(job);
        }
    }

    private void processTrack(DownloadJob job, Track track, String baseDirPath) {
        String artistName = track.getArtists()[0].getName();
        String trackName = track.getName();
        String sanitizedTrackName = trackName.replaceAll("[\\/:*?\"<>|]", "");
        String finalPath = Paths.get(baseDirPath, sanitizedTrackName + ".mp3").toString();
        File finalFile = new File(finalPath);

        job.setArtist(artistName);
        job.setTrack(trackName);
        job.setPath(finalPath);
        job.setErrorMessage(null);
        downloadJobRepository.save(job);

        if (finalFile.exists()) {
            logger.info("Arquivo já existe: {}", finalPath);
            job.setCompletedTracks(increment(job.getCompletedTracks()));
            downloadJobRepository.save(job);
            return;
        }

        try {
            String searchQuery = buildYoutubeSearchQuery(artistName, trackName, false);
            downloadFromYouTube(searchQuery, finalPath);
            job.setCompletedTracks(increment(job.getCompletedTracks()));
            downloadJobRepository.save(job);
        } catch (YtDlpException primaryError) {
            if (shouldRetryWithoutLive(primaryError, trackName)) {
                String fallbackTrackName = removeLiveKeywords(trackName);
                String fallbackQuery = buildYoutubeSearchQuery(artistName, fallbackTrackName, false);
                logger.info("Tentando novamente sem marcadores ao vivo: {}", fallbackQuery);
                try {
                    downloadFromYouTube(fallbackQuery, finalPath);
                    job.setCompletedTracks(increment(job.getCompletedTracks()));
                    downloadJobRepository.save(job);
                    return;
                } catch (YtDlpException fallbackError) {
                    registerFailure(job, trackName, fallbackError);
                } catch (Exception fallbackError) {
                    registerFailure(job, trackName, fallbackError);
                }
            } else {
                registerFailure(job, trackName, primaryError);
            }
        } catch (Exception e) {
            registerFailure(job, trackName, e);
        }
    }

    private void registerFailure(DownloadJob job, String trackName, Exception error) {
        logger.error("Falha ao baixar faixa {}", trackName, error);
        job.setFailedTracks(increment(job.getFailedTracks()));
        job.setErrorMessage(truncateErrorMessage(error.getMessage()));
        downloadJobRepository.save(job);
    }

    private int increment(Integer value) {
        return value == null ? 1 : value + 1;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "download";
        }
        return name.replaceAll("[\\/:*?\"<>|]", "_");
    }

    private String extractSpotifyId(String reference) {
        if (reference == null) {
            return null;
        }
        int index = reference.lastIndexOf('/');
        String id = index >= 0 ? reference.substring(index + 1) : reference;
        int queryIndex = id.indexOf('?');
        if (queryIndex >= 0) {
            id = id.substring(0, queryIndex);
        }
        return id;
    }

    private String buildYoutubeSearchQuery(String artistName, String trackName, boolean restrictToOfficialAudio) {
        StringBuilder builder = new StringBuilder("ytsearch1:");
        if (artistName != null && !artistName.isBlank()) {
            builder.append('"').append(artistName.trim()).append('"');
        }
        if (trackName != null && !trackName.isBlank()) {
            if (builder.charAt(builder.length() - 1) != ':') {
                builder.append(' ');
            }
            builder.append('"').append(trackName.trim()).append('"');
        }
        if (restrictToOfficialAudio) {
            builder.append(" áudio oficial");
        }
        return builder.toString();
    }

    private void downloadFromYouTube(String youtubeUrl, String outputPath) throws IOException, InterruptedException {
        logger.info("Baixando do YouTube: {}", youtubeUrl);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--audio-format", "mp3",
                "--match-filter", "!is_short",
                "-o", outputPath,
                youtubeUrl
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("yt-dlp falhou ({}): {}", exitCode, output);
            throw new YtDlpException(exitCode, output);
        }
        if (!output.isBlank()) {
            logger.debug("yt-dlp saída: {}", output);
        }
        logger.info("Arquivo salvo em: {}", outputPath);
    }

    private String readProcessOutput(Process process) throws IOException {
        try (var inputStream = process.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        }
    }

    private boolean shouldRetryWithoutLive(YtDlpException error, String trackName) {
        if (error.getExitCode() != 1 || trackName == null) {
            return false;
        }
        String normalized = trackName.toLowerCase();
        if (normalized.contains("ao vivo") || normalized.contains("live")) {
            String cleaned = removeLiveKeywords(trackName);
            return cleaned != null && !cleaned.isBlank() && !cleaned.equals(trackName);
        }
        return false;
    }

    private String removeLiveKeywords(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.replaceAll("(?i)\\s*[-–—]?\\s*(ao vivo|live)", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? name : cleaned;
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 255) {
            return message;
        }
        return message.substring(0, 252) + "...";
    }

    private static class YtDlpException extends IOException {
        private final int exitCode;
        private final String output;

        private YtDlpException(int exitCode, String output) {
            super(buildMessage(exitCode, output));
            this.exitCode = exitCode;
            this.output = output;
        }

        int getExitCode() {
            return exitCode;
        }

        String getOutput() {
            return output;
        }

        private static String buildMessage(int exitCode, String output) {
            if (output == null || output.isBlank()) {
                return "yt-dlp finalizou com código " + exitCode;
            }
            return "yt-dlp finalizou com código " + exitCode + ": " + output;
        }
    }
}
