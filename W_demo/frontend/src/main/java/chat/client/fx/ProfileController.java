package chat.client.fx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;


import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import javafx.scene.layout.BorderPane;


public class ProfileController {

    private Stage primaryStage;
    private Runnable logoutCallback;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final ExecutorService executor;
    private StackPane mainContentPane;
    private ChatMainController mainController;

    public ProfileController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Profile-Worker-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }


    public void setMainController(ChatMainController mainController) {
        this.mainController = mainController;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setLogoutCallback(Runnable callback) {
        this.logoutCallback = callback;
    }

    public void setMainContentPane(StackPane mainContentPane) {
        this.mainContentPane = mainContentPane;
    }

    private static final String API_BASE = "http://192.192.192.192:8081/api";

    // ==================== FXML ELEMENTS ====================
    @FXML private StackPane root;

    @FXML private ImageView avatarImage;
    @FXML private Circle avatarClip;
    @FXML private ImageView coverImage;

    @FXML private Label displayNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label bioLabel;
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label genderLabel;
    @FXML private Label dobLabel;
    @FXML private Label lastActiveLabel;
    @FXML private Label verifiedLabel;
    @FXML private Label userIdLabel;
    @FXML private Hyperlink websiteLink;

    @FXML private VBox viewInfoPane;
    @FXML private VBox editInfoPane;
    @FXML private TextField displayNameField;
    @FXML private TextField bioField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField websiteField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private DatePicker dobPicker;

    @FXML private DialogPane changePassDialog;
    @FXML private PasswordField oldPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label passErrorLabel;


    @FXML private StackPane avatarContainer;
    @FXML private StackPane coverStackPane;


    @FXML private Button btnHomeNav;
    @FXML private Button changeCoverButton;

    // ==================== Phương Thức Initialize ====================
    @FXML
    public void initialize() {
        setupUI();
        setupSizeBehavior();
        loadProfile();
    }

    // ==================== Các Phương Thức Cấu Hình UI ====================
    private void setupUI() {
        String cssPath = "/styles/profile.css";
        String cssUrl = getClass().getResource(cssPath) != null
                ? getClass().getResource(cssPath).toExternalForm()
                : null;
        if (cssUrl != null) {
            root.getStylesheets().add(cssUrl);
        }

        // Setup avatar với clip tròn
        if (avatarImage != null) {
            Circle clip = new Circle(75, 75, 75);
            avatarImage.setClip(clip);
            avatarImage.setFitWidth(150);
            avatarImage.setFitHeight(150);
            avatarImage.setPreserveRatio(false);
        }


        if (avatarContainer != null) {
            System.out.println("Setting up avatarContainer click");
            avatarContainer.setCursor(javafx.scene.Cursor.HAND);
            avatarContainer.setOnMouseClicked(e -> {
                System.out.println("Avatar container clicked!");
                e.consume(); // Ngăn event lan ra ngoài
                onChangeAvatar();
            });

            // Debug: In ra khi mouse enter/exit
            avatarContainer.setOnMouseEntered(e -> {
                System.out.println("Mouse entered avatar container");
                avatarContainer.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(37, 211, 102, 0.6), 15, 0, 0, 0);");
            });

            avatarContainer.setOnMouseExited(e -> {
                System.out.println("Mouse exited avatar container");
                avatarContainer.setStyle("-fx-cursor: hand; -fx-effect: null;");
            });
        } else {
            System.err.println("avatarContainer is NULL!");
        }


        // ảnh bìa
        if (coverImage != null && coverStackPane != null) {
            coverImage.setCursor(javafx.scene.Cursor.HAND);
            coverImage.setOnMouseClicked(e -> onChangeCover());
            coverImage.setPreserveRatio(false);
            coverImage.setSmooth(true);

            // Thêm binding đơn giản cho sizing cắt ảnh
            coverImage.fitWidthProperty().bind(coverStackPane.widthProperty());
            coverImage.fitHeightProperty().bind(coverStackPane.heightProperty());

            coverStackPane.widthProperty().addListener((obs, oldW, newW) -> {
                Image currentImg = coverImage.getImage();
                if (currentImg != null && !currentImg.isError()) {
                    autoCropCover(currentImg);
                }
            });

            coverStackPane.heightProperty().addListener((obs, oldH, newH) -> {
                Image currentImg = coverImage.getImage();
                if (currentImg != null && !currentImg.isError()) {
                    autoCropCover(currentImg);
                }
            });
        }


        // Setup gender combo box
        genderComboBox.getItems().setAll("Nam", "Nữ", "Khác");
        genderComboBox.setValue("Khác");

        // Setup date picker
        dobPicker.setConverter(new StringConverter<>() {
            final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override public String toString(LocalDate date) {
                return date != null ? fmt.format(date) : "";
            }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s, fmt); }
                catch (Exception e) { return null; }
            }
        });
    }

    private void showLoadingState() {
        Platform.runLater(() -> {
            displayNameLabel.setText("Đang tải...");
            usernameLabel.setText("@...");
            bioLabel.setText("Đang tải thông tin cá nhân...");
            firstNameLabel.setText("...");
            lastNameLabel.setText("...");
            emailLabel.setText("...");
            genderLabel.setText("...");
            dobLabel.setText("...");
            lastActiveLabel.setText("...");
            verifiedLabel.setText("...");
            userIdLabel.setText("ID người dùng: ...");
        });
    }

    private void setupSizeBehavior() {
        if (root != null) {
            // Đảm bảo view có thể mở rộng theo cửa sổ
            root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }

    // ==================== Các Phương Thức Tải Profile ====================
    private void loadProfile() {
        String token = SessionStore.getSessionToken();
        if (token == null || token.isEmpty()) {
            redirectToLogin();
            return;
        }

        executor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/users/me"))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        parseAndDisplayProfile(response.body());
                    } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                        showAlert("Phiên hết hạn", "Vui lòng đăng nhập lại.");
                        SessionStore.clearSession();
                        redirectToLogin();
                    } else {
                        showError("Lỗi tải hồ sơ", "Mã lỗi: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        showError("Lỗi kết nối", "Không thể kết nối đến server."));
            }
        });
    }

    private void parseAndDisplayProfile(String json) {
        try {
            JsonNode node = mapper.readTree(json);

            SessionStore.setUserId(node.path("userId").asLong(0L));
            userIdLabel.setText("ID người dùng: " + node.path("userId").asLong());

            displayNameLabel.setText(getText(node, "displayName", "Chưa đặt tên hiển thị"));
            displayNameField.setText(node.path("displayName").asText(""));

            usernameLabel.setText("@" + getText(node, "username", "unknown"));

            bioLabel.setText(getText(node, "bio", "Chưa có tiểu sử"));
            bioField.setText(node.path("bio").asText(""));

            firstNameLabel.setText(getText(node, "firstName", "Chưa đặt"));
            firstNameField.setText(node.path("firstName").asText(""));

            lastNameLabel.setText(getText(node, "lastName", "Chưa đặt"));
            lastNameField.setText(node.path("lastName").asText(""));

            emailLabel.setText(getText(node, "email", "Chưa có"));

            String gender = node.path("gender").asText("OTHER").toUpperCase();
            String genderText = switch (gender) {
                case "MALE" -> "Nam";
                case "FEMALE" -> "Nữ";
                default -> "Khác";
            };
            genderLabel.setText(genderText);
            genderComboBox.setValue(genderText);

            String dobStr = node.path("dateOfBirth").asText();
            if (dobStr.isBlank() || "null".equalsIgnoreCase(dobStr)) {
                dobLabel.setText("Chưa đặt");
                dobPicker.setValue(null);
            } else {
                try {
                    LocalDate date = LocalDate.parse(dobStr);
                    dobLabel.setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    dobPicker.setValue(date);
                } catch (Exception e) {
                    dobLabel.setText("Không hợp lệ");
                }
            }

            String website = node.path("website").asText("").trim();
            websiteField.setText(website);
            if (website.matches("^https?://.*")) {
                websiteLink.setText(website);
                websiteLink.setOnAction(e -> copyLinkToClipboard(website));
            } else {
                websiteLink.setText("Chưa có");
                websiteLink.setOnAction(null);
            }

            verifiedLabel.setText(node.path("verified").asBoolean()
                    ? "Đã xác thực" : "Chưa xác thực");
            verifiedLabel.setStyle(node.path("verified").asBoolean()
                    ? "-fx-text-fill: #25D366;" : "-fx-text-fill: #888;");

            String lastActive = node.path("lastActive").asText("");
            lastActiveLabel.setText(lastActive.isEmpty()
                    ? "Chưa từng hoạt động"
                    : "Hoạt động từ " + formatTimeAgo(Instant.parse(lastActive)));

            safeLoadImage(convertToFullUrl(node.path("avatarUrl").asText("")),
                    avatarImage, "/images/default-avatar.png");
            safeLoadImage(convertToFullUrl(node.path("coverUrl").asText("")),
                    coverImage, "/images/default-cover.jpg");

        } catch (Exception e) {
            showError("Lỗi dữ liệu", "Không thể đọc thông tin từ server.");
        }
    }

    private String getText(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() || "null".equals(value) ? fallback : value;
    }

    // ==================== SAVE PROFILE ====================
    @FXML
    private void onSaveProfile() {
        if (displayNameField.getText().trim().isEmpty()) {
            showAlert("Lỗi", "Tên hiển thị không được để trống!");
            return;
        }

        ObjectNode json = mapper.createObjectNode();
        json.put("displayName", displayNameField.getText().trim());
        json.put("bio", bioField.getText().trim());
        json.put("firstName", firstNameField.getText().trim());
        json.put("lastName", lastNameField.getText().trim());
        json.put("gender", mapGender(genderComboBox.getValue()));
        json.put("website", websiteField.getText().trim());

        LocalDate dob = dobPicker.getValue();
        if (dob != null) json.put("dateOfBirth", dob.toString());
        else json.set("dateOfBirth", mapper.nullNode());

        executor.submit(() -> sendPatchRequest(API_BASE + "/users/me", json, () -> {
            showAlert("Thành công", "Cập nhật hồ sơ thành công!");
            editInfoPane.setVisible(false);
            viewInfoPane.setVisible(true);
            loadProfile();
        }));
    }
    //


    // Trong ProfileController.java

    @FXML
    private void onHome(ActionEvent event) {
        // Lấy Stage hiện tại và đóng nó
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }

    @FXML
    private void goToProfile(ActionEvent event) {
        System.out.println("Already on Profile page");
    }

    // ==================== UPLOAD MEDIA ====================
    @FXML
    private void onChangeAvatar() {
        uploadAndSetMedia("AVATAR", avatarImage);
    }

    @FXML
    private void onChangeCover() {
        uploadAndSetMedia("COVER", coverImage);
    }

    private void uploadAndSetMedia(String mediaType, ImageView target) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn " + (mediaType.equals("AVATAR") ? "ảnh đại diện" : "ảnh bìa"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Hình ảnh",
                        "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );

        File file = fc.showOpenDialog(primaryStage);
        if (file == null) return;

        Platform.runLater(() -> {
            if (mediaType.equals("AVATAR")) {
                displayNameLabel.setText("Đang tải...");
            } else {
                bioLabel.setText("Đang tải...");
            }
        });

        executor.submit(() -> {
            try {
                // STEP 1: Upload file
                String boundary = "----Boundary" + System.currentTimeMillis();
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String mime = Files.probeContentType(file.toPath());
                if (mime == null) mime = "image/jpeg";

                StringBuilder sb = new StringBuilder();
                sb.append("--").append(boundary).append("\r\n");
                // Giữ nguyên tên "file" cho phần file, đây là một quy ước phổ biến
                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(file.getName()).append("\"\r\n");
                sb.append("Content-Type: ").append(mime).append("\r\n\r\n");
                byte[] header = sb.toString().getBytes();

                StringBuilder sb2 = new StringBuilder();
                sb2.append("\r\n--").append(boundary).append("--\r\n");
                byte[] footer = sb2.toString().getBytes();

                HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofByteArrays(
                        List.of(header, fileBytes, footer)
                );

                // THAY ĐỔI: Thêm mediaType vào URL dưới dạng tham số truy vấn
                String uploadUrl = API_BASE + "/media/upload?mediaType=" + mediaType;
                HttpRequest uploadReq = HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .header("Authorization", "Bearer " + SessionStore.getSessionToken())
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(body)
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> uploadResp = httpClient.send(uploadReq,
                        HttpResponse.BodyHandlers.ofString());

                if (uploadResp.statusCode() < 200 || uploadResp.statusCode() >= 300) {
                    Platform.runLater(() -> {
                        showError("Upload thất bại",
                                "Status: " + uploadResp.statusCode() + "\n" + uploadResp.body());
                        loadProfile();
                    });
                    return;
                }

                JsonNode mediaNode = mapper.readTree(uploadResp.body());
                long mediaId = mediaNode.path("id").asLong();
                String fileUrl = mediaNode.path("fileUrl").asText();

                // STEP 2: Set as avatar/cover
                String setUrl = mediaType.equals("AVATAR")
                        ? API_BASE + "/users/me/avatar?mediaId=" + mediaId
                        : API_BASE + "/users/me/cover?mediaId=" + mediaId;

                HttpRequest setReq = HttpRequest.newBuilder()
                        .uri(URI.create(setUrl))
                        .header("Authorization", "Bearer " + SessionStore.getSessionToken())
                        .method("PATCH", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> setResp = httpClient.send(setReq,
                        HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (setResp.statusCode() >= 200 && setResp.statusCode() < 300) {
                        showAlert("Thành công",
                                "Đã cập nhật " +
                                        (mediaType.equals("AVATAR") ? "avatar" : "ảnh bìa") + "!");
                        loadProfile();
                    } else {
                        showError("Lỗi", "Status: " + setResp.statusCode());
                        loadProfile();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Lỗi", e.getMessage());
                    loadProfile();
                });
            }
        });
    }

    // ==================== CHANGE PASSWORD ====================
    @FXML
    private void onChangePassword() {
        oldPassField.clear();
        newPassField.clear();
        confirmPassField.clear();
        passErrorLabel.setText("");
        changePassDialog.setVisible(true);
    }

    @FXML
    private void onCancelChangePassword() {
        changePassDialog.setVisible(false);
    }

    @FXML
    private void onConfirmChangePassword() {
        String oldPass = oldPassField.getText();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        passErrorLabel.setText("");
        if (oldPass.isBlank() || newPass.isBlank() || confirm.isBlank()) {
            passErrorLabel.setText("Vui lòng điền đầy đủ!");
            return;
        }
        if (newPass.length() < 6) {
            passErrorLabel.setText("Mật khẩu mới ít nhất 6 ký tự!");
            return;
        }
        if (!newPass.equals(confirm)) {
            passErrorLabel.setText("Mật khẩu xác nhận không khớp!");
            return;
        }
        if (oldPass.equals(newPass)) {
            passErrorLabel.setText("Mật khẩu mới không được trùng cũ!");
            return;
        }

        ObjectNode json = mapper.createObjectNode();
        json.put("oldPassword", oldPass);
        json.put("newPassword", newPass);

        executor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/users/me/password"))
                        .header("Authorization", "Bearer " + SessionStore.getSessionToken())
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(
                                mapper.writeValueAsString(json)))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> resp = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                        // --- THÀNH CÔNG ---
                        showAlert("Thành công", "Đổi mật khẩu thành công! Đang đăng xuất...");
                        changePassDialog.setVisible(false);

                        // Thêm độ trễ 2 giây để người dùng đọc thông báo
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    // Dọn dẹp session và gọi callback đăng xuất
                                    SessionStore.clearSession();
                                    redirectToLogin();
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        // --- THẤT BẠI ---
                        showError("Đổi mật khẩu thất bại", resp.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> passErrorLabel.setText("Lỗi kết nối"));
            }
        });
    }

    // ==================== EDIT/CANCEL ====================
    @FXML
    private void onEditProfile() {
        viewInfoPane.setVisible(false);
        editInfoPane.setVisible(true);
    }

    @FXML
    private void onCancelEdit() {
        editInfoPane.setVisible(false);
        viewInfoPane.setVisible(true);
        loadProfile();
    }

    // ==================== LOGOUT ====================
    @FXML
    private void onLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn đăng xuất?", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Đăng xuất");
        if (primaryStage != null) alert.initOwner(primaryStage);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (logoutCallback != null) {
                    logoutCallback.run(); // Gọi callback để hiển thị màn hình đăng nhập
                }
                // THÊM DÒNG NÀY ĐỂ ĐÓNG CỬA SỔ PROFILE
                if (primaryStage != null) {
                    primaryStage.close();
                }
            }
        });
    }

    private void redirectToLogin() {
        if (logoutCallback != null) {
            logoutCallback.run();
        }
    }

    // ==================== HELPER METHODS ====================
    private String convertToFullUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || "null".equals(fileUrl)) {
            return null;
        }

        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return fileUrl;
        }

        String base = API_BASE.replace("/api", "");

        if (fileUrl.startsWith("/api/")) {
            return base + fileUrl;
        }

        if (fileUrl.startsWith("/uploads/")) {
            return base + "/api" + fileUrl;
        }

        if (fileUrl.startsWith("/")) {
            return base + fileUrl;
        }

        return base + "/api/uploads/" + fileUrl;
    }

    private void safeLoadImage(String url, ImageView iv, String fallbackPath) {
        if (url == null || url.isBlank()) {
            loadResourceImage(iv, fallbackPath);
            return;
        }

        try {
            Image img = new Image(url, true);

            img.errorProperty().addListener((obs, was, is) -> {
                if (is) {
                    System.err.println("Image error: " + url);
                    loadResourceImage(iv, fallbackPath);
                }
            });

            img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0 && !img.isError()) {
                    Platform.runLater(() -> {
                        iv.setImage(img);
                        // Avatar sẽ tự động crop qua cropImageToFit
                        if (iv == avatarImage) {
                            cropImageToFit(iv, img);
                        }
                        // Cover sẽ tự động crop qua listener
                    });
                }
            });

            iv.setImage(img);
            if (img.getProgress() >= 1.0 && !img.isError()) {
                if (iv == avatarImage) {
                    cropImageToFit(iv, img);
                }
            }

        } catch (Exception e) {
            System.err.println("Exception loading: " + url);
            e.printStackTrace();
            loadResourceImage(iv, fallbackPath);
        }
    }

    private void cropImageToFit(ImageView iv, Image img) {
        if (img == null || img.isError()) return;

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();

        if (imgWidth == 0 || imgHeight == 0) return;

        // CHỈ XỬ LÝ AVATAR - Cover tự động qua listener
        if (iv == avatarImage) {
            double size = Math.min(imgWidth, imgHeight);
            double offsetX = (imgWidth - size) / 2;
            double offsetY = (imgHeight - size) / 2;

            Rectangle2D viewport = new Rectangle2D(offsetX, offsetY, size, size);
            iv.setViewport(viewport);

            System.out.println("Avatar cropped: " + imgWidth + "x" + imgHeight + " -> " + size + "x" + size);
        }
    }

    // GIẢI PHÁP SỬA LỖI - Xóa binding và dùng viewport
    private void autoCropCover(Image img) {
        if (img == null || coverImage == null || coverStackPane == null) return;

        double imgW = img.getWidth();
        double imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        // Lấy kích thước container hiện tại
        double containerW = coverStackPane.getWidth();
        double containerH = coverStackPane.getHeight();

        if (containerW <= 0 || containerH <= 0) {
            containerW = 1280; // Fallback 16:9
            containerH = 350;
        }

        // Tỷ lệ container (không bắt buộc 16:9, dùng tỷ lệ thực tế của container)
        double containerRatio = containerW / containerH;
        double imgRatio = imgW / imgH;

        double viewportW, viewportH, offsetX, offsetY;

        // Logic crop ĐÚNG để fill container không bị méo
        if (imgRatio > containerRatio) {
            // Ảnh rộng hơn → Crop ngang (center)
            viewportH = imgH;
            viewportW = imgH * containerRatio;
            offsetX = (imgW - viewportW) / 2;
            offsetY = 0;
        } else {
            // Ảnh cao hơn → Crop dọc (center)
            viewportW = imgW;
            viewportH = imgW / containerRatio;
            offsetX = 0;
            offsetY = (imgH - viewportH) / 2;
        }

        // Đảm bảo viewport hợp lệ
        viewportW = Math.min(viewportW, imgW);
        viewportH = Math.min(viewportH, imgH);

        // UNBIND trước khi set
        coverImage.fitWidthProperty().unbind();
        coverImage.fitHeightProperty().unbind();

        // Áp dụng viewport và sizing
        coverImage.setViewport(new Rectangle2D(offsetX, offsetY, viewportW, viewportH));
        coverImage.setFitWidth(containerW);
        coverImage.setFitHeight(containerH);
        coverImage.setPreserveRatio(false); // QUAN TRỌNG: false để fill đủ không méo
        coverImage.setSmooth(true);

        // BIND lại sau khi set xong
        coverImage.fitWidthProperty().bind(coverStackPane.widthProperty());
        coverImage.fitHeightProperty().bind(coverStackPane.heightProperty());

        System.out.println("Cover cropped: img=" + imgW + "x" + imgH +
                " viewport=" + viewportW + "x" + viewportH +
                " container=" + containerW + "x" + containerH);
    }

    private void loadResourceImage(ImageView iv, String path) {
        try {
            var resource = getClass().getResource(path);
            if (resource != null) {
                Image img = new Image(resource.toExternalForm());
                iv.setImage(img);
                cropImageToFit(iv, img);
            }
        } catch (Exception e) {
            System.err.println("Failed to load fallback: " + path);
        }
    }

    private void sendPatchRequest(String url, ObjectNode json, Runnable onSuccess) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionStore.getSessionToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(json)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                Platform.runLater(onSuccess);
            } else {
                Platform.runLater(() -> showError("Lỗi", resp.body()));
            }
        } catch (Exception e) {
            Platform.runLater(() -> showError("Lỗi kết nối", "Không thể cập nhật."));
        }
    }

    private String mapGender(String vn) {
        return switch (vn == null ? "" : vn) {
            case "Nam" -> "MALE";
            case "Nữ" -> "FEMALE";
            default -> "OTHER";
        };
    }

    private String formatTimeAgo(Instant i) {
        Duration d = Duration.between(i, Instant.now());
        long m = d.toMinutes();
        if (m < 1) return "vừa xong";
        if (m < 60) return m + " phút trước";
        long h = d.toHours();
        if (h < 24) return h + " giờ trước";
        long days = d.toDays();
        if (days < 7) return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        if (days < 365) return (days / 30) + " tháng trước";
        return (days / 365) + " năm trước";
    }

    private void copyLinkToClipboard(String url) {
        Clipboard.getSystemClipboard().setContent(
                new ClipboardContent() {{ putString(url); }});
        showAlert("Đã copy!", "Link đã được copy vào clipboard");
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.initOwner(primaryStage);
            a.showAndWait();
        });
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.initOwner(primaryStage);
            a.showAndWait();
        });
    }

    public void dispose() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}