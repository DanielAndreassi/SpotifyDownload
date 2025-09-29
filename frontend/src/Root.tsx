import { Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Container, Spinner } from 'react-bootstrap';
import { useEffect, useState } from 'react';

const Root = () => {
    const { isAuthenticated, isLoading } = useAuth();
    const [minimumLoadTime, setMinimumLoadTime] = useState(true);

    // Ensure minimum loading time to avoid flash of login page
    useEffect(() => {
        const timer = setTimeout(() => {
            setMinimumLoadTime(false);
        }, 1000); // Wait at least 1 second

        return () => clearTimeout(timer);
    }, []);

    if (isLoading || minimumLoadTime) {
        return (
            <Container className="d-flex justify-content-center align-items-center" style={{ height: '100vh' }}>
                <Spinner animation="border" variant="success" />
                <span className="ms-2">Verificando autenticação...</span>
            </Container>
        );
    }

    return isAuthenticated ? <Navigate to="/dashboard" /> : <Navigate to="/login" />;
};

export default Root;
