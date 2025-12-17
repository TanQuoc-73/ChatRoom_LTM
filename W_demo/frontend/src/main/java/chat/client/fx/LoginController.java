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

public class LoginController {

    private static final String API_BASE = "http://localhost:8081/api";
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
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink registerLink;

    private MediaPlayer mediaPlayer;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        this.prefs = Preferences.userRoot().node(SESSION_PREFS);
        loadSavedCredentials();
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

    private void handleSuccessfulLogin(String responseBody, String username) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            // Extract token using same logic as web version
            String token = extractToken(root);
            long userId = extractUserId(root);
            String usernameFromResp = extractUsername(root);
            
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
            // Load main chat window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat-main.fxml"));
            Parent root = loader.load();
            
            Stage mainStage = new Stage();
            Scene scene = new Scene(root, 1200, 800);
            
            // Apply styles
            scene.getStylesheets().add(getClass().getResource("/styles/chat-main.css").toExternalForm());
            
            mainStage.setTitle("ZMNT Chat - " + SessionStore.getUsername());
            mainStage.setScene(scene);
            mainStage.setMaximized(true);
            
            // Close login window
            if (primaryStage != null) {
                primaryStage.close();
            }
            
            // Stop background video
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            
            mainStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Lỗi khi mở cửa sổ chính!", true);
            loginButton.setDisable(false);
        }
    }

    @FXML
    private void onRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register-view.fxml"));
            Parent registerRoot = loader.load();
            
            Stage registerStage = new Stage();
            Scene scene = new Scene(registerRoot, 500, 700);
            scene.getStylesheets().add(getClass().getResource("/styles/register.css").toExternalForm());
            
            registerStage.setTitle("Đăng Ký - ZMNT Chat");
            registerStage.setScene(scene);
            registerStage.initOwner(primaryStage);
            registerStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở màn hình đăng ký!");
            alert.showAndWait();
        }
    }

    @FXML
    private void onForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quên mật khẩu");
        alert.setHeaderText(null);
        alert.setContentText("Vui lòng liên hệ quản trị viên để được hỗ trợ.");
        alert.showAndWait();
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