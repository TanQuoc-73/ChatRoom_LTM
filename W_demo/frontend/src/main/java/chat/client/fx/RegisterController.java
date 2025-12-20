package chat.client.fx;

import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.prefs.Preferences;

public class RegisterController {

    // ========================== CÀI ĐẶT KẾT NỐI API ==========================
    private static final String API_BASE = "http://192.168.0.100:8081/api";     // Địa chỉ server backend
    private static final String SESSION_PREFS = "zmnt_chat_session";        // Key lưu trữ Preferences

    // Lưu trữ thông tin đăng nhập cục bộ (Preferences của Java)
    private final Preferences prefs = Preferences.userRoot().node(SESSION_PREFS);

    // Client HTTP để gọi API (Java 11+ HttpClient)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Jackson ObjectMapper để parse JSON phản hồi từ server
    private final ObjectMapper mapper = new ObjectMapper();

    // ========================== CÁC THÀNH PHẦN GIAO DIỆN (FXML) ==========================
    @FXML private StackPane root;                    // Container chính của màn hình
    @FXML private MediaView backgroundVideo;        // Hiển thị video nền động

    // Các trường nhập liệu thông tin cá nhân
    @FXML private TextField usernameField;           // Tên đăng nhập
    @FXML private TextField emailField;              // Email
    @FXML private TextField displayNameField;        // Tên hiển thị (nickname)
    @FXML private TextField firstNameField;          // Họ
    @FXML private TextField lastNameField;           // Tên

    // Mật khẩu (có 2 chế độ: ẩn và hiện)
    @FXML private PasswordField passwordField;       // Trường mật khẩu ẩn
    @FXML private PasswordField confirmPasswordField;// Trường xác nhận mật khẩu ẩn
    @FXML private TextField passwordVisibleField;    // Trường hiển thị mật khẩu dạng text
    @FXML private TextField confirmPasswordVisibleField;

    // Nút chuyển đổi hiển thị/ẩn mật khẩu
    @FXML private Button togglePasswordBtn;
    @FXML private Button toggleConfirmBtn;

    @FXML private Button registerButton;             // Nút "Đăng ký"

    @FXML private Label messageLabel;                // Hiển thị thông báo thành công/lỗi

    // Ngày sinh và giới tính
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;

    // Nhóm radio button giới tính
    private final ToggleGroup genderGroup = new ToggleGroup();

    // Trình phát video nền
    private MediaPlayer mediaPlayer;

    // ========================== KHỞI TẠO GIAO DIỆN ==========================
    @FXML
    public void initialize() {
        // Không tự động thoát ứng dụng khi đóng cửa sổ này (dùng cho nhiều màn hình)
        Platform.setImplicitExit(false);

        // Đồng bộ nội dung giữa PasswordField và TextField (để hiện/ẩn mật khẩu)
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        // Thiết lập nhóm radio button giới tính
        maleRadio.setToggleGroup(genderGroup);
        femaleRadio.setToggleGroup(genderGroup);
        otherRadio.setToggleGroup(genderGroup);
        otherRadio.setSelected(true); // Mặc định chọn "Khác"

        setupDatePicker();           // Cấu hình DatePicker hỗ trợ nhập tay
        showMessage("", false);      // Xóa thông báo cũ
        Platform.runLater(this::setupBackgroundVideo); // Tải video nền sau khi UI sẵn sàng
    }

    // ========================== CẤU HÌNH DATEPICKER ==========================
    private void setupDatePicker() {
        if (dateOfBirthPicker == null) return;

        dateOfBirthPicker.setEditable(true); // Cho phép nhập tay

        // Định dạng ngày dd/MM/yyyy, hỗ trợ parse linh hoạt
        dateOfBirthPicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(string.trim(), formatter);
                } catch (Exception e1) {
                    try {
                        return LocalDate.parse(string.trim()); // Thử định dạng ISO
                    } catch (Exception e2) {
                        return null;
                    }
                }
            }
        });

        // Khi mất focus → tự động parse lại giá trị người dùng nhập
        dateOfBirthPicker.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    dateOfBirthPicker.setValue(
                            dateOfBirthPicker.getConverter().fromString(dateOfBirthPicker.getEditor().getText())
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    // ========================== VIDEO NỀN ĐỘNG ==========================
    private void setupBackgroundVideo() {
        try {
            URL videoUrl = getClass().getResource("/video/background.mp4");
            if (videoUrl == null) {
                applyFallbackBackground(); // Không tìm thấy video → dùng nền màu
                return;
            }

            Media media = new Media(videoUrl.toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Lặp vô hạn
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setMute(true);                        // Tắt tiếng

            mediaPlayer.setOnReady(() -> {
                backgroundVideo.setMediaPlayer(mediaPlayer);
                // Fit video full màn hình
                backgroundVideo.fitWidthProperty().bind(root.widthProperty());
                backgroundVideo.fitHeightProperty().bind(root.heightProperty());
                backgroundVideo.setPreserveRatio(false);
                mediaPlayer.play();
            });

            mediaPlayer.setOnError(this::applyFallbackBackground);

        } catch (Exception e) {
            e.printStackTrace();
            applyFallbackBackground();
        }
    }

    // Dùng nền màu gradient nếu không load được video
    private void applyFallbackBackground() {
        Platform.runLater(() -> {
            root.setStyle("-fx-background-color: linear-gradient(to right, #000000 0%, #1a1a2e 70%, #16213e 100%);");
            if (backgroundVideo != null) backgroundVideo.setVisible(false);
        });
    }

    // ========================== HIỆN/ẨN MẬT KHẨU ==========================
    @FXML
    private void onTogglePassword() {
        toggleVisibility(passwordField, passwordVisibleField, togglePasswordBtn);
    }

    @FXML
    private void onToggleConfirmPassword() {
        toggleVisibility(confirmPasswordField, confirmPasswordVisibleField, toggleConfirmBtn);
    }

    // Chuyển đổi giữa PasswordField (ẩn) và TextField (hiện)
    private void toggleVisibility(PasswordField pf, TextField tf, Button btn) {
        boolean hidden = pf.isVisible();
        pf.setVisible(!hidden);
        pf.setManaged(!hidden);
        tf.setVisible(hidden);
        tf.setManaged(hidden);

        if (hidden) {
            tf.requestFocus();
            tf.positionCaret(tf.getText().length());
            btn.setText("Hidden"); // Ẩn
        } else {
            pf.requestFocus();
            pf.positionCaret(pf.getText().length());
            btn.setText("Visible"); // Hiện
        }
    }

    // ========================== XỬ LÝ NÚT ĐĂNG KÝ ==========================
    @FXML
    private void onRegister() {
        // Lấy dữ liệu từ các trường
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String displayName = displayNameField != null ? displayNameField.getText().trim() : "";
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // Kiểm tra các trường bắt buộc
        if (username.isEmpty() || email.isEmpty() || firstName.isEmpty() ||
                lastName.isEmpty() || displayName.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ các trường bắt buộc!", true);
            return;
        }

        // Ép DatePicker commit giá trị nếu người dùng nhập tay
        if (dateOfBirthPicker != null && dateOfBirthPicker.isEditable()) {
            try {
                String text = dateOfBirthPicker.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    LocalDate parsed = dateOfBirthPicker.getConverter().fromString(text);
                    dateOfBirthPicker.setValue(parsed);
                }
            } catch (Exception ignored) {}
        }

        // Kiểm tra ngày sinh không được là tương lai
        if (dateOfBirthPicker != null && dateOfBirthPicker.getValue() != null &&
                dateOfBirthPicker.getValue().isAfter(LocalDate.now())) {
            showMessage("Ngày sinh không thể là ngày tương lai!", true);
            return;
        }

        // Kiểm tra mật khẩu trùng khớp và đủ độ dài
        if (!password.equals(confirm)) {
            showMessage("Mật khẩu nhập lại không khớp!", true);
            return;
        }
        if (password.length() < 6) {
            showMessage("Mật khẩu phải có ít nhất 6 ký tự!", true);
            return;
        }

        // Vô hiệu hóa nút và hiển thị đang xử lý
        registerButton.setDisable(true);
        showMessage("Đang tạo tài khoản...", false);

        // Thực hiện đăng ký trên luồng nền để không block giao diện
        new Thread(() -> performRegister(username, password, email, displayName, firstName, lastName)).start();
    }

    // ========================== GỌI API ĐĂNG KÝ ==========================
    private void performRegister(String username, String password, String email,
                                 String displayName, String firstName, String lastName) {
        try {
            // Lấy ngày sinh (nếu có)
            String dateOfBirth = (dateOfBirthPicker != null && dateOfBirthPicker.getValue() != null)
                    ? dateOfBirthPicker.getValue().toString() : null;

            // Xác định giới tính
            String gender = maleRadio.isSelected() ? "MALE"
                    : femaleRadio.isSelected() ? "FEMALE" : "OTHER";

            // Tạo JSON body thủ công (có thể thay bằng ObjectMapper nếu có DTO)
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                    .append("\"username\":\"").append(escapeJson(username)).append("\",")
                    .append("\"email\":\"").append(escapeJson(email)).append("\",")
                    .append("\"password\":\"").append(escapeJson(password)).append("\",")
                    .append("\"displayName\":\"").append(escapeJson(displayName)).append("\",")
                    .append("\"firstName\":\"").append(escapeJson(firstName)).append("\",")
                    .append("\"lastName\":\"").append(escapeJson(lastName)).append("\",");

            if (dateOfBirth != null) {
                jsonBuilder.append("\"dateOfBirth\":\"").append(dateOfBirth).append("\",");
            }
            jsonBuilder.append("\"gender\":\"").append(gender).append("\"")
                    .append("}");

            String jsonBody = jsonBuilder.toString();

            // In ra console để debug
            System.out.println("=== REGISTER REQUEST JSON ===");
            System.out.println(jsonBody);
            System.out.println("=============================");

            // Tạo request POST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            // Gửi request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Xử lý phản hồi trên luồng JavaFX
            Platform.runLater(() -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    handleSuccessfulRegister(response.body(), username);
                } else {
                    handleFailedRegister(response.body(), response.statusCode());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showMessage("Không thể kết nối đến server!", true);
                registerButton.setDisable(false);
            });
        }
    }

    // ========================== XỬ LÝ ĐĂNG KÝ THÀNH CÔNG ==========================
    private void handleSuccessfulRegister(String responseBody, String username) {
        try {
            JsonNode rootNode = mapper.readTree(responseBody);
            String token = extractToken(rootNode);
            long userId = extractUserId(rootNode);
            String usernameFromResp = extractUsername(rootNode);

            if (token != null && !token.isEmpty()) {
                // Lưu session vào Preferences và bộ nhớ tạm
                saveSessionToLocalStorage(token, userId, usernameFromResp != null ? usernameFromResp : username);
                SessionStore.setSessionToken(token);
                SessionStore.setUserId(userId);
                SessionStore.setUsername(usernameFromResp != null ? usernameFromResp : username);
            }

            showMessage("Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.", false);
            registerButton.setDisable(true); // Không cho đăng ký lại

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Đăng ký thành công nhưng có lỗi xử lý phản hồi.", false);
        }
    }

    // ========================== XỬ LÝ ĐĂNG KÝ THẤT BẠI ==========================
    private void handleFailedRegister(String responseBody, int statusCode) {
        String errorMsg = "Đăng ký thất bại";
        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                JsonNode node = mapper.readTree(responseBody);
                if (node.has("message")) errorMsg = node.get("message").asText();
                else if (node.has("error")) errorMsg = node.get("error").asText();
            } catch (Exception ignored) {
                if (responseBody.length() < 200) errorMsg = responseBody;
            }
        }
        showMessage(errorMsg, true);
        registerButton.setDisable(false);
    }

    // ========================== TRÍCH XUẤT DỮ LIỆU TỪ JSON ==========================
    private String extractToken(JsonNode root) {
        JsonNode tokenNode = root.path("token");
        return tokenNode.isTextual() ? tokenNode.asText() : null;
    }

    private long extractUserId(JsonNode root) {
        JsonNode userNode = root.path("user").path("id");
        return userNode.isLong() ? userNode.asLong() : 0L;
    }

    private String extractUsername(JsonNode root) {
        JsonNode userNode = root.path("user").path("username");
        return userNode.isTextual() ? userNode.asText() : null;
    }

    // ========================== LƯU SESSION VÀO MÁY ==========================
    private void saveSessionToLocalStorage(String token, long userId, String username) {
        prefs.put("zmnt_session_token", token);
        prefs.putLong("zmnt_user_id", userId);
        prefs.put("zmnt_username", username);
        prefs.putLong("zmnt_login_time", System.currentTimeMillis());
    }

    // ========================== CHUYỂN VỀ MÀN HÌNH ĐĂNG NHẬP ==========================
    @FXML
    private void onLoginLink() {
        dispose(); // Giải phóng tài nguyên video
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close(); // Đóng cửa sổ đăng ký → thường sẽ mở cửa sổ đăng nhập ở nơi khác
    }

    // ========================== HIỂN THỊ THÔNG BÁO ==========================
    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #6bff80;");
    }

    // ========================== THOÁT CHUẨN JSON (ESCAPE) ==========================
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ========================== GIẢI PHÓNG TÀI NGUYÊN ==========================
    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}