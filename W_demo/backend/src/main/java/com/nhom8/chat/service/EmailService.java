package com.nhom8.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendOtpEmail(String toEmail, String otp, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã OTP Khôi Phục Mật Khẩu - Chat Room");
            
            String emailBody = String.format(
                "Xin chào %s,\n\n" +
                "Bạn đã yêu cầu khôi phục mật khẩu cho tài khoản Chat Room của mình.\n\n" +
                "Mã OTP của bạn là: %s\n\n" +
                "Mã này sẽ hết hạn sau 10 phút.\n\n" +
                "Nếu bạn không yêu cầu khôi phục mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Chat Room Team",
                username,
                otp
            );
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
    
    public void sendPasswordResetConfirmation(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mật Khẩu Đã Được Thay Đổi - Chat Room");
            
            String emailBody = String.format(
                "Xin chào %s,\n\n" +
                "Mật khẩu của bạn đã được thay đổi thành công.\n\n" +
                "Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với chúng tôi ngay lập tức.\n\n" +
                "Trân trọng,\n" +
                "Chat Room Team",
                username
            );
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Password reset confirmation email sent to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send confirmation email to: {}", toEmail, e);
            // Don't throw exception here, password was already reset
        }
    }
}
