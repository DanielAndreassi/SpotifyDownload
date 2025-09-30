import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { DownloadQueueProvider } from './context/DownloadQueueContext';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PlaylistDetailPage from './pages/PlaylistDetailPage';
import AlbumDetailPage from './pages/AlbumDetailPage';
import DownloadQueuePage from './pages/DownloadQueuePage';
import CallbackPage from './pages/CallbackPage';
import ProtectedRoute from './components/ProtectedRoute';
import DownloadProgressToasts from './components/DownloadProgress';
import Root from './Root';

function App() {
    return (
        <AuthProvider>
            <DownloadQueueProvider>
                <Router>
                    <DownloadProgressToasts />
                    <Routes>
                        <Route path="/" element={<Root />} />
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/callback" element={<CallbackPage />} />
                        <Route element={<ProtectedRoute />}>
                            <Route path="/dashboard" element={<DashboardPage />} />
                            <Route path="/playlist/:id" element={<PlaylistDetailPage />} />
                            <Route path="/album/:id" element={<AlbumDetailPage />} />
                            <Route path="/queue" element={<DownloadQueuePage />} />
                        </Route>
                    </Routes>
                </Router>
            </DownloadQueueProvider>
        </AuthProvider>
    );
}

export default App;
