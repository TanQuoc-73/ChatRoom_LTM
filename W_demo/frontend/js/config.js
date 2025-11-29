// API Configuration
const API_BASE_URL = 'http://localhost:8081/api';

const API_ENDPOINTS = {
    AUTH: {
        LOGIN: `${API_BASE_URL}/auth/login`,
        REGISTER: `${API_BASE_URL}/auth/register`,
        LOGOUT: `${API_BASE_URL}/auth/logout`,
        VALIDATE: `${API_BASE_URL}/auth/validate`,
        FORGOT_PASSWORD: `${API_BASE_URL}/auth/forgot-password`,
        RESET_PASSWORD: `${API_BASE_URL}/auth/reset-password`,
    }
};

// Local Storage Keys
const STORAGE_KEYS = {
    SESSION_TOKEN: 'sessionToken',
    USER_ID: 'userId',
    USERNAME: 'username',
    DISPLAY_NAME: 'displayName',
};
