import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true, // Important: include cookies in requests
    timeout: 10000 // 10 second timeout
});

// Request interceptor to add JWT token and logging
api.interceptors.request.use(
    (config) => {
        console.log(`Making request to: ${config.baseURL}${config.url}`);

        // Add JWT token from localStorage or cookie
        let token = localStorage.getItem('jwt_token');

        // If no token in localStorage, try to get from cookies
        if (!token) {
            const cookies = document.cookie.split(';');
            const jwtCookie = cookies.find(cookie => cookie.trim().startsWith('jwt_token='));
            if (jwtCookie) {
                token = jwtCookie.split('=')[1];
            }
        }

        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
            console.log('Adding JWT token to request');
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor for better error handling
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            console.warn('Authentication error detected in API call');
            // Clear invalid token
            localStorage.removeItem('jwt_token');
            // Clear JWT cookie
            document.cookie = 'jwt_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
            // Redirect to login if not already there
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default api;
