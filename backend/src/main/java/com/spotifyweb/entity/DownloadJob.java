package com.spotifyweb.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "download_jobs")
public class DownloadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", unique = true, nullable = false)
    private String jobId;

    @Column(name = "playlist_name")
    private String playlistName;

    @Column(name = "playlist_id")
    private String playlistId;

    @Column(name = "total_tracks")
    private Integer totalTracks;

    @Column(name = "completed_tracks")
    private Integer completedTracks = 0;

    @Column(name = "failed_tracks")
    private Integer failedTracks = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "track_name")
    private String track;

    @Column(name = "artist_name")
    private String artist;

    @Column(name = "file_path")
    private String path;

    public enum JobStatus {
        PENDING, IN_PROGRESS, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    }

    // Constructors
    public DownloadJob() {}

    public DownloadJob(String jobId, String playlistName, String playlistId, Integer totalTracks, Long userId) {
        this.jobId = jobId;
        this.playlistName = playlistName;
        this.playlistId = playlistId;
        this.totalTracks = totalTracks;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    public Integer getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(Integer totalTracks) {
        this.totalTracks = totalTracks;
    }

    public Integer getCompletedTracks() {
        return completedTracks;
    }

    public void setCompletedTracks(Integer completedTracks) {
        this.completedTracks = completedTracks;
    }

    public Integer getFailedTracks() {
        return failedTracks;
    }

    public void setFailedTracks(Integer failedTracks) {
        this.failedTracks = failedTracks;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}