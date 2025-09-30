import { useEffect, useMemo, useState } from 'react';
import { Container, Row, Col, Card, Spinner, Alert, Nav, Form, Button, Badge, ProgressBar } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import AppNavbar from '../components/Navbar';
import type { Album } from '../types/Album';
import type { DownloadProgress } from '../types/Download';

interface Playlist {
    id: string;
    name?: string;
    totalTracks?: number;
    trackCount?: number;
    imageUrl?: string;
    owner?: string;
}

type ViewType = 'playlists' | 'albums' | 'liked' | 'top';

type DownloadState = {
    message: string;
    type: 'info' | 'error' | null;
};

const TEMP_PREFIX = 'SpotifyDownloader - ';

const normalizePlaylist = (item: any, fallbackOwner: string): Playlist => ({
    id: item.id,
    name: item.name || 'Playlist sem nome',
    totalTracks: item.totalTracks ?? item.trackCount ?? item.tracksTotal ?? item.tracks?.total ?? 0,
    trackCount: item.totalTracks ?? item.trackCount ?? item.tracksTotal ?? item.tracks?.total ?? 0,
    imageUrl: item.imageUrl ?? (item.images?.length ? item.images[0].url : undefined),
    owner: item.owner || fallbackOwner,
});

const normalizeAlbum = (item: Album): Album => ({
    ...item,
    name: item.name || 'Álbum sem nome',
    artists: item.artists?.length ? item.artists : ['Desconhecido'],
    totalTracks: item.totalTracks ?? 0,
});

export default function DashboardPage() {
    const { isLoading: authLoading } = useAuth();

    const [activeView, setActiveView] = useState<ViewType>('playlists');
    const [playlists, setPlaylists] = useState<Playlist[]>([]);
    const [albums, setAlbums] = useState<Album[]>([]);
    const [liked, setLiked] = useState<Playlist | null>(null);
    const [top, setTop] = useState<Playlist | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [filter, setFilter] = useState('');
    const [downloadState, setDownloadState] = useState<DownloadState>({ message: '', type: null });
    const [albumDownloadJob, setAlbumDownloadJob] = useState<{ jobId: string; name: string } | null>(null);
    const [albumProgress, setAlbumProgress] = useState<DownloadProgress | null>(null);

    useEffect(() => {
        if (authLoading) return;

        const loadInitial = async () => {
            try {
                setLoading(true);
                setError('');
                const [playlistResponse, albumResponse] = await Promise.all([
                    api.get('/playlists'),
                    api.get('/albums'),
                ]);

                const playlistData: Playlist[] = (Array.isArray(playlistResponse.data) ? playlistResponse.data : [playlistResponse.data])
                    .filter((item) => item)
                    .filter((item) => !(item.name || '').startsWith(TEMP_PREFIX))
                    .map((item) => normalizePlaylist(item, 'Desconhecido'));

                const albumData: Album[] = (albumResponse.data || [])
                    .filter((item: Album | null) => item !== null)
                    .map((item: Album) => normalizeAlbum(item));

                setPlaylists(playlistData);
                setAlbums(albumData);
            } catch (err: any) {
                console.error('Erro ao carregar dados iniciais', err);
                const msg = err.response?.data?.error || err.message || 'Erro desconhecido';
                setError(`Não foi possível carregar suas playlists e álbuns: ${msg}`);
            } finally {
                setLoading(false);
            }
        };

        loadInitial();
    }, [authLoading]);

    useEffect(() => {
        const loadLiked = async () => {
            try {
                setLoading(true);
                setError('');
                const response = await api.get('/playlists/liked-tracks');
                setLiked(normalizePlaylist(response.data, 'Você'));
            } catch (err: any) {
                console.error('Erro ao carregar músicas curtidas', err);
                const msg = err.response?.data?.error || err.message || 'Erro desconhecido';
                setError(`Não foi possível carregar suas músicas curtidas: ${msg}`);
            } finally {
                setLoading(false);
            }
        };

        const loadTop = async () => {
            try {
                setLoading(true);
                setError('');
                const response = await api.get('/playlists/top-tracks');
                setTop(normalizePlaylist(response.data, 'Você'));
            } catch (err: any) {
                console.error('Erro ao carregar top músicas', err);
                const msg = err.response?.data?.error || err.message || 'Erro desconhecido';
                setError(`Não foi possível carregar suas top músicas: ${msg}`);
            } finally {
                setLoading(false);
            }
        };

        if (activeView === 'liked' && !liked) {
            loadLiked();
        } else if (activeView === 'top' && !top) {
            loadTop();
        }
    }, [activeView, liked, top]);

    const filteredPlaylists = useMemo(() => {
        const term = filter.trim().toLowerCase();
        return playlists.filter((playlist) =>
            term === '' || (playlist.name ?? '').toLowerCase().includes(term)
        );
    }, [playlists, filter]);

    const filteredAlbums = useMemo(() => {
        const term = filter.trim().toLowerCase();
        return albums.filter((album) =>
            term === '' || album.name.toLowerCase().includes(term) || album.artists.join(', ').toLowerCase().includes(term)
        );
    }, [albums, filter]);

    useEffect(() => {
        if (!albumDownloadJob) {
            setAlbumProgress(null);
            return;
        }

        let active = true;
        let interval: ReturnType<typeof setInterval> | undefined;

        const fetchProgress = async () => {
            if (!active) return;
            try {
                const response = await api.get(`/downloads/status/${albumDownloadJob.jobId}`);
                if (!active) return;
                setAlbumProgress(response.data);
                if (response.data.status === 'COMPLETED') {
                    setDownloadState({ message: 'Download do álbum concluído com sucesso!', type: 'info' });
                    setAlbumProgress(response.data);
                    setAlbumDownloadJob(null);
                    if (interval) clearInterval(interval);
                    active = false;
                } else if (response.data.status === 'FAILED') {
                    setDownloadState({ message: response.data.errorMessage || 'O download do álbum terminou com falhas.', type: 'error' });
                    setAlbumProgress(response.data);
                    setAlbumDownloadJob(null);
                    if (interval) clearInterval(interval);
                    active = false;
                }
            } catch (err: any) {
                if (!active) return;
                const msg = err.response?.data?.error || err.message || 'Não foi possível consultar o status do download do álbum.';
                setDownloadState({ message: msg, type: 'error' });
                setAlbumDownloadJob(null);
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
    }, [albumDownloadJob]);

    const handleDownloadAlbum = async (album: Album) => {
        try {
            setDownloadState({ message: '', type: null });
            setAlbumProgress(null);
            const response = await api.post('/download/album', { url: album.id });
            setDownloadState({ message: response.data.message || 'Download do álbum iniciado.', type: 'info' });
            setAlbumDownloadJob({ jobId: response.data.jobId, name: album.name });
        } catch (err: any) {
            console.error('Erro ao iniciar download do álbum', err);
            const msg = err.response?.data?.error || err.message || 'Erro desconhecido';
            setDownloadState({ message: `Não foi possível iniciar o download do álbum: ${msg}`, type: 'error' });
            setAlbumDownloadJob(null);
            setAlbumProgress(null);
        }
    };

    const renderPlaylists = () => {
        if (loading) {
            return <div className="text-center"><Spinner animation="border" variant="success" /></div>;
        }
        if (error) {
            return <Alert variant="danger">{error}</Alert>;
        }
        if (filteredPlaylists.length === 0) {
            return <Alert variant="secondary">Nenhuma playlist encontrada.</Alert>;
        }
        return (
            <Row>
                {filteredPlaylists.map((item) => (
                    <Col key={item.id} md={6} lg={4} xl={3} className="mb-4">
                        <Card bg="dark" text="light" className="h-100 shadow-sm border-0">
                            <Link to={`/playlist/${item.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                                <Card.Img
                                    variant="top"
                                    src={item.imageUrl || 'https://via.placeholder.com/400x400?text=Playlist'}
                                    alt={item.name}
                                    style={{ objectFit: 'cover', height: '220px' }}
                                />
                            </Link>
                            <Card.Body className="d-flex flex-column">
                                <div className="d-flex justify-content-between align-items-start mb-2">
                                    <Card.Title className="mb-0 text-white">{item.name}</Card.Title>
                                    <Badge bg="success">{item.trackCount ?? item.totalTracks ?? 0}</Badge>
                                </div>
                                <Card.Text className="text-muted small mb-3">Playlist criada por {item.owner || 'Spotify'}.</Card.Text>
                                <div className="mt-auto d-flex justify-content-between align-items-center">
                                    <small className="text-secondary">{item.owner || 'Spotify'}</small>
                                    <Link to={`/playlist/${item.id}`} className="btn btn-outline-success btn-sm">
                                        Abrir
                                    </Link>
                                </div>
                            </Card.Body>
                        </Card>
                    </Col>
                ))}
            </Row>
        );
    };

    const renderAlbums = () => {
        if (loading) {
            return <div className="text-center"><Spinner animation="border" variant="success" /></div>;
        }
        if (error) {
            return <Alert variant="danger">{error}</Alert>;
        }
        if (filteredAlbums.length === 0) {
            return <Alert variant="secondary">Nenhum álbum encontrado.</Alert>;
        }
        return (
            <>
                {albumDownloadJob && (
                    <Alert variant="dark" className="mb-3">
                        <div className="d-flex flex-column gap-2">
                            <div className="d-flex justify-content-between">
                                <span className="fw-semibold">Baixando: {albumDownloadJob.name}</span>
                                {albumProgress && (
                                    <span className="text-muted small">{albumProgress.completedTracks}/{albumProgress.totalTracks} músicas</span>
                                )}
                            </div>
                            <ProgressBar
                                now={albumProgress?.progressPercentage ?? 0}
                                variant={albumProgress?.status === 'FAILED' ? 'danger' : 'success'}
                                animated={albumProgress?.status === 'IN_PROGRESS'}
                            />
                            {albumProgress?.currentTrack && <span className="text-muted">Baixando: {albumProgress.currentTrack}</span>}
                        </div>
                    </Alert>
                )}
                <Row>
                    {filteredAlbums.map((album) => (
                        <Col key={album.id} md={6} lg={4} xl={3} className="mb-4">
                            <Card bg="dark" text="light" className="h-100 shadow-sm border-0">
                                <Link to={`/album/${album.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                                    <Card.Img
                                        variant="top"
                                        src={album.imageUrl || 'https://via.placeholder.com/400x400?text=Álbum'}
                                        alt={album.name}
                                        style={{ objectFit: 'cover', height: '220px' }}
                                    />
                                </Link>
                                <Card.Body className="d-flex flex-column">
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <Card.Title className="mb-0 text-white">{album.name}</Card.Title>
                                        <Badge bg="success">{album.totalTracks ?? 0}</Badge>
                                    </div>
                                    <Card.Text className="text-muted small mb-2">{album.artists.join(', ')}</Card.Text>
                                    {album.releaseDate && (
                                        <Card.Text className="text-muted small">Lançamento: {album.releaseDate}</Card.Text>
                                    )}
                                    <div className="mt-auto d-flex justify-content-between align-items-center">
                                        <Link to={`/album/${album.id}`} className="btn btn-outline-light btn-sm">
                                            Ver detalhes
                                        </Link>
                                        <Button variant="outline-success" size="sm" onClick={() => handleDownloadAlbum(album)}>
                                            Baixar álbum
                                        </Button>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    ))}
                </Row>
            </>
        );
    };

    const renderSinglePlaylistCard = (item: Playlist | null, description: string) => {
        if (loading && !item) {
            return <div className="text-center"><Spinner animation="border" variant="success" /></div>;
        }
        if (error && !item) {
            return <Alert variant="danger">{error}</Alert>;
        }
        if (!item) {
            return <Alert variant="secondary">Nada para mostrar aqui.</Alert>;
        }
        return (
            <Card bg="dark" text="light" className="shadow-sm border-0">
                <Card.Body className="d-flex flex-column flex-md-row gap-3">
                    <div className="flex-grow-1">
                        <Card.Title className="text-white">{item.name}</Card.Title>
                        <Card.Text className="text-muted mb-2">{description}</Card.Text>
                        <Badge bg="success" className="mb-3">{item.trackCount ?? item.totalTracks ?? 0} músicas</Badge>
                        <div>
                            <Link to={`/playlist/${item.id}`} className="btn btn-outline-success btn-sm">
                                Abrir
                            </Link>
                        </div>
                    </div>
                </Card.Body>
            </Card>
        );
    };

    const renderContent = () => {
        switch (activeView) {
            case 'playlists':
                return renderPlaylists();
            case 'albums':
                return renderAlbums();
            case 'liked':
                return renderSinglePlaylistCard(liked, 'Coleção com todas as músicas que você curtiu.');
            case 'top':
                return renderSinglePlaylistCard(top, 'Suas músicas mais reproduzidas.');
            default:
                return null;
        }
    };

    return (
        <>
            <AppNavbar />
            <Container className="mt-4">
                {downloadState.type && (
                    <Alert
                        variant={downloadState.type === 'error' ? 'danger' : 'info'}
                        className="mb-4"
                        onClose={() => setDownloadState({ message: '', type: null })}
                        dismissible
                    >
                        {downloadState.message}
                    </Alert>
                )}

                <Nav variant="tabs" activeKey={activeView} onSelect={(key) => { setActiveView(key as ViewType); setFilter(''); }} className="mb-4">
                    <Nav.Item>
                        <Nav.Link eventKey="playlists">Minhas playlists</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="albums">Álbuns</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="liked">Curtidas</Nav.Link>
                    </Nav.Item>
                    <Nav.Item>
                        <Nav.Link eventKey="top">Top músicas</Nav.Link>
                    </Nav.Item>
                </Nav>

                {['playlists', 'albums'].includes(activeView) && (
                    <Form className="mb-4">
                        <Form.Control
                            type="text"
                            placeholder="Filtrar pelo nome..."
                            value={filter}
                            onChange={(e) => setFilter(e.target.value)}
                        />
                    </Form>
                )}

                {renderContent()}
            </Container>
        </>
    );
}
