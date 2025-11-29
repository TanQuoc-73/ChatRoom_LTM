// Application State
let currentMode = 'login'; // 'login', 'register', 'forgot'
let otpSent = false;

// DOM Elements
const authTitle = document.getElementById('auth-title');
const authForm = document.getElementById('auth-form');
const submitBtn = document.getElementById('submit-btn');

// Form Fields
const usernameField = document.getElementById('username-field');
const emailField = document.getElementById('email-field');
const displayNameField = document.getElementById('displayname-field');
const nameFields = document.getElementById('name-fields');
const passwordField = document.getElementById('password-field');
const confirmPasswordField = document.getElementById('confirm-password-field');
const otpField = document.getElementById('otp-field');

// Inputs
const usernameInput = document.getElementById('username');
const emailInput = document.getElementById('email');
const displayNameInput = document.getElementById('displayName');
const firstNameInput = document.getElementById('firstName');
const lastNameInput = document.getElementById('lastName');
const passwordInput = document.getElementById('password');
const confirmPasswordInput = document.getElementById('confirmPassword');
const otpInput = document.getElementById('otp');

// Buttons
const sendOtpBtn = document.getElementById('send-otp-btn');
const gotoRegisterBtn = document.getElementById('goto-register');
const gotoForgotBtn = document.getElementById('goto-forgot');
const gotoLoginBtn = document.getElementById('goto-login');

// Link containers
const loginLinks = document.getElementById('login-links');
const otherLinks = document.getElementById('other-links');

// OTP message
const otpSentMessage = document.getElementById('otp-sent-message');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    // Check if user is already logged in
    checkSession();

    // Event Listeners
    authForm.addEventListener('submit', handleSubmit);
    sendOtpBtn.addEventListener('click', handleSendOtp);
    gotoRegisterBtn.addEventListener('click', () => switchMode('register'));
    gotoForgotBtn.addEventListener('click', () => switchMode('forgot'));
    gotoLoginBtn.addEventListener('click', () => switchMode('login'));

    // Clear messages on input
    const allInputs = [usernameInput, emailInput, displayNameInput, firstNameInput, lastNameInput, passwordInput, confirmPasswordInput, otpInput];
    allInputs.forEach(input => {
        input.addEventListener('input', hideMessages);
    });
});

// Check if user has valid session
async function checkSession() {
    const token = SessionManager.getToken();
    if (token) {
        const session = await AuthService.validateSession();
        if (session && session.success) {
            // Redirect to home page
            window.location.href = 'home.html';
        }
    }
}

// Switch between modes (login, register, forgot)
function switchMode(mode) {
    currentMode = mode;
    otpSent = false;
    resetForm();
    updateUI();
}

// Update UI based on current mode
function updateUI() {
    // Update title
    if (currentMode === 'login') {
        authTitle.textContent = 'Đăng Nhập';
        submitBtn.textContent = 'Đăng Nhập';
    } else if (currentMode === 'register') {
        authTitle.textContent = 'Đăng Ký';
        submitBtn.textContent = 'Đăng Ký';
    } else {
        authTitle.textContent = 'Quên Mật Khẩu';
        submitBtn.textContent = 'Đặt Lại Mật Khẩu';
    }

    // Show/hide fields based on mode
    usernameField.style.display = (currentMode === 'login' || currentMode === 'register') ? 'block' : 'none';
    emailField.style.display = (currentMode === 'register' || currentMode === 'forgot') ? 'block' : 'none';
    displayNameField.style.display = currentMode === 'register' ? 'block' : 'none';
    nameFields.style.display = currentMode === 'register' ? 'grid' : 'none';
    confirmPasswordField.style.display = (currentMode === 'register' || currentMode === 'forgot') ? 'block' : 'none';
    otpField.style.display = currentMode === 'forgot' ? 'block' : 'none';
    sendOtpBtn.style.display = currentMode === 'forgot' ? 'inline-block' : 'none';

    // Update password placeholder
    passwordInput.placeholder = currentMode === 'login' ? 'Mật khẩu' : 'Mật khẩu mới';

    // Show/hide links
    loginLinks.style.display = currentMode === 'login' ? 'flex' : 'none';
    otherLinks.style.display = (currentMode === 'register' || currentMode === 'forgot') ? 'flex' : 'none';

    // Update required attributes
    usernameInput.required = (currentMode === 'login' || currentMode === 'register');
    emailInput.required = (currentMode === 'register' || currentMode === 'forgot');
    confirmPasswordInput.required = (currentMode === 'register' || currentMode === 'forgot');
    otpInput.required = currentMode === 'forgot';
}

// Reset form
function resetForm() {
    authForm.reset();
    hideMessages();
    otpSentMessage.style.display = 'none';
}

// Handle Send OTP
async function handleSendOtp() {
    const email = emailInput.value.trim();

    if (!email) {
        showMessage('error', 'Vui lòng nhập email');
        return;
    }

    if (!isValidEmail(email)) {
        showMessage('error', 'Email không hợp lệ');
        return;
    }

    setLoading(true);
    hideMessages();

    const result = await AuthService.forgotPassword(email);

    setLoading(false);

    if (result.success) {
        otpSent = true;
        otpSentMessage.style.display = 'block';
        showMessage('success', 'OTP đã được gửi đến email của bạn!');
        setTimeout(() => hideMessages(), 5000);
    } else {
        showMessage('error', result.message || 'Không thể gửi OTP');
    }
}

// Handle Form Submit
async function handleSubmit(e) {
    e.preventDefault();
    hideMessages();

    const username = usernameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;
    const displayName = displayNameInput.value.trim();
    const firstName = firstNameInput.value.trim();
    const lastName = lastNameInput.value.trim();
    const otp = otpInput.value.trim();

    // Validation
    if (currentMode === 'register' || currentMode === 'forgot') {
        if (password !== confirmPassword) {
            showMessage('error', 'Mật khẩu không khớp!');
            return;
        }
    }

    if (currentMode === 'register' || currentMode === 'forgot') {
        const passwordValidation = isValidPassword(password);
        if (!passwordValidation.valid) {
            showMessage('error', passwordValidation.message);
            return;
        }
    }

    if ((currentMode === 'register' || currentMode === 'forgot') && !isValidEmail(email)) {
        showMessage('error', 'Email không hợp lệ');
        return;
    }

    setLoading(true);

    let result;

    if (currentMode === 'login') {
        result = await AuthService.login(username, password);
        
        if (result.success) {
            showMessage('success', 'Đăng nhập thành công!');
            setTimeout(() => {
                // Redirect to home page
                window.location.href = 'home.html';
            }, 1500);
        } else {
            setLoading(false);
            showMessage('error', result.message || 'Đăng nhập thất bại');
        }
    } else if (currentMode === 'register') {
        result = await AuthService.register(username, email, password, displayName, firstName, lastName);
        
        setLoading(false);
        
        if (result.success) {
            showMessage('success', 'Đăng ký thành công! Đang chuyển về đăng nhập...');
            setTimeout(() => {
                switchMode('login');
            }, 2000);
        } else {
            showMessage('error', result.message || 'Đăng ký thất bại');
        }
    } else {
        // Forgot password mode
        result = await AuthService.resetPassword(email, otp, password);
        
        setLoading(false);
        
        if (result.success) {
            showMessage('success', 'Đặt lại mật khẩu thành công! Đang chuyển về đăng nhập...');
            setTimeout(() => {
                switchMode('login');
            }, 2000);
        } else {
            showMessage('error', result.message || 'Đặt lại mật khẩu thất bại');
        }
    }
}
