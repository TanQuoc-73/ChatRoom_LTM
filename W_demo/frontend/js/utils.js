// Session Manager
const SessionManager = {
    // Get token from localStorage
    getToken() {
        return localStorage.getItem(STORAGE_KEYS.SESSION_TOKEN);
    },

    // Set token in localStorage
    setToken(token) {
        localStorage.setItem(STORAGE_KEYS.SESSION_TOKEN, token);
    },

    // Remove token from localStorage
    removeToken() {
        localStorage.removeItem(STORAGE_KEYS.SESSION_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.USER_ID);
        localStorage.removeItem(STORAGE_KEYS.USERNAME);
        localStorage.removeItem(STORAGE_KEYS.DISPLAY_NAME);
    },

    // Set user info in localStorage
    setUserInfo(userId, username, displayName) {
        localStorage.setItem(STORAGE_KEYS.USER_ID, userId);
        localStorage.setItem(STORAGE_KEYS.USERNAME, username);
        if (displayName) {
            localStorage.setItem(STORAGE_KEYS.DISPLAY_NAME, displayName);
        }
    },

    // Get auth header for API requests
    getAuthHeader() {
        const token = this.getToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }
};

// Validation Functions
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function isValidPassword(password) {
    if (password.length < 6) {
        return {
            valid: false,
            message: 'Mật khẩu phải có ít nhất 6 ký tự'
        };
    }
    return { valid: true };
}

// Error Handler
function handleApiError(error) {
    if (error instanceof Error) {
        return error.message;
    }
    return 'Đã xảy ra lỗi không xác định';
}

// UI Helper Functions
function showMessage(type, message) {
    const errorDiv = document.getElementById('error-message');
    const successDiv = document.getElementById('success-message');
    const errorText = document.getElementById('error-text');
    const successText = document.getElementById('success-text');

    // Hide both messages first
    errorDiv.style.display = 'none';
    successDiv.style.display = 'none';

    if (type === 'error') {
        errorText.textContent = message;
        errorDiv.style.display = 'block';
    } else if (type === 'success') {
        successText.textContent = message;
        successDiv.style.display = 'block';
    }
}

function hideMessages() {
    document.getElementById('error-message').style.display = 'none';
    document.getElementById('success-message').style.display = 'none';
}

function setLoading(isLoading) {
    const form = document.getElementById('auth-form');
    const submitBtn = document.getElementById('submit-btn');
    const inputs = form.querySelectorAll('input, button');

    if (isLoading) {
        form.classList.add('loading');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang xử lý...';
        inputs.forEach(input => input.disabled = true);
    } else {
        form.classList.remove('loading');
        submitBtn.disabled = false;
        inputs.forEach(input => input.disabled = false);
    }
}
