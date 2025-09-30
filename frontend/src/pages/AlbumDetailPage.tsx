import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Container, Spinner, Alert, Button, ListGroup, Badge, ProgressBar } from 'react-bootstrap';
import AppNavbar from '../components/Navbar';
import api from '../services/api';

interface AlbumDetailDTO {
    album: {
        id: string;
        name: string;
        artists: string[];
        totalTracks: number;
        imageUrl?: string;
        releaseDate?: string;
    };
    tracks: Array<{
        id: string;
        name: string;
        durationMs: number;
        artists: string;
    }>;
}

type DownloadStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

interface DownloadProgress {
    jobId: string;
    playlistName: string;
    totalTracks: number;
    completedTracks: number;
    failedTracks: number;
    status: DownloadStatus;
    currentTrack?: string;
    progressPercentage?: number;
    errorMessage?: string;
}

function formatDuration(ms: number) {
    const minutes = Math.floor(ms / 60000);
    const seconds = ((ms % 60000) / 1000).toFixed(0);
    return `${minutes}:${parseInt(seconds) < 10 ? '0' : ''}${seconds}`;
}

export default function AlbumDetailPage() {
    const { id: albumId } = useParams<{ id: string }>();
    const [albumDetail, setAlbumDetail] = useState<AlbumDetailDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [downloadMessage, setDownloadMessage] = useState('');
    const [downloadError, setDownloadError] = useState('');
    const [jobId, setJobId] = useState<string | null>(null);
    const [progress, setProgress] = useState<DownloadProgress | null>(null);

    useEffect(() => {
        if (!albumId) return;

        const fetchAlbum = async () => {
            try {
                setLoading(true);
                setError('');
                const response = await api.get(`/albums/${albumId}`);
                setAlbumDetail(response.data);
            } catch (err: any) {
                console.error('Erro ao carregar álbum', err);
                const errorMessage = err.response?.data?.error || err.message || 'Não foi possível carregar este álbum.';
                setError(errorMessage);
            } finally {
                setLoading(false);
            }
        };

        fetchAlbum();
    }, [albumId]);

    useEffect(() => {
        if (!jobId) return;

        let active = true;
        let interval: ReturnType<typeof setInterval> | undefined;

        const fetchProgress = async () => {
            if (!active) return;
            try {
                const response = await api.get(`/downloads/status/${jobId}`);
                if (!active) return;
                setProgress(response.data);
                if (response.data.status === 'COMPLETED') {
                    setDownloadMessage('Download do álbum concluído com sucesso!');
                    if (interval) clearInterval(interval);
                    active = false;
                } else if (response.data.status === 'FAILED') {
                    setDownloadError(response.data.errorMessage || 'O download do álbum terminou com falhas.');
                    if (interval) clearInterval(interval);
                    active = false;
                }
            } catch (err: any) {
                if (!active) return;
                const errorMessage = err.response?.data?.error || err.message || 'Não foi possível consultar o status do download.';
                setDownloadError(errorMessage);
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

    const handleDownloadAlbum = async () => {
        if (!albumId) return;
        try {
            setDownloadMessage('Iniciando download do álbum...');
            setDownloadError('');
            const response = await api.post('/download/album', { url: albumId });
            setDownloadMessage(response.data.message || 'Download do álbum iniciado.');
            setJobId(response.data.jobId);
            setProgress(null);
        } catch (err: any) {
            console.error('Erro ao iniciar download do álbum', err);
            const errorMessage = err.response?.data?.error || err.message || 'Não foi possível iniciar o download do álbum.';
            setDownloadError(errorMessage);
        }
    };

    return (
        <>
            <AppNavbar />
            <Container className="mt-4">
                {loading && <div className="text-center"><Spinner animation="border" variant="success" /></div>}
                {error && <Alert variant="danger">{error}</Alert>}
                {!loading && !error && albumDetail && (
                    <>
                        <div className="d-flex flex-column flex-md-row gap-4 mb-4 align-items-start">
                            <img
                                src={albumDetail.album.imageUrl || 'https://via.placeholder.com/240x240?text=Álbum'}
                                alt={albumDetail.album.name}
                                width={240}
                                height={240}
                                style={{ objectFit: 'cover', borderRadius: '8px' }}
                            />
                            <div className="flex-grow-1">
                                <h1 className="text-white">{albumDetail.album.name}</h1>
                                <p className="text-muted mb-1">{albumDetail.album.artists.join(', ')}</p>
                                {albumDetail.album.releaseDate && (
                                    <p className="text-muted mb-1">Lançamento: {albumDetail.album.releaseDate}</p>
                                )}
                                <p className="text-muted">{albumDetail.album.totalTracks} músicas</p>
                                <Button variant="success" onClick={handleDownloadAlbum}>Baixar álbum</Button>
                            </div>
                        </div>

                        {downloadMessage && <Alert variant="info">{downloadMessage}</Alert>}
                        {downloadError && <Alert variant="danger">{downloadError}</Alert>}
                        {progress && (
                            <Alert variant="dark">
                                <div className="d-flex flex-column gap-2">
                                    <div className="d-flex justify-content-between">
                                        <span>Status: {progress.status}</span>
                                        <span className="text-muted small">{progress.completedTracks}/{progress.totalTracks} músicas</span>
                                    </div>
                                    <ProgressBar now={progress.progressPercentage ?? 0} variant="success" animated={progress.status === 'IN_PROGRESS'} />
                                    {progress.currentTrack && <span className="text-muted">Baixando: {progress.currentTrack}</span>}
                                </div>
                            </Alert>
                        )}

                        <ListGroup variant="flush" className="bg-dark rounded">
                            {albumDetail.tracks.map((track, index) => (
                                <ListGroup.Item key={track.id || `${index}`} className="bg-dark text-white d-flex justify-content-between align-items-center">
                                    <div>
                                        <div className="fw-semibold">{index + 1}. {track.name}</div>
                                        <div className="text-muted small">{track.artists}</div>
                                    </div>
                                    <Badge bg="secondary">{formatDuration(track.durationMs)}</Badge>
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                    </>
                )}
            </Container>
        </>
    );
}
