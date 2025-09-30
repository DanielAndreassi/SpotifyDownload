export interface DownloadProgress {
    jobId: string;
    playlistName: string;
    totalTracks: number;
    completedTracks: number;
    failedTracks: number;
    status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
    currentTrack: string;
    progressPercentage?: number;
    errorMessage?: string;
}
