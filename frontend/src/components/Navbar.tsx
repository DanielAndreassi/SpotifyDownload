import { Navbar, Container, Button, Nav, Badge } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext';
import { useDownloadQueue } from '../context/DownloadQueueContext';
import { useNavigate, Link } from 'react-router-dom';

export default function AppNavbar() {
    const { user, logout } = useAuth();
    const { queue } = useDownloadQueue();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <Navbar bg="dark" variant="dark" expand="lg">
            <Container>
                <Navbar.Brand as={Link} to="/dashboard">Spotify Downloader</Navbar.Brand>
                <Navbar.Toggle aria-controls="basic-navbar-nav" />
                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className="ms-auto d-flex align-items-center">
                        {user && <Navbar.Text className="me-3">Signed in as: {user.displayName}</Navbar.Text>}
                        
                        <Link to="/queue" className="btn btn-secondary me-3">
                            Download Queue <Badge bg="success">{queue.length}</Badge>
                        </Link>
                        
                        <Button variant="outline-danger" onClick={handleLogout}>Logout</Button>
                    </Nav>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
}