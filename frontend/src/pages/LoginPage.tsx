import { useState } from 'react';
import { Container, Form, Button, Card, Spinner } from 'react-bootstrap';
import api from '../services/api';

export default function LoginPage() {
    const [clientId, setClientId] = useState('');
    const [clientSecret, setClientSecret] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        if (!clientId || !clientSecret) {
            setError('Client ID and Client Secret are required.');
            setLoading(false);
            return;
        }

        try {
            const response = await api.post('/auth/authorize', { clientId, clientSecret });
            const { authUrl } = response.data;

            // Store credentials temporarily for the callback
            sessionStorage.setItem('spotify_client_id', clientId);
            sessionStorage.setItem('spotify_client_secret', clientSecret);

            // Redirect the user to the Spotify authorization page
            window.location.href = authUrl;
        } catch (err) {
            setError('Failed to get authorization URL. Please check your credentials.');
            setLoading(false);
        }
    };

    return (
        <Container className="d-flex align-items-center justify-content-center" style={{ minHeight: '100vh' }}>
            <Card bg="dark" text="white" style={{ width: '30rem' }}>
                <Card.Body>
                    <Card.Title className="text-center mb-4">Spotify Downloader</Card.Title>
                    <Card.Subtitle className="text-center text-muted mb-4">Enter your Spotify Credentials</Card.Subtitle>
                    <Form onSubmit={handleSubmit}>
                        <Form.Group className="mb-3" controlId="clientId">
                            <Form.Label>Client ID</Form.Label>
                            <Form.Control
                                type="text"
                                placeholder="Enter your Client ID"
                                value={clientId}
                                onChange={(e) => setClientId(e.target.value)}
                                disabled={loading}
                            />
                        </Form.Group>

                        <Form.Group className="mb-3" controlId="clientSecret">
                            <Form.Label>Client Secret</Form.Label>
                            <Form.Control
                                type="password"
                                placeholder="Enter your Client Secret"
                                value={clientSecret}
                                onChange={(e) => setClientSecret(e.target.value)}
                                disabled={loading}
                            />
                        </Form.Group>

                        {error && <p className="text-danger">{error}</p>}

                        <div className="d-grid">
                            <Button variant="success" type="submit" disabled={loading}>
                                {loading ? <Spinner as="span" animation="border" size="sm" /> : 'Login with Spotify'}
                            </Button>
                        </div>
                    </Form>
                </Card.Body>
            </Card>
        </Container>
    );
}
