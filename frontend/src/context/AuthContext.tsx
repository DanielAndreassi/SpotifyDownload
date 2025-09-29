import { createContext, useState, useEffect, useContext } from 'react';
import type { ReactNode } from 'react';
import api from '../services/api';

interface User {
    displayName: string;
    email: string;
    spotifyId: string;
    userId: number;
}

interface AuthContextType {
    isAuthenticated: boolean;
    user: User | null;
    isLoading: boolean;
    logout: () => void;
    checkAuthStatus: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const checkAuthStatus = async (): Promise<boolean> => {
        try {
            const response = await api.get('/auth/status', { 
                timeout: 5000 // 5 second timeout for faster response
            });
            if (response.data.authenticated) {
                setIsAuthenticated(true);
                setUser({
                    displayName: response.data.user,
                    email: response.data.email,
                    spotifyId: response.data.spotifyId,
                    userId: response.data.userId
                });

                console.log('User authenticated:', response.data.user);
                return true;
            } else {
                setIsAuthenticated(false);
                setUser(null);
                console.log('User not authenticated:', response.data.error);
                return false;
            }
        } catch (error: any) {
            setIsAuthenticated(false);
            setUser(null);
            // Only log if it's not a network error
            if (error.response?.status !== 401) {
                console.error("Failed to check auth status", error);
            }
            return false;
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        // Initial check immediately
        checkAuthStatus();
        
        // Check auth status periodically to catch authentication changes
        const interval = setInterval(() => {
            checkAuthStatus();
        }, 30000); // Check every 30 seconds
        
        // Check auth status when the window regains focus (user comes back to tab)
        const handleFocus = () => {
            checkAuthStatus();
        };
        
        // Check on page visibility change (user switches tabs)
        const handleVisibilityChange = () => {
            if (!document.hidden) {
                checkAuthStatus();
            }
        };
        
        window.addEventListener('focus', handleFocus);
        document.addEventListener('visibilitychange', handleVisibilityChange);
        
        return () => {
            clearInterval(interval);
            window.removeEventListener('focus', handleFocus);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, []);

    const logout = async () => {
        try {
            await api.post('/auth/logout');
        } catch (error) {
            console.error('Error during logout:', error);
        } finally {
            // Clear local state and storage
            setIsAuthenticated(false);
            setUser(null);
            localStorage.removeItem('jwt_token');
            // Clear JWT cookie
            document.cookie = 'jwt_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        }
    };

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, isLoading, logout, checkAuthStatus }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
