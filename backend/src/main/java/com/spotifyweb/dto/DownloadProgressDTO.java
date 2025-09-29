package com.spotifyweb.dto;

import com.spotifyweb.entity.DownloadJob;

public class DownloadProgressDTO {
    private String jobId;
    private String playlistName;
    private Integer totalTracks;
    private Integer completedTracks;
    private Integer failedTracks;
    private DownloadJob.JobStatus status;
    private String currentTrack;
    private Double progressPercentage;
    private String errorMessage;

    public DownloadProgressDTO() {}

    public DownloadProgressDTO(DownloadJob job) {
        this.jobId = job.getJobId();
        this.playlistName = job.getPlaylistName();
        this.totalTracks = job.getTotalTracks();
        this.completedTracks = job.getCompletedTracks();
        this.failedTracks = job.getFailedTracks();
        this.status = job.getStatus();
        this.errorMessage = job.getErrorMessage();
        this.currentTrack = job.getTrack();

        int safeCompleted = this.completedTracks == null ? 0 : this.completedTracks;
        int safeTotal = this.totalTracks == null ? 0 : this.totalTracks;

        if (safeTotal > 0) {
            this.progressPercentage = (double) safeCompleted / safeTotal * 100;
        } else {
            this.progressPercentage = 0.0;
        }
    }

    // Getters and Setters
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

    public DownloadJob.JobStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadJob.JobStatus status) {
        this.status = status;
    }

    public String getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(String currentTrack) {
        this.currentTrack = currentTrack;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
