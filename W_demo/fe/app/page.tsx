"use client";

import { useState } from "react";
import { useAuth } from "@/hooks/useAuth";

type AuthMode = "login" | "register" | "forgot";

export default function Home() {
  const [mode, setMode] = useState<AuthMode>("login");
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
    displayName: "",
    firstName: "",
    lastName: "",
    otp: "",
  });
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [otpSent, setOtpSent] = useState(false);

  const { isLoading, error, login, register, forgotPassword, resetPassword, clearError } = useAuth();

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    clearError();
    setSuccessMessage(null);
  };

  const handleSendOtp = async () => {
    if (!formData.email) return;

    const success = await forgotPassword(formData.email);
    if (success) {
      setOtpSent(true);
      setSuccessMessage("OTP đã được gửi đến email của bạn!");
      setTimeout(() => setSuccessMessage(null), 5000);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMessage(null);

    if (mode === "login") {
      const success = await login(formData.username, formData.password);
      if (success) {
        setSuccessMessage("Đăng nhập thành công!");
        setTimeout(() => {
          window.location.href = "/home";
        }, 1500);
      }
    } else if (mode === "register") {
      if (formData.password !== formData.confirmPassword) {
        clearError();
        alert("Mật khẩu không khớp!");
        return;
      }

      const success = await register(
        formData.username,
        formData.email,
        formData.password,
        formData.displayName,
        formData.firstName,
        formData.lastName
      );

      if (success) {
        setSuccessMessage("Đăng ký thành công! Đang chuyển về đăng nhập...");
        setTimeout(() => {
          setMode("login");
          resetForm();
        }, 2000);
      }
    } else {
      if (formData.password !== formData.confirmPassword) {
        clearError();
        alert("Mật khẩu không khớp!");
        return;
      }

      const success = await resetPassword(formData.email, formData.otp, formData.password);
      if (success) {
        setSuccessMessage("Đặt lại mật khẩu thành công! Đang chuyển về đăng nhập...");
        setTimeout(() => {
          setMode("login");
          resetForm();
        }, 2000);
      }
    }
  };

  const resetForm = () => {
    setFormData({
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
      displayName: "",
      firstName: "",
      lastName: "",
      otp: "",
    });
    setOtpSent(false);
    clearError();
    setSuccessMessage(null);
  };

  const inputClass = "w-full px-4 py-2.5 bg-white/30 backdrop-blur-sm border-2 border-white/40 rounded-xl focus:border-white/60 focus:bg-white/40 focus:outline-none transition-all text-black placeholder-black/70 font-medium";
  const buttonClass = "px-4 py-2.5 bg-blue-500/80 backdrop-blur-sm text-white rounded-xl hover:bg-blue-600/80 transition-colors font-medium whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed border border-white/30";

  return (
    <div className="relative w-full min-h-screen flex overflow-hidden">
      {/* Full-Width Background Video */}
      <div className="absolute inset-0">
        <video
          autoPlay
          loop
          muted
          playsInline
          className="absolute inset-0 w-full h-full object-cover"
        >
          <source src="/videos/background.mp4" type="video/mp4" />
          <img src="/auth-bg.jpg" alt="Background" className="w-full h-full object-cover" />
        </video>
      </div>

      {/* Right Side - Auth Form with Backdrop Blur */}
      <div className="relative z-10 ml-auto w-full md:w-[420px] flex items-center justify-center p-2.5">
        <div className="w-full max-w-sm bg-white/20 backdrop-blur-2xl rounded-2xl shadow-2xl p-6 border-2 border-black/50">
          {/* Title */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-white mb-1 drop-shadow-lg">
              {mode === "login" ? "Đăng Nhập" : mode === "register" ? "Đăng Ký" : "Quên Mật Khẩu"}
            </h1>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-4 p-3 bg-red-500/80 backdrop-blur-sm border border-red-300/50 rounded-lg">
              <p className="text-sm text-white font-medium">{error}</p>
            </div>
          )}

          {/* Success Message */}
          {successMessage && (
            <div className="mb-4 p-3 bg-green-500/80 backdrop-blur-sm border border-green-300/50 rounded-lg">
              <p className="text-sm text-white font-medium">{successMessage}</p>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            {/* Username (Login & Register) */}
            {(mode === "login" || mode === "register") && (
              <div>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleInputChange}
                  placeholder="Tên đăng nhập"
                  className={inputClass}
                  required
                  disabled={isLoading}
                />
              </div>
            )}

            {/* Email (Register & Forgot Password) */}
            {(mode === "register" || mode === "forgot") && (
              <div className="flex gap-2">
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  placeholder="Email"
                  className={inputClass}
                  required
                  disabled={isLoading}
                />
                {mode === "forgot" && (
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    disabled={isLoading || !formData.email}
                    className={buttonClass}
                  >
                    {isLoading ? "..." : "Gửi OTP"}
                  </button>
                )}
              </div>
            )}

            {/* Display Name, First Name, Last Name (Register only) */}
            {mode === "register" && (
              <>
                <div>
                  <input
                    type="text"
                    name="displayName"
                    value={formData.displayName}
                    onChange={handleInputChange}
                    placeholder="Tên hiển thị"
                    className={inputClass}
                    disabled={isLoading}
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    placeholder="Tên"
                    className={inputClass}
                    disabled={isLoading}
                  />
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    placeholder="Họ"
                    className={inputClass}
                    disabled={isLoading}
                  />
                </div>
              </>
            )}

            {/* Password */}
            <div>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder={mode === "login" ? "Mật khẩu" : "Mật khẩu mới"}
                className={inputClass}
                required
                disabled={isLoading}
              />
            </div>

            {/* Confirm Password (Register & Forgot Password) */}
            {(mode === "register" || mode === "forgot") && (
              <div>
                <input
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleInputChange}
                  placeholder="Nhập lại mật khẩu"
                  className={inputClass}
                  required
                  disabled={isLoading}
                />
              </div>
            )}

            {/* OTP (Forgot Password only) */}
            {mode === "forgot" && (
              <div>
                <input
                  type="text"
                  name="otp"
                  value={formData.otp}
                  onChange={handleInputChange}
                  placeholder="Mã xác nhận (OTP)"
                  className={inputClass}
                  required
                  disabled={isLoading}
                />
                {otpSent && (
                  <p className="mt-1 text-xs text-green-300 font-medium">
                    ✓ OTP đã được gửi đến email
                  </p>
                )}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-2.5 bg-gradient-to-r from-blue-500/80 to-blue-600/80 backdrop-blur-sm text-white rounded-xl font-semibold hover:from-blue-600/80 hover:to-blue-700/80 transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none border border-white/30"
            >
              {isLoading
                ? "Đang xử lý..."
                : mode === "login"
                ? "Đăng Nhập"
                : mode === "register"
                ? "Đăng Ký"
                : "Đặt Lại Mật Khẩu"}
            </button>
          </form>

          {/* Toggle Mode Links */}
          <div className="mt-6 text-center space-y-2">
            {mode === "login" && (
              <>
                <button
                  type="button"
                  onClick={() => { setMode("register"); resetForm(); }}
                  disabled={isLoading}
                  className="block w-full text-black hover:text-black/70 font-medium transition-colors disabled:opacity-50 drop-shadow-md"
                >
                  Chưa có tài khoản? Đăng ký ngay
                </button>
                <button
                  type="button"
                  onClick={() => { setMode("forgot"); resetForm(); }}
                  disabled={isLoading}
                  className="block w-full text-black/90 hover:text-black font-medium transition-colors disabled:opacity-50 drop-shadow-md"
                >
                  Quên mật khẩu?
                </button>
              </>
            )}
            {(mode === "register" || mode === "forgot") && (
              <button
                type="button"
                onClick={() => { setMode("login"); resetForm(); }}
                disabled={isLoading}
                className="block w-full text-black hover:text-black/70 font-medium transition-colors disabled:opacity-50 drop-shadow-md"
              >
                Đã có tài khoản? Đăng nhập
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
