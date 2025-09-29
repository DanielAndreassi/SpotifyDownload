package com.spotifyweb.repository;

import com.spotifyweb.entity.DownloadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadJobRepository extends JpaRepository<DownloadJob, Long> {

    Optional<DownloadJob> findByJobId(String jobId);

    List<DownloadJob> findAllByOrderByCreatedAtDesc();

    List<DownloadJob> findByStatusInOrderByCreatedAtDesc(List<DownloadJob.JobStatus> statuses);

    List<DownloadJob> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<DownloadJob> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<DownloadJob.JobStatus> statuses);

    Optional<DownloadJob> findByJobIdAndUserId(String jobId, Long userId);
}