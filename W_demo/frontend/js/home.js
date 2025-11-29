// Check authentication on page load
document.addEventListener('DOMContentLoaded', async () => {
    // Check if user is logged in
    const token = SessionManager.getToken();
    if (!token) {
        // Redirect to login if not authenticated
        window.location.href = 'index.html';
        return;
    }

    // Validate session
    const session = await AuthService.validateSession();
    if (!session || !session.success) {
        // Session invalid, redirect to login
        SessionManager.removeToken();
        window.location.href = 'index.html';
        return;
    }

    // Load user info
    loadUserInfo();

    // Setup event listeners
    setupEventListeners();
});

// Load user information
function loadUserInfo() {
    const username = localStorage.getItem(STORAGE_KEYS.USERNAME);
    const displayName = localStorage.getItem(STORAGE_KEYS.DISPLAY_NAME);

    // Update display name in welcome message
    const userDisplayNameSpan = document.getElementById('user-display-name');
    
    if (userDisplayNameSpan) {
        if (displayName) {
            userDisplayNameSpan.textContent = displayName;
        } else if (username) {
            userDisplayNameSpan.textContent = username;
        } else {
            userDisplayNameSpan.textContent = 'Người dùng';
        }
    }

    // Update display name in dropdown menu
    const displayNameText = document.getElementById('display-name-text');
    if (displayNameText && displayName) {
        displayNameText.textContent = displayName;
    } else if (displayNameText && username) {
        displayNameText.textContent = username;
    }
}

// Setup event listeners
function setupEventListeners() {
    // User menu toggle
    const userMenuBtn = document.getElementById('user-menu-btn');
    const dropdownMenu = document.getElementById('dropdown-menu');

    if (userMenuBtn && dropdownMenu) {
        userMenuBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdownMenu.classList.toggle('show');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!dropdownMenu.contains(e.target) && !userMenuBtn.contains(e.target)) {
                dropdownMenu.classList.remove('show');
            }
        });
    }

    // Logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
}

// Handle logout
async function handleLogout() {
    try {
        // Call logout API
        await AuthService.logout();
        
        // Redirect to login page
        window.location.href = 'index.html';
    } catch (error) {
        console.error('Logout error:', error);
        // Still redirect to login even if API call fails
        SessionManager.removeToken();
        window.location.href = 'index.html';
    }
}
