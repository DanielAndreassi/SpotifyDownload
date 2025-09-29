import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Container, Spinner, Alert, Button, ListGroup, Image, Badge, ProgressBar } from 'react-bootstrap';
import api from '../services/api';
import AppNavbar from '../components/Navbar';
import { useDownloadQueue } from '../context/DownloadQueueContext';
import type { Track } from '../types/Track';

type DownloadStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

interface DownloadProgress {
    jobId: string;
    playlistName?: string;
    totalTracks?: number;
    completedTracks?: number;
    failedTracks?: number;
    status: DownloadStatus;
    progressPercentage?: number;
    errorMessage?: string;
    currentTrack?: string;
}

function formatStatusLabel(status: DownloadStatus) {
    switch (status) {
        case 'PENDING':
            return 'Aguardando início';
        case 'IN_PROGRESS':
            return 'Baixando';
        case 'COMPLETED':
            return 'Concluído';
        case 'FAILED':
            return 'Falhou';
        case 'CANCELLED':
            return 'Cancelado';
        default:
            return status;
    }
}

export default function PlaylistDetailPage() {
    const { id: playlistId } = useParams<{ id: string }>();
    const [tracks, setTracks] = useState<Track[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [downloading, setDownloading] = useState(false);
    const [jobId, setJobId] = useState<string | null>(null);
    const [progress, setProgress] = useState<DownloadProgress | null>(null);
    const { addToQueue, isInQueue, removeFromQueue } = useDownloadQueue();

    useEffect(() => {
        if (!playlistId) return;

        const fetchTracks = async () => {
            try {
                setLoading(true);
                const response = await api.get(`/playlists/${playlistId}`);
                const data = Array.isArray(response.data)
                    ? response.data
                    : Array.isArray(response.data?.items)
                        ? response.data.items
                        : [];
                setTracks(data);
            } catch (err) {
                setError('Não foi possível carregar as faixas da playlist.');
                console.error(err);
            } finally {
                setLoading(false);
                setMessage('');
            }
        };

        fetchTracks();
    }, [playlistId]);

    const handleDownloadPlaylist = async () => {
        if (!playlistId) return;
        try {
            setDownloading(true);
            setError('');
            setMessage('Iniciando download da playlist...');
            const response = await api.post('/downloads/start', { playlistId });
            setJobId(response.data.jobId);
            setMessage(response.data.message || 'Download da playlist iniciado.');
            setProgress(null);
        } catch (err: any) {
            console.error(err);
            const errorMessage = err.response?.data?.error || err.message || 'Não foi possível iniciar o download da playlist.';
            setError(errorMessage);
        } finally {
            setDownloading(false);
        }
    };

    useEffect(() => {
        if (!jobId) return;

        let active = true;
        let interval: ReturnType<typeof setInterval> | undefined;

        const fetchProgress = async () => {
            if (!active) return;
            try {
                const response = await api.get(`/downloads/status/${jobId}`);
                if (!active) return;
                const data: DownloadProgress = response.data;
                setProgress(data);

                if (data.status === 'COMPLETED') {
                    setMessage('Download concluído com sucesso!');
                    if (interval) clearInterval(interval);
                    active = false;
                } else if (data.status === 'FAILED') {
                    setError(data.errorMessage || 'O download da playlist terminou com erros.');
                    if (interval) clearInterval(interval);
                    active = false;
                }
            } catch (err: any) {
                if (!active) return;
                console.error('Falha ao consultar status do download', err);
                const errorMessage = err.response?.data?.error || err.message || 'Não foi possível consultar o status do download.';
                setError(errorMessage);
                if (interval) clearInterval(interval);
                active = false;
            }
        };

        fetchProgress().finally(() => {
            if (active) {
                interval = setInterval(fetchProgress, 3000);
            }
        });

        return () => {
            active = false;
            if (interval) {
                clearInterval(interval);
            }
        };
    }, [jobId]);

    return (
        <>
            <AppNavbar />
            <Container className="mt-4">
                <div className="d-flex justify-content-between align-items-center flex-wrap mb-4 gap-2">
                    <div>
                        <h1 className="mb-0">Playlist</h1>
                        <div className="text-muted small">
                            {tracks.length} {tracks.length === 1 ? 'música' : 'músicas'}
                        </div>
                    </div>
                    <Button variant="success" onClick={handleDownloadPlaylist} disabled={downloading}>
                        {downloading ? 'Iniciando...' : 'Baixar playlist'}
                    </Button>
                </div>

                {message && <Alert variant="info" className="mb-3">{message}</Alert>}

                {progress && (
                    <Alert variant="dark" className="mb-3">
                        <div className="d-flex flex-column gap-2">
                            <div className="d-flex justify-content-between align-items-center">
                                <span className="fw-semibold">Status: {formatStatusLabel(progress.status)}</span>
                                <span className="text-muted small">
                                    {progress.completedTracks ?? 0}/{progress.totalTracks ?? 0} músicas
                                </span>
                            </div>
                            <ProgressBar now={progress.progressPercentage ?? 0} variant="success" animated={progress.status === 'IN_PROGRESS'} />
                            {progress.currentTrack && (
                                <div className="text-muted">Baixando agora: {progress.currentTrack}</div>
                            )}
                            {progress.failedTracks ? (
                                <div className="text-danger small">Falhas: {progress.failedTracks}</div>
                            ) : null}
                        </div>
                    </Alert>
                )}

                {loading && <div className="text-center"><Spinner animation="border" variant="success" /></div>}
                {error && <Alert variant="danger">{error}</Alert>}
                {!loading && !error && (
                    <ListGroup variant="flush" className="bg-dark rounded">
                        {tracks.map((track, index) => {
                            const imageUrl = track.album?.images?.[0]?.url;
                            const artists = track.artists.map(a => a.name).join(', ');
                            const inQueue = isInQueue(track.id);
                            return (
                                <ListGroup.Item
                                    key={`${track.id}-${index}`}
                                    className="bg-dark text-white d-flex align-items-center gap-3 py-3"
                                >
                                    <span className="text-muted fw-semibold" style={{ width: '2rem' }}>{index + 1}</span>
                                    {imageUrl ? (
                                        <Image src={imageUrl} rounded width={56} height={56} alt={track.name} />
                                    ) : (
                                        <div style={{ width: 56, height: 56 }} className="bg-secondary rounded" />
                                    )}
                                    <div className="flex-grow-1">
                                        <div className="fw-semibold">{track.name}</div>
                                        <div className="text-muted small">{artists}</div>
                                    </div>
                                    {inQueue && <Badge bg="outline-light" text="light" className="me-2">Na fila</Badge>}
                                    <Button
                                        variant={inQueue ? 'outline-danger' : 'outline-success'}
                                        size="sm"
                                        onClick={() => (inQueue ? removeFromQueue(track.id) : addToQueue(track))}
                                    >
                                        {inQueue ? 'Remover da fila' : 'Adicionar à fila'}
                                    </Button>
                                </ListGroup.Item>
                            );
                        })}
                    </ListGroup>
                )}
            </Container>
        </>
    );
}
