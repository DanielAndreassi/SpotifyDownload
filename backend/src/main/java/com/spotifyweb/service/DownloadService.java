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
        String playlistId = request.getUrl().substring(request.getUrl().lastIndexOf('/') + 1);
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
        String albumId = request.getUrl().substring(request.getUrl().lastIndexOf('/') + 1);
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
            String youtubeUrl = searchYouTube(artistName + " " + trackName + " audio");
            downloadFromYouTube(youtubeUrl, finalPath);
            job.setCompletedTracks(increment(job.getCompletedTracks()));
            downloadJobRepository.save(job);
        } catch (Exception e) {
            logger.error("Falha ao baixar faixa {}", trackName, e);
            job.setFailedTracks(increment(job.getFailedTracks()));
            job.setErrorMessage(e.getMessage());
            downloadJobRepository.save(job);
        }
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

    private String searchYouTube(String query) throws IOException {
        logger.warn("Busca no YouTube é placeholder. Usando URL padrão.");
        return "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    }

    private void downloadFromYouTube(String youtubeUrl, String outputPath) throws IOException, InterruptedException {
        logger.info("Baixando do YouTube: {}", youtubeUrl);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "-x",
                "--audio-format", "mp3",
                "-o", outputPath,
                youtubeUrl
        );
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp finalizou com código " + exitCode);
        }
        logger.info("Arquivo salvo em: {}", outputPath);
    }
}
