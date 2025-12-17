const API_BASE = "/api";
const formLogin = document.querySelector(".login-form");
const inputUser = document.querySelector('input[type="text"]');
const inputPass = document.querySelector('input[type="password"]');
const btnLogin = document.querySelector(".login-button");

// message box nhỏ
const messageBox = document.createElement("div");
messageBox.style.marginTop = "10px";
messageBox.style.textAlign = "center";
messageBox.style.fontSize = "14px";
formLogin.appendChild(messageBox);

// ẩn/show password
const passwordToggle = document.querySelector(".password-toggle");
if (passwordToggle) {
  passwordToggle.addEventListener("click", () => {
    if (inputPass.type === "password") {
      inputPass.type = "text";
      passwordToggle.innerHTML = '<i class="fi fi-sr-eye"></i>';
    } else {
      inputPass.type = "password";
      passwordToggle.innerHTML = '<i class="fi fi-sr-eye-crossed"></i>';
    }
  });
}

function showMessage(msg = "", isError = true) {
  messageBox.textContent = msg;
  messageBox.style.color = isError ? "#ff6b6b" : "#6bff80";
}

// lưu session vào localStorage
function saveSession(token, userId, username, extra = {}) {
  localStorage.setItem("zmnt_session_token", token);
  if (userId !== undefined && userId !== null)
    localStorage.setItem("zmnt_user_id", String(userId));
  if (username) localStorage.setItem("zmnt_username", username);
  if (extra.expiresAt)
    localStorage.setItem("zmnt_token_expires_at", String(extra.expiresAt));
}

function extractTokenFromResponse(body) {
  if (!body) return null;
  if (typeof body === "string") return null;
  const maybe = (o) =>
    o &&
    (o.sessionToken ||
      o.token ||
      o.data?.sessionToken ||
      o.data?.token ||
      o.data?.session?.sessionToken);
  return maybe(body) || maybe(body.data) || null;
}

async function login(username, password) {
  btnLogin.disabled = true;
  showMessage("");

  try {
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

    const text = await res.text();
    let json;
    try {
      json = JSON.parse(text);
    } catch (e) {
      json = null;
    }

    if (!res.ok) {
      const msg =
        json?.message || json?.error || text || `Login failed (${res.status})`;
      showMessage(msg, true);
      return null;
    }

    const token = extractTokenFromResponse(json);
    const userId =
      json?.userId ||
      json?.data?.userId ||
      json?.data?.session?.userId ||
      json?.data?.user?.id;
    const usernameResp =
      json?.username || json?.data?.username || json?.data?.user?.username;
    let extra = {};
    if (json?.expiresAt) extra.expiresAt = json.expiresAt;
    if (json?.expiresIn)
      extra.expiresAt = Date.now() + Number(json.expiresIn) * 1000;

    if (!token) {
      console.warn("login: no token found in response", json);
      showMessage("Không nhận được token từ server!", true);
      return null;
    }

    saveSession(token, userId, usernameResp, extra);
    showMessage("Đăng nhập thành công!", false);
    return true;
  } catch (err) {
    console.error("Login error:", err);
    showMessage("Không thể kết nối server!", true);
    return null;
  } finally {
    btnLogin.disabled = false;
  }
}

formLogin.addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = inputUser.value.trim();
  const password = inputPass.value.trim();
  if (!username || !password) {
    showMessage("Vui lòng nhập đầy đủ thông tin!", true);
    return;
  }
  const ok = await login(username, password);
  if (ok) {
    window.location.href = "/api/public/html/trangchu.html";
  }
});
