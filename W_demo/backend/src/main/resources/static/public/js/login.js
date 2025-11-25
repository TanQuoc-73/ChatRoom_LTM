// ===============================
// LOGIN.JS - dành cho trang login.html
// ===============================

const API_BASE = "/api";

// --- Lấy các phần tử từ HTML ---
const formLogin = document.querySelector(".login-form");
const inputUser = document.querySelector('input[type="text"]');
const inputPass = document.querySelector('input[type="password"]');
const btnLogin = document.querySelector(".login-button");
const messageBox = document.createElement("div");
messageBox.style.marginTop = "10px";
messageBox.style.textAlign = "center";
messageBox.style.color = "#ffcccc";
formLogin.appendChild(messageBox);

// ===============================
// HIỆN/ẨN MẬT KHẨU
// ===============================
const passwordToggle = document.querySelector(".password-toggle");
passwordToggle.addEventListener("click", () => {
  if (inputPass.type === "password") {
    inputPass.type = "text";
    passwordToggle.innerHTML = '<i class="fi fi-sr-eye"></i>';
  } else {
    inputPass.type = "password";
    passwordToggle.innerHTML = '<i class="fi fi-sr-eye-crossed"></i>';
  }
});

// ===============================
// HÀM HIỂN THỊ THÔNG BÁO
// ===============================
function showMessage(msg, isError = true) {
  messageBox.textContent = msg;
  messageBox.style.color = isError ? "#ff6b6b" : "#6bff80";
}

// ===============================
// LƯU SESSION TOKEN
// ===============================
function saveSession(token, userId, username) {
  localStorage.setItem("zmnt_session_token", token);
  if (userId) localStorage.setItem("zmnt_user_id", userId);
  if (username) localStorage.setItem("zmnt_username", username);
}

// ===============================
// GỌI API LOGIN
// ===============================
async function login(username, password) {
  try {
    btnLogin.disabled = true;
    showMessage("");

    const payload = {
      username,
      password,
      deviceType: "WEB",
    };

    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    const json = await res.json();

    if (!res.ok) {
      showMessage(json.message || "Đăng nhập thất bại");
      btnLogin.disabled = false;
      return null;
    }

    const token = json.sessionToken || json.data?.sessionToken;
    const userId = json.userId || json.data?.userId;
    const usernameResp = json.username || json.data?.username;

    if (!token) {
      showMessage("Không nhận được token từ server!", true);
      return null;
    }

    saveSession(token, userId, usernameResp);
    showMessage("Đăng nhập thành công!", false);

    return true;
  } catch (err) {
    console.error("Login error:", err);
    showMessage("Không thể kết nối server!");
    return null;
  } finally {
    btnLogin.disabled = false;
  }
}

// ===============================
// XỬ LÝ SUBMIT LOGIN
// ===============================
formLogin.addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = inputUser.value.trim();
  const password = inputPass.value.trim();

  if (!username || !password) {
    showMessage("Vui lòng nhập đầy đủ thông tin!");
    return;
  }

  const ok = await login(username, password);

  if (ok) {
    setTimeout(() => {
      window.location.href = "/api/public/html/trangchu.html"; // 👉 đổi theo trang chính của bạn
    }, 500);
  }
});
