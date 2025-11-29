// Authentication Service
const AuthService = {
    /**
     * Login user with username and password
     */
    async login(username, password) {
        try {
            const response = await fetch(API_ENDPOINTS.AUTH.LOGIN, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username,
                    password,
                    deviceType: 'WEB',
                }),
            });

            const data = await response.json();

            if (data.success && data.sessionToken) {
                SessionManager.setToken(data.sessionToken);
                if (data.userId && data.username) {
                    SessionManager.setUserInfo(data.userId, data.username, data.displayName);
                }
            }

            return data;
        } catch (error) {
            return {
                success: false,
                message: handleApiError(error),
            };
        }
    },

    /**
     * Register new user
     */
    async register(username, email, password, displayName, firstName, lastName) {
        try {
            const response = await fetch(API_ENDPOINTS.AUTH.REGISTER, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username,
                    email,
                    password,
                    displayName,
                    firstName,
                    lastName,
                }),
            });

            const data = await response.json();
            return data;
        } catch (error) {
            return {
                success: false,
                message: handleApiError(error),
            };
        }
    },

    /**
     * Logout current user
     */
    async logout() {
        const token = SessionManager.getToken();
        if (token) {
            try {
                await fetch(API_ENDPOINTS.AUTH.LOGOUT, {
                    method: 'POST',
                    headers: {
                        ...SessionManager.getAuthHeader(),
                    },
                });
            } catch (error) {
                console.error('Logout error:', error);
            } finally {
                SessionManager.removeToken();
            }
        }
    },

    /**
     * Send OTP to email for password reset
     */
    async forgotPassword(email) {
        try {
            const response = await fetch(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email }),
            });

            return await response.json();
        } catch (error) {
            return {
                success: false,
                message: handleApiError(error),
            };
        }
    },

    /**
     * Reset password with OTP
     */
    async resetPassword(email, otp, newPassword) {
        try {
            const response = await fetch(API_ENDPOINTS.AUTH.RESET_PASSWORD, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    email,
                    otp,
                    newPassword,
                }),
            });

            return await response.json();
        } catch (error) {
            return {
                success: false,
                message: handleApiError(error),
            };
        }
    },

    /**
     * Validate current session token
     */
    async validateSession() {
        const token = SessionManager.getToken();
        if (!token) return null;

        try {
            const response = await fetch(API_ENDPOINTS.AUTH.VALIDATE, {
                method: 'GET',
                headers: {
                    ...SessionManager.getAuthHeader(),
                },
            });

            const data = await response.json();

            if (data.success && data.userId && data.username) {
                SessionManager.setUserInfo(data.userId, data.username, data.displayName);
            }

            return data;
        } catch (error) {
            SessionManager.removeToken();
            return null;
        }
    },
};
