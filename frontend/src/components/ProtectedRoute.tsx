import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Container, Spinner } from 'react-bootstrap';
import { useEffect, useState } from 'react';

const ProtectedRoute = () => {
    const { isAuthenticated, isLoading } = useAuth();
    const [initialCheck, setInitialCheck] = useState(true);

    // Give some time for auth check to complete on page refresh
    useEffect(() => {
        const timer = setTimeout(() => {
            setInitialCheck(false);
        }, 1500); // Wait 1.5 seconds for auth check

        return () => clearTimeout(timer);
    }, []);

    if (isLoading || initialCheck) {
        return (
            <Container className="d-flex justify-content-center align-items-center" style={{ height: '100vh' }}>
                <Spinner animation="border" variant="success" />
                <span className="ms-2">Carregando...</span>
            </Container>
        );
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
