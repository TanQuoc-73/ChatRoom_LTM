const API_BASE = "/api";
const form = document.getElementById("registerForm");
const btn = document.getElementById("btnRegister");

const qs = (id) => document.getElementById(id);
const showError = (id, msg) => {
  const el = qs(id);
  if (el) el.textContent = msg;
};
const clearErrors = () =>
  document.querySelectorAll(".error").forEach((el) => (el.textContent = ""));

// Gọi API đăng ký
async function callRegister(body) {
  btn.disabled = true;
  btn.textContent = "Đang xử lý...";

  try {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await res.json();

    if (!res.ok) {
      if (data.errors && typeof data.errors === "object") {
        Object.keys(data.errors).forEach((field) => {
          const errorId = field + "Error";
          showError(errorId, data.errors[field]);
        });
      } else {
        alert(data.message || "Đăng ký thất bại!");
      }
      return false;
    }

    alert(data.message || "Đăng ký thành công!");

    // Reset form + chuyển trang
    form.reset();
    setTimeout(() => {
      window.location.href = "/api/public/html/Login.html";
    }, 1000);

    return true;
  } catch (err) {
    console.error("Lỗi kết nối:", err);
    alert("Không thể kết nối đến server!");
    return false;
  } finally {
    btn.disabled = false;
    btn.textContent = "Đăng Ký";
  }
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  clearErrors();

  const lastName = qs("lastName").value.trim();
  const firstName = qs("firstName").value.trim();
  const username = qs("username").value.trim();
  const email = qs("email").value.trim();
  const password = qs("password").value;
  const confirm = qs("confirmPassword").value;

  // Validate frontend (khớp với RegisterRequest DTO)
  if (!lastName) return showError("lastNameError", "Bắt buộc phải nhập họ");
  if (!firstName) return showError("firstNameError", "Bắt buộc phải nhập tên");
  if (!username)
    return showError("usernameError", "Tên đăng nhập không được để trống");
  if (!email) return showError("emailError", "Email không được để trống");
  if (!password)
    return showError("passwordError", "Mật khẩu không được để trống");
  if (password.length < 6)
    return showError("passwordError", "Mật khẩu phải ít nhất 6 ký tự");
  if (password !== confirm)
    return showError("confirmPasswordError", "Mật khẩu nhập lại không khớp!");

  const displayName = `${lastName} ${firstName}`.trim();

  const payload = {
    username: username,
    email: email,
    password: password,
    displayName: displayName,
    firstName: firstName,
    lastName: lastName,
  };

  await callRegister(payload);
});
