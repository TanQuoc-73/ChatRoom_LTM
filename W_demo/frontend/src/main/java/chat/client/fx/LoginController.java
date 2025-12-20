package chat.client.fx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;


import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.prefs.Preferences;

import javafx.stage.Modality;
import javafx.scene.layout.VBox;


public class LoginController {

    private static final String API_BASE = "http://192.168.0.100:8081/api";
    private static final String SESSION_PREFS = "zmnt_chat_session";

    private Stage primaryStage;
    private Preferences prefs;

    @FXML private StackPane root;
    @FXML private MediaView backgroundVideo;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button loginButton;
    @FXML private Button togglePasswordBtn;
    @FXML private Label messageLabel;
        @FXML
    private Hyperlink forgotPasswordLink;
    @FXML
    private Hyperlink registerLink;

    private MediaPlayer mediaPlayer;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        this.prefs = Preferences.userRoot().node(SESSION_PREFS);
        loadSavedCredentials();

        // THÊM: Dispose video khi đóng cửa sổ
        stage.setOnCloseRequest(e -> {
            dispose();
        });
    }

    private void setupBackgroundVideo() {
        try {
            URL videoUrl = null;
            String[] possiblePaths = {
                    "/video/background.mp4"
            };

            for (String path : possiblePaths) {
                videoUrl = getClass().getResource(path);
                if (videoUrl != null) {
                    System.out.println(" Tìm thấy video tại: " + path);
                    break;
                }

                File file = new File(path);
                if (file.exists()) {
                    videoUrl = file.toURI().toURL();
                    System.out.println(" Tìm thấy video từ file: " + file.getAbsolutePath());
                    break;
                }
            }

            if (videoUrl == null) {
                System.err.println(" Không tìm thấy video background.mp4");
                // Fallback: gradient background
                root.setStyle("-fx-background-color: linear-gradient(to right, #000000 0%, #1a1a2e 70%, #16213e 100%);");
                return;
            }

            Media media = new Media(videoUrl.toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setMute(true);

            mediaPlayer.setOnReady(() -> {
                // Đặt opacity 0.7 như web
                backgroundVideo.setOpacity(1.0);

                // Fill toàn màn hình
                backgroundVideo.fitWidthProperty().bind(root.widthProperty());
                backgroundVideo.fitHeightProperty().bind(root.heightProperty());
                backgroundVideo.setPreserveRatio(false);

                mediaPlayer.play();
            });

            mediaPlayer.setOnError(() -> {
                System.err.println("Lỗi video: " + mediaPlayer.getError());
                Platform.runLater(() -> {
                    root.setStyle("-fx-background-color: linear-gradient(to right, #000000 0%, #1a1a2e 70%, #16213e 100%);");
                    backgroundVideo.setVisible(false);
                });
            });

            backgroundVideo.setMediaPlayer(mediaPlayer);

        } catch (Exception e) {
            e.printStackTrace();
            root.setStyle("-fx-background-color: linear-gradient(to right, #000000 0%, #1a1a2e 70%, #16213e 100%);");
        }
    }
    @FXML
    public void initialize() {
        // Sync password fields
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        showMessage("", false);

        // Setup video background
        setupBackgroundVideo();

        // Setup enter key for login
        setupEnterKeyHandler();
    }

    private void setupEnterKeyHandler() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> onLogin());
        passwordVisibleField.setOnAction(e -> onLogin());
    }

    private void loadSavedCredentials() {
        String savedUser = prefs.get("username", "");
        if (!savedUser.isEmpty()) {
            usernameField.setText(savedUser);
            passwordField.requestFocus();
        }
    }

    private void saveCredentials(String username) {
        prefs.put("username", username);
    }

    @FXML
    private void onTogglePassword() {
        boolean isPasswordHidden = passwordField.isVisible();

        if (isPasswordHidden) {
            // Switch to visible text
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            togglePasswordBtn.setText("🙈");
        } else {
            // Switch to password dots
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", true);
            return;
        }

        loginButton.setDisable(true);
        showMessage("Đang đăng nhập...", false);

        // Save username for next time
        saveCredentials(username);

        new Thread(() -> performLogin(username, password)).start();
    }

    private void performLogin(String username, String password) {
        try {
            String jsonBody = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\",\"deviceType\":\"DESKTOP\"}",
                    escapeJson(username),
                    escapeJson(password)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Login response status: " + response.statusCode());
            System.out.println("Login response body: " + response.body());

            Platform.runLater(() -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    handleSuccessfulLogin(response.body(), username);
                } else {
                    handleFailedLogin(response.body(), response.statusCode());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showMessage("Không thể kết nối đến server!", true);
                loginButton.setDisable(false);
            });
        }
    }

    private String extractRole(JsonNode root) {
    if (root == null) return "USER";

    if (root.has("role")) return root.get("role").asText();

    if (root.has("data")) {
        JsonNode data = root.get("data");
        if (data.has("role")) return data.get("role").asText();

        if (data.has("user") && data.get("user").has("role")) {
            return data.get("user").get("role").asText();
        }
    }
    return "USER";
}

    private void handleSuccessfulLogin(String responseBody, String username) {
        try {
            JsonNode root = mapper.readTree(responseBody);

            // Extract token using same logic as web version
            String token = extractToken(root);
            long userId = extractUserId(root);
            String usernameFromResp = extractUsername(root);
            String role = extractRole(root);

            System.out.println("Extracted token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            System.out.println("Extracted userId: " + userId);
            System.out.println("Extracted username: " + usernameFromResp);

            if (token == null || token.isEmpty()) {
                showMessage("Không nhận được token từ server!", true);
                loginButton.setDisable(false);
                return;
            }

            // Save session using same keys as web version
            saveSessionToLocalStorage(token, userId, usernameFromResp != null ? usernameFromResp : username);

            // Also save to JavaFX SessionStore
            SessionStore.setSessionToken(token);
            SessionStore.setUserId(userId);
            SessionStore.setUsername(usernameFromResp != null ? usernameFromResp : username);
            SessionStore.setRole(role);
            System.out.println("Extracted role: " + role);

            showMessage("Đăng nhập thành công! Đang chuyển hướng...", false);

            // Open main chat window after delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Wait 1 second to show success message
                    Platform.runLater(() -> openMainWindow());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Lỗi xử lý phản hồi từ server!", true);
            loginButton.setDisable(false);
        }
    }

    private void handleFailedLogin(String responseBody, int statusCode) {
        try {
            String errorMessage = "Đăng nhập thất bại (" + statusCode + ")";

            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonNode root = mapper.readTree(responseBody);
                    if (root.has("message")) errorMessage = root.get("message").asText();
                    else if (root.has("error")) errorMessage = root.get("error").asText();
                } catch (Exception e) {
                    // If not JSON, use raw text
                    if (responseBody.length() < 100) {
                        errorMessage = responseBody;
                    }
                }
            }

            showMessage(errorMessage, true);
            loginButton.setDisable(false);

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Đăng nhập thất bại!", true);
            loginButton.setDisable(false);
        }
    }

    private String extractToken(JsonNode root) {
        if (root == null) return null;

        // Try multiple possible locations like web version
        JsonNode tokenNode = null;

        // Check root level
        if (root.has("sessionToken")) tokenNode = root.get("sessionToken");
        else if (root.has("token")) tokenNode = root.get("token");

        // Check data object
        if (tokenNode == null && root.has("data")) {
            JsonNode data = root.get("data");
            if (data.has("sessionToken")) tokenNode = data.get("sessionToken");
            else if (data.has("token")) tokenNode = data.get("token");

            // Check nested session object
            if (tokenNode == null && data.has("session")) {
                JsonNode session = data.get("session");
                if (session.has("sessionToken")) tokenNode = session.get("sessionToken");
            }
        }

        return tokenNode != null && tokenNode.isTextual() ? tokenNode.asText() : null;
    }

    private long extractUserId(JsonNode root) {
        if (root == null) return 0L;

        JsonNode idNode = null;

        if (root.has("userId")) idNode = root.get("userId");

        if (idNode == null && root.has("data")) {
            JsonNode data = root.get("data");
            if (data.has("userId")) idNode = data.get("userId");

            if (idNode == null && data.has("session")) {
                JsonNode session = data.get("session");
                if (session.has("userId")) idNode = session.get("userId");
            }

            if (idNode == null && data.has("user")) {
                JsonNode user = data.get("user");
                if (user.has("id")) idNode = user.get("id");
            }
        }

        return idNode != null && idNode.isNumber() ? idNode.asLong() : 0L;
    }

    private String extractUsername(JsonNode root) {
        if (root == null) return null;

        JsonNode usernameNode = null;

        if (root.has("username")) usernameNode = root.get("username");

        if (usernameNode == null && root.has("data")) {
            JsonNode data = root.get("data");
            if (data.has("username")) usernameNode = data.get("username");

            if (usernameNode == null && data.has("user")) {
                JsonNode user = data.get("user");
                if (user.has("username")) usernameNode = user.get("username");
            }
        }

        return usernameNode != null && usernameNode.isTextual() ? usernameNode.asText() : null;
    }

    private void saveSessionToLocalStorage(String token, long userId, String username) {
        // Save to Preferences for desktop app persistence
        prefs.put("zmnt_session_token", token);
        prefs.putLong("zmnt_user_id", userId);
        prefs.put("zmnt_username", username);
        prefs.putLong("zmnt_login_time", System.currentTimeMillis());

        System.out.println("Session saved to preferences:");
        System.out.println("  Token: " + token.substring(0, Math.min(20, token.length())) + "...");
        System.out.println("  UserId: " + userId);
        System.out.println("  Username: " + username);
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat-main.fxml"));
            Parent root = loader.load();

            ChatMainController chatController = loader.getController();
            chatController.setPrimaryStage(primaryStage);  // ← truyền đúng Stage đang sống

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/chat-main.css").toExternalForm());

            // DÙNG LẠI primaryStage → KHÔNG tạo Stage mới!
            primaryStage.setScene(scene);
            primaryStage.setTitle("ZMNT Chat - " + SessionStore.getUsername());
            primaryStage.setMaximized(true);
            primaryStage.centerOnScreen();
            primaryStage.show();

            // Stop video background
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Lỗi khi mở cửa sổ chính!", true);
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void onRegister() {
        try {
            Platform.setImplicitExit(false);

            // Dùng đường dẫn tương đối từ file hiện tại
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/register-view.fxml"));
            Parent registerRoot = loader.load();

            RegisterController registerController = loader.getController();

            Stage registerStage = new Stage();
            Scene scene = new Scene(registerRoot, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

            registerStage.setTitle("Đăng Ký - ZMNT Chat");
            registerStage.setScene(scene);
            registerStage.initOwner(primaryStage);

            registerStage.setOnCloseRequest(e -> registerController.dispose());
            registerStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).showAndWait();
        }
    }


    @FXML
    private void onForgotPassword() {
        showForgotPasswordDialog();
    }

    private void showForgotPasswordDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Quên mật khẩu");
        dialog.initOwner(primaryStage);

        VBox root = new VBox(15);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: white;");

        // Title
        Label titleLabel = new Label("Đặt lại mật khẩu");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Instructions
        Label instructionLabel = new Label("Nhập email của bạn để nhận mã OTP");
        instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        instructionLabel.setWrapText(true);

        // Email field
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setPrefHeight(35);

        // OTP field (hidden initially)
        TextField otpField = new TextField();
        otpField.setPromptText("Mã OTP (6 số)");
        otpField.setPrefHeight(35);
        otpField.setVisible(false);
        otpField.setManaged(false);

        // New password field (hidden initially)
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Mật khẩu mới");
        newPasswordField.setPrefHeight(35);
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);

        // Confirm password field (hidden initially)
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Nhập lại mật khẩu mới");
        confirmPasswordField.setPrefHeight(35);
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setManaged(false);

        // Status label
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        // Send OTP button
        Button sendOtpBtn = new Button("Gửi mã OTP");
        sendOtpBtn.setPrefWidth(Double.MAX_VALUE);
        sendOtpBtn.setPrefHeight(40);
        sendOtpBtn.setStyle(
                "-fx-background-color: #2196F3;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 6px;");

        // Reset password button (hidden initially)
        Button resetBtn = new Button("Đặt lại mật khẩu");
        resetBtn.setPrefWidth(Double.MAX_VALUE);
        resetBtn.setPrefHeight(40);
        resetBtn.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 6px;");
        resetBtn.setVisible(false);
        resetBtn.setManaged(false);

        // Cancel button
        Button cancelBtn = new Button("Hủy");
        cancelBtn.setPrefWidth(Double.MAX_VALUE);
        cancelBtn.setPrefHeight(40);
        cancelBtn.setStyle(
                "-fx-background-color: #e0e0e0;" +
                        "-fx-text-fill: #333;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 6px;");

        root.getChildren().addAll(
                titleLabel, instructionLabel, emailField,
                otpField, newPasswordField, confirmPasswordField,
                statusLabel, sendOtpBtn, resetBtn, cancelBtn);

        // Send OTP action
        sendOtpBtn.setOnAction(e -> {
            String email = emailField.getText().trim();

            if (email.isEmpty()) {
                statusLabel.setText("Vui lòng nhập email!");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                statusLabel.setVisible(true);
                return;
            }

            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                statusLabel.setText("Email không hợp lệ!");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                statusLabel.setVisible(true);
                return;
            }

            sendOtpBtn.setDisable(true);
            statusLabel.setText("Đang gửi OTP...");
            statusLabel.setStyle("-fx-text-fill: #2196F3;");
            statusLabel.setVisible(true);

            new Thread(() -> {
                try {
                    String jsonBody = String.format("{\"email\":\"%s\"}", escapeJson(email));

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE + "/auth/forgot-password"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    Platform.runLater(() -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            statusLabel.setText("✓ OTP đã được gửi đến email của bạn!");
                            statusLabel.setStyle("-fx-text-fill: #4CAF50;");

                            // Show OTP and password fields
                            instructionLabel.setText("Nhập mã OTP và mật khẩu mới");
                            otpField.setVisible(true);
                            otpField.setManaged(true);
                            newPasswordField.setVisible(true);
                            newPasswordField.setManaged(true);
                            confirmPasswordField.setVisible(true);
                            confirmPasswordField.setManaged(true);

                            sendOtpBtn.setVisible(false);
                            sendOtpBtn.setManaged(false);
                            resetBtn.setVisible(true);
                            resetBtn.setManaged(true);

                            emailField.setDisable(true);
                        } else {
                            statusLabel.setText("Lỗi: " + response.body());
                            statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                            sendOtpBtn.setDisable(false);
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Không thể kết nối đến server!");
                        statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                        sendOtpBtn.setDisable(false);
                    });
                }
            }).start();
        });

        // Reset password action
        resetBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String otp = otpField.getText().trim();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                statusLabel.setText("Vui lòng điền đầy đủ thông tin!");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                statusLabel.setText("Mật khẩu xác nhận không khớp!");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }

            if (newPassword.length() < 6) {
                statusLabel.setText("Mật khẩu phải có ít nhất 6 ký tự!");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }

            resetBtn.setDisable(true);
            statusLabel.setText("Đang đặt lại mật khẩu...");
            statusLabel.setStyle("-fx-text-fill: #2196F3;");

            new Thread(() -> {
                try {
                    String jsonBody = String.format(
                            "{\"email\":\"%s\",\"otp\":\"%s\",\"newPassword\":\"%s\"}",
                            escapeJson(email), escapeJson(otp), escapeJson(newPassword));

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE + "/auth/reset-password"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    Platform.runLater(() -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("Thành công");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText(
                                    "Mật khẩu đã được đặt lại thành công!\nBạn có thể đăng nhập với mật khẩu mới.");
                            successAlert.showAndWait();
                            dialog.close();
                        } else {
                            statusLabel.setText("Lỗi: OTP không đúng hoặc đã hết hạn!");
                            statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                            resetBtn.setDisable(false);
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Không thể kết nối đến server!");
                        statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                        resetBtn.setDisable(false);
                    });
                }
            }).start();
        });

        cancelBtn.setOnAction(e -> dialog.close());

        Scene scene = new Scene(root, 400, 500);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #6bff80;");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}