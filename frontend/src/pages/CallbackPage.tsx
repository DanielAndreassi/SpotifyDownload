import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Container, Spinner, Alert } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext';

export default function CallbackPage() {
    const [message, setMessage] = useState('Finalizing authentication...');
    const [error, setError] = useState('');
    const location = useLocation();
    const navigate = useNavigate();
    const { checkAuthStatus } = useAuth();

    useEffect(() => {
        const processAuth = async () => {
            const queryParams = new URLSearchParams(location.search);
            const token = queryParams.get('token');
            const errorParam = queryParams.get('error');

            if (errorParam) {
                setError('Authentication failed. Please try logging in again.');
                setTimeout(() => navigate('/login', { replace: true }), 3000);
                return;
            }

            let storedToken = token;
            if (!storedToken) {
                // If no token in URL, try to get from cookies as a fallback
                const cookies = document.cookie.split(';');
                const jwtCookie = cookies.find(cookie => cookie.trim().startsWith('jwt_token='));
                if (jwtCookie) {
                    storedToken = jwtCookie.split('=')[1];
                }
            }

            if (storedToken) {
                localStorage.setItem('jwt_token', storedToken);
                console.log('JWT token stored');
            } else {
                setError('Authentication token not found. Please try logging in again.');
                setTimeout(() => navigate('/login', { replace: true }), 3000);
                return;
            }

            // Now that token is stored, verify it
            setMessage('Verifying credentials...');
            const isAuthenticated = await checkAuthStatus();

            if (isAuthenticated) {
                setMessage('Success! Redirecting to your dashboard...');
                setTimeout(() => {
                    navigate('/dashboard', { replace: true });
                }, 1000); // Short delay so user can see success message
            } else {
                setError('Could not verify authentication. Please try logging in again.');
                setTimeout(() => {
                    navigate('/login', { replace: true });
                }, 3000);
            }
        };

        processAuth();
    }, [location, navigate, checkAuthStatus]);

    return (
        <Container className="d-flex flex-column align-items-center justify-content-center" style={{ minHeight: '100vh' }}>
            {error ? (
                <Alert variant="danger">{error}</Alert>
            ) : (
                <div className="text-center">
                    <Spinner animation="border" variant="success" />
                    <p className="mt-3">{message}</p>
                </div>
            )}
        </Container>
    );
}