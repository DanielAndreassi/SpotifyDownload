import { Container, Table, Button, Alert, Spinner } from 'react-bootstrap';
import { useDownloadQueue } from '../context/DownloadQueueContext';
import AppNavbar from '../components/Navbar';
import type { Track } from '../types/Track';
import api from '../services/api';
import { useState } from 'react';

function formatDuration(ms: number) {
    const minutes = Math.floor(ms / 60000);
    const seconds = ((ms % 60000) / 1000).toFixed(0);
    return minutes + ":" + (parseInt(seconds) < 10 ? '0' : '') + seconds;
}

export default function DownloadQueuePage() {
    const { queue, removeFromQueue, clearQueue } = useDownloadQueue();
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleDownloadAll = async () => {
        if (queue.length === 0) {
            setError("Sua fila está vazia.");
            return;
        }

        setLoading(true);
        setMessage('');
        setError('');

        try {
            // 1. Create a temporary playlist
            setMessage('Passo 1/3: criando playlist temporária...');
            const playlistName = `SpotifyDownloader - ${new Date().toLocaleString()}`;
            const createPlaylistResponse = await api.post('/playlists/create', {
                name: playlistName,
                description: 'Playlist temporária utilizada para baixar as faixas.'
            });
            const newPlaylistId = createPlaylistResponse.data.id;

            // 2. Add tracks to the new playlist
            setMessage('Passo 2/3: adicionando músicas na playlist...');
            const trackUris = queue.map(track => `spotify:track:${track.id}`);
            await api.post(`/playlists/${newPlaylistId}/add-tracks`, { trackUris });

            // 3. Start the download
            setMessage('Passo 3/3: iniciando download...');
            await api.post('/downloads/start', { playlistId: newPlaylistId });

            setMessage(`Download iniciado! Uma playlist temporária chamada "${playlistName}" foi criada na sua conta Spotify.`);
            clearQueue();

        } catch (err) {
            console.error(err);
            setError('Ocorreu um erro durante o processo de download. Verifique o console para mais detalhes.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <AppNavbar />
            <Container className="mt-4">
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h1>Fila de download</h1>
                    <div>
                        <Button variant="danger" onClick={clearQueue} disabled={queue.length === 0 || loading} className="me-2">
                            Limpar fila
                        </Button>
                        <Button variant="success" onClick={handleDownloadAll} disabled={queue.length === 0 || loading}>
                            {loading ? <Spinner as="span" animation="border" size="sm" /> : `Baixar tudo (${queue.length})`}
                        </Button>
                    </div>
                </div>

                {message && <Alert variant="info">{message}</Alert>}
                {error && <Alert variant="danger">{error}</Alert>}

                {queue.length > 0 ? (
                    <Table striped bordered hover variant="dark">
                        <thead>
                            <tr>
                                <th>Música</th>
                                <th>Artista</th>
                                <th>Album</th>
                                <th>Duração</th>
                                <th>Ação</th>
                            </tr>
                        </thead>
                        <tbody>
                            {queue.map((track: Track, index) => (
                                <tr key={`${track.id}-${index}`}>
                                    <td>{track.name}</td>
                                    <td>{track.artists.map(a => a.name).join(', ')}</td>
                                    <td>{track.album.name}</td>
                                    <td>{formatDuration(track.duration_ms)}</td>
                                    <td>
                                        <Button variant="outline-danger" size="sm" onClick={() => removeFromQueue(track.id)} disabled={loading}>
                                            Remover
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                ) : (
                    !loading && <Alert variant="secondary">Sua fila de download está vazia. Adicione músicas a partir de uma playlist para começar.</Alert>
                )}
            </Container>
        </>
    );
}
