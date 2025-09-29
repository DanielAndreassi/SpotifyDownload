import { useEffect, useState } from 'react';
import { Toast, ToastContainer, ProgressBar } from 'react-bootstrap';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { DownloadProgress } from '../types/Download';

export default function DownloadProgressToasts() {
    const [downloads, setDownloads] = useState<Record<string, DownloadProgress>>({});

    useEffect(() => {
        // Only connect to WebSocket if we're in development or localhost
        const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

        if (!isLocal) {
            console.log('WebSocket disabled for non-local environment');
            return;
        }

        const client = new Client({
            webSocketFactory: () => new SockJS('/ws'),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('Connected to WebSocket');
                client.subscribe('/topic/download', (message) => {
                    const progress: DownloadProgress = JSON.parse(message.body);
                    setDownloads(prev => ({ ...prev, [progress.jobId]: progress }));
                });
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, []);

    const handleClose = (jobId: string) => {
        setDownloads(prev => {
            const newDownloads = { ...prev };
            delete newDownloads[jobId];
            return newDownloads;
        });
    };

    return (
        <ToastContainer position="bottom-end" className="p-3">
            {Object.values(downloads).map((d) => (
                <Toast key={d.jobId} onClose={() => handleClose(d.jobId)} bg="dark" autohide={d.status !== 'IN_PROGRESS'} delay={5000}>
                    <Toast.Header closeButton={d.status !== 'IN_PROGRESS'}>
                        <strong className="me-auto">{d.playlistName}</strong>
                        <small>{d.status}</small>
                    </Toast.Header>
                    <Toast.Body className="text-white">
                        {d.status === 'IN_PROGRESS' && <p>Downloading: {d.currentTrack}</p>}
                        {d.status === 'COMPLETED' && <p>Download finished!</p>}
                        {d.status === 'FAILED' && <p>Download failed.</p>}
                        <ProgressBar 
                            now={d.progress} 
                            label={`${Math.round(d.progress)}%`} 
                            variant={d.status === 'FAILED' ? 'danger' : 'success'} 
                            animated={d.status === 'IN_PROGRESS'} 
                        />
                        <small className="text-muted">
                            {d.completedTracks}/{d.totalTracks} tracks
                        </small>
                    </Toast.Body>
                </Toast>
            ))}
        </ToastContainer>
    );
}
