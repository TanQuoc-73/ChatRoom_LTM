package chat.client.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import chat.client.fx.service.ChatService;
import chat.client.fx.model.Post;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableMap;
import javafx.scene.control.cell.CheckBoxListCell;
import java.io.ByteArrayInputStream;
import java.time.Instant;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;


public class ChatMainController implements Initializable {

    @FXML private Button btnHome, btnPicture, btnPlus, btnBell, btnUser, themeToggle;
    @FXML private Label themeIcon;
    @FXML private TextField searchField;
    @FXML private VBox searchResultPanel;
    @FXML private ListView<JsonNode> searchResultsList;
    @FXML private Button btnCreate, btnCollection, btnSettings;
    @FXML private ImageView profileAvatar;
    @FXML private Label profileName;
    @FXML private ListView<JsonNode> friendsList;
    @FXML private ListView<JsonNode> groupsList;
    @FXML private Button btnAdmin;
    @FXML private StackPane mainContentPane;
    @FXML private VBox feedView;


    // ====== FEED SYSTEM - ListView<Post> ======
    @FXML private ListView<Post> feedListView;
    private final ObservableList<Post> feedData = FXCollections.observableArrayList();

    // ====== BADGE LỜI MỜI KẾT BẠN ======
    @FXML private Label friendRequestBadge;
    @FXML private StackPane bellWrapper;

    private final ChatService chatService = ChatService.getInstance();
    private final Preferences prefs = Preferences.userRoot().node("zmnt_chat_session");
    private final ObjectMapper mapper;

    {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    private long currentUserId = 0L;
    private Stage primaryStage;
    private Runnable onLogoutCallback;

    private final Map<Long, ChatWindow> openChats = new HashMap<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private ScheduledExecutorService friendReloadScheduler;
    private ScheduledExecutorService heartbeatScheduler; // Heartbeat mới thêm
    private String currentTheme = "light";

    private interface ChatWindow {
        void show();
        void toFront();
        void close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchPanel();
        setupListViews();
        setupFeedListView();

        String theme = prefs.get("zmnt_theme", "light");
        applyTheme(theme);

        // Badge: load lần đầu khi scene sẵn sàng
        bellWrapper.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::loadFriendRequestCount);
            }
        });
        

        Platform.runLater(() -> {
            checkComponents();
            new Thread(this::loadProfileSafe).start();
            new Thread(this::loadFriendsSafe).start();
            new Thread(this::loadGroupsSafe).start();
            new Thread(this::loadFeedSafe).start();
        });

        startFriendListAutoReload();
        startHeartbeat(); // Khởi động heartbeat
    }

    // ====== HEARTBEAT VALIDATE TOKEN ======
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                chatService.get("/auth/validate");
            } catch (Exception ignored) {
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    // ====== BADGE SỐ LƯỢNG LỜI MỜI KẾT BẠN ======
    private void loadFriendRequestCount() {
        if (currentUserId == 0) return;
        new Thread(() -> {
            try {
                JsonNode arr = chatService.get("/friends/requests?currentUserId=" + currentUserId);
                int count = (arr != null && arr.isArray()) ? arr.size() : 0;
                Platform.runLater(() -> {
                    if (count > 0) {
                        friendRequestBadge.setText(String.valueOf(count));
                        friendRequestBadge.setVisible(true);
                        friendRequestBadge.setManaged(true);
                    } else {
                        friendRequestBadge.setVisible(false);
                        friendRequestBadge.setManaged(false);
                    }
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void setupFeedListView() {
        if (feedListView != null) {
            feedListView.setCellFactory(lv -> new PostListCell());
            feedListView.setItems(feedData);
            feedListView.setPlaceholder(new Label("Chưa có bài đăng nào"));
        }
    }

    private void checkComponents() {
        System.out.println("=== Component Check ===");
        System.out.println("feedListView: " + (feedListView != null ? "OK" : "NULL"));
        System.out.println("profileName: " + (profileName != null ? "OK" : "NULL"));
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setOnLogoutCallback(Runnable callback) {
        this.onLogoutCallback = callback;
    }

    public void initializeData() {
        // Additional initialization after window is shown
    }

    public void dispose() {
        for (ChatWindow window : openChats.values()) {
            window.close();
        }
        openChats.clear();

        chatService.disconnectWebSocket();

        if (friendReloadScheduler != null && !friendReloadScheduler.isShutdown()) {
            friendReloadScheduler.shutdownNow();
        }
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
    }

    private void setupSearchPanel() {
        if (searchResultPanel != null) {
            searchResultPanel.setVisible(false);
            searchResultPanel.setManaged(false);
        }

        if (searchField != null) {
            searchField.addEventFilter(KeyEvent.KEY_PRESSED, this::onSearchEnter);
        }
    }

    private void setupListViews() {
        if (friendsList != null) {
            friendsList.setPlaceholder(new Label("Chưa có Cốt nào"));
            friendsList.setCellFactory(list -> new FriendCell());

            friendsList.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    JsonNode item = friendsList.getSelectionModel().getSelectedItem();
                    if (item != null) {
                        FriendInfo fi = extractFriendInfo(item);
                        if (fi != null) {
                            openDirectChat(fi.friendId, fi.friendName);
                        }
                    }
                }
            });
        }

        if (groupsList != null) {
            groupsList.setPlaceholder(new Label("Chưa có gò rúp nào"));
            groupsList.setCellFactory(list -> new GroupCell());

            groupsList.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    JsonNode g = groupsList.getSelectionModel().getSelectedItem();
                    if (g != null) {
                        long convId = g.path("id").asLong(0);
                        String name = g.path("name").asText("Nhóm không tên");
                        if (convId > 0) {
                            openGroupChat(convId, name);
                        }
                    }
                }
            });
        }

        if (searchResultsList != null) {
            searchResultsList.setPlaceholder(new Label("Không có kết quả"));
            searchResultsList.setCellFactory(list -> new SearchResultCell());
        }
    }

    private void startFriendListAutoReload() {
        friendReloadScheduler = Executors.newSingleThreadScheduledExecutor();
        friendReloadScheduler.scheduleAtFixedRate(() -> {
            try {
                if (currentUserId == 0) return;
                loadFriends();
            } catch (Exception e) {
                System.err.println("Reload friends failed: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    // ====== DATA LOADING ======
    private void loadProfileSafe() {
        try { loadProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadFriendsSafe() {
        try { loadFriends(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadGroupsSafe() {
        try { loadGroups(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadFeedSafe() {
        try { loadFeed(); } catch (Exception e) { e.printStackTrace(); }
    }


 private void loadProfile() throws Exception {
    JsonNode me = chatService.get("/users/me");
    if (me == null) return;

    long uid = me.path("userId").asLong(me.path("id").asLong(0));
    currentUserId = uid;
    prefs.putLong("zmnt_user_id", uid);

    String displayName = me.path("displayName")
            .asText(me.path("username").asText("Bạn"));

    String avatarUrl = me.path("avatarUrl").asText(null);

    // 🔥 LẤY ROLE
    String role = me.path("role").asText(null);
    SessionStore.setRole(role);

    System.out.println("🔥 USER ROLE = " + role);

    Platform.runLater(() -> {
        profileName.setText(displayName);

        // 🔥 BẬT ADMIN BUTTON TẠI ĐÂY
        btnAdmin.setVisible(SessionStore.isModerator());

        chatService.connectWebSocket();

        if (avatarUrl != null && !avatarUrl.isBlank()) {
            loadImageAsync(avatarUrl, profileAvatar);
        }

        loadFriendRequestCount();
    });
}


    private void loadFriends() throws Exception {
        if (currentUserId == 0) {
            currentUserId = prefs.getLong("zmnt_user_id", 0L);
            if (currentUserId == 0) return;
        }

        JsonNode arr = chatService.get("/friends?currentUserId=" + currentUserId);
        List<JsonNode> list = mapper.convertValue(arr, new TypeReference<>() {});

        Platform.runLater(() -> {
            ObservableList<JsonNode> items = FXCollections.observableArrayList(list);
            friendsList.setItems(items);
        });
    }

    private void loadGroups() throws Exception {
        JsonNode page = chatService.get("/conversations/my?page=0&size=50");
        List<JsonNode> all = new ArrayList<>();

        if (page != null) {
            if (page.isArray()) {
                page.forEach(all::add);
            } else if (page.has("content") && page.get("content").isArray()) {
                page.get("content").forEach(all::add);
            }
        }

        List<JsonNode> groups = new ArrayList<>();
        for (JsonNode c : all) {
            String type = c.path("type").asText("");
            if ("GROUP".equalsIgnoreCase(type) || "GROUP_CHAT".equalsIgnoreCase(type)) {
                groups.add(c);
            }
        }

        Platform.runLater(() -> groupsList.setItems(FXCollections.observableArrayList(groups)));
    }

    private void loadFeed() throws Exception {
        List<Post> posts = new ArrayList<>();

        try {
            // Gọi API và lấy dữ liệu JSON thô
            JsonNode pageNode = chatService.get("/feed?page=0&size=20");

            // ==== QUAN TRỌNG: IN RA DỮ LIỆU JSON ĐỂ KIỂM TRA ====
            System.out.println("======================================================");
            System.out.println("RAW JSON RESPONSE FROM SERVER /feed:");
            if (pageNode != null) {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageNode));
            } else {
                System.out.println("Server returned null or empty response.");
            }
            System.out.println("======================================================");

            // Phần code mapping từ JSON sang danh sách Post
            if (pageNode != null && pageNode.has("content") && pageNode.get("content").isArray()) {
                posts = mapper.convertValue(pageNode.get("content"), new TypeReference<List<Post>>() {});
            }

            // In ra thông tin của đối tượng Post sau khi đã mapping
            System.out.println("\n===== MAPPED POST OBJECTS =====");
            for (Post post : posts) {
                System.out.println("Post ID: " + post.getId() + ", Caption: '" + post.getCaption() + "'");
            }
            System.out.println("=================================\n");


        } catch (Exception e) {
            System.err.println("Error loading feed: " + e.getMessage());
            e.printStackTrace();
        }

        List<Post> finalPosts = posts;
        Platform.runLater(() -> {
            feedData.clear();
            feedData.addAll(finalPosts);
        });
    }

    private void loadImageAsync(String url, ImageView target) {
        new Thread(() -> {
            try {
                String fullUrl = url.startsWith("http") ? url :
                        "http://192.168.0.100:8081/api" + (url.startsWith("/") ? url : "/" + url);
                Image img = new Image(fullUrl, true);
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0) {
                        Platform.runLater(() -> target.setImage(img));
                    }
                });
                img.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        System.err.println("Failed to load image: " + url);
                    }
                });
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        }).start();
    }

    // ====== EVENT HANDLERS ======
    @FXML
    private void onHome() {
        mainContentPane.getChildren().setAll(feedView);
    }


    @FXML
    private void onPhotos() {
        new Thread(this::loadFeedSafe).start();
    }

    @FXML
    private void onCreatePost() {
        showPostDialog();
    }
    @FXML
private void onAdmin() {
    try {
        FXMLLoader loader =
            new FXMLLoader(getClass().getResource("/fxml/admin-view.fxml"));
        Parent adminView = loader.load();

        mainContentPane.getChildren().setAll(adminView);

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private void showPostDialog() {
        if (primaryStage == null && btnPlus != null && btnPlus.getScene() != null) {
            primaryStage = (Stage) btnPlus.getScene().getWindow();
        }

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        StackPane overlayRoot = new StackPane();
        overlayRoot.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlayRoot.setPickOnBounds(true);

        VBox modal = new VBox();
        modal.setSpacing(12);
        modal.setMaxWidth(520);
        modal.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 18 24 20 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.25, 0, 8);"
        );

        HBox header = new HBox();
        Label title = new Label("Đăng ảnh chiu");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:700;");
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:16px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, btnClose);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane imgWrap = new StackPane();
        imgWrap.setPrefHeight(260);
        imgWrap.setStyle(
                "-fx-background-color: #000;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;"
        );

        ImageView preview = new ImageView();
        preview.setPreserveRatio(true);
        preview.fitWidthProperty().bind(imgWrap.widthProperty().subtract(24));
        preview.fitHeightProperty().bind(imgWrap.heightProperty().subtract(24));
        Label placeholder = new Label("Nhấn để chọn ảnh...");
        placeholder.setStyle("-fx-text-fill:#f9fafb;-fx-font-size:14px;");
        imgWrap.getChildren().addAll(preview, placeholder);
        StackPane.setAlignment(placeholder, Pos.CENTER);

        Label capLabel = new Label("Cáp sừn");
        TextArea captionArea = new TextArea();
        captionArea.setPromptText("Viết gì đó dễ thương...");
        captionArea.setPrefRowCount(3);
        captionArea.setStyle(
                "-fx-background-radius: 16;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-color:#d1d5db;" +
                        "-fx-padding:8 10;"
        );

        Button btnSubmit = new Button("Đăng tin");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle(
                "-fx-background-color:#ff7b1a;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:16px;" +
                        "-fx-font-weight:700;" +
                        "-fx-background-radius:14;" +
                        "-fx-padding:10 0;"
        );

        modal.getChildren().addAll(header, imgWrap, capLabel, captionArea, btnSubmit);
        overlayRoot.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Scene dialogScene = new Scene(overlayRoot,
                primaryStage.getWidth(), primaryStage.getHeight());
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.show();

        final File[] selectedFile = {null};

        imgWrap.setOnMouseClicked(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif")
            );
            File f = fc.showOpenDialog(primaryStage);
            if (f != null) {
                selectedFile[0] = f;
                Image img = new Image(f.toURI().toString());
                preview.setImage(img);
                placeholder.setVisible(false);
            }
        });

        Runnable close = dialog::close;
        btnClose.setOnAction(e -> close.run());
        overlayRoot.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayRoot) close.run();
        });

        btnSubmit.setOnAction(e -> {
            if (selectedFile[0] == null) {
                showError("Đăng tin", "Hãy chọn 1 ảnh trước đã!");
                return;
            }
            String caption = captionArea.getText() == null ? "" : captionArea.getText().trim();

            new Thread(() -> {
                try {
                    // 1. Tạo Map để chứa dữ liệu đi kèm
                    Map<String, String> formData = new HashMap<>();
                    formData.put("caption", caption); // <-- Đảm bảo bạn thêm caption vào đây
                    formData.put("mediaType", "PHOTO"); // Backend cần tham số này

                    // 2. Gọi phương thức uploadFile mới với cả file và Map dữ liệu
                    chatService.uploadFile("/media/upload", selectedFile[0].toPath(), formData);

                    Platform.runLater(() -> {
                        loadFeedSafe();
                        close.run();
                        showInfo("Đăng tin", "Đăng bài thành công!");
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showError("Đăng tin lỗi", ex.getMessage()));
                }
            }).start();
        });
    }

    // ====== XỬ LÝ THÔNG BÁO KẾT BẠN THỰC TẾ ======
    @FXML
    private void onNotifications() {
        new Thread(this::showFriendRequestDialog).start();
    }

    private void showFriendRequestDialog() {
        try {
            JsonNode arr = chatService.get("/friends/requests?currentUserId=" + currentUserId);
            List<JsonNode> requests = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                arr.forEach(requests::add);
            }

            Platform.runLater(() -> {
                Stage dialog = new Stage();
                dialog.initOwner(primaryStage);
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Lời mời kết bạn");

                VBox root = new VBox(10);
                root.setPadding(new Insets(15));

                if (requests.isEmpty()) {
                    root.getChildren().add(new Label("Không có lời mời nào."));
                } else {
                    ListView<JsonNode> listView = new ListView<>();
                    listView.setItems(FXCollections.observableArrayList(requests));
                    listView.setCellFactory(lv -> new FriendRequestCell(dialog));
                    listView.setPrefHeight(400);
                    root.getChildren().add(listView);
                }

                Scene scene = new Scene(root, 380, requests.isEmpty() ? 150 : 480);
                dialog.setScene(scene);
                dialog.show();
            });
        } catch (Exception e) {
            Platform.runLater(() -> showError("Lỗi tải lời mời", e.getMessage()));
        }
    }

    private class FriendRequestCell extends ListCell<JsonNode> {
        private final Stage dialog;

        FriendRequestCell(Stage dialog) {
            this.dialog = dialog;
        }

        @Override
        protected void updateItem(JsonNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            long friendshipId = item.path("id").asLong();
            String senderName = item.path("actionUserName").asText("Người dùng");

            Label nameLbl = new Label(senderName);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Button acceptBtn = new Button("✔ Đồng ý");
            Button rejectBtn = new Button("✖ Từ chối");

            acceptBtn.setOnAction(e -> handleRequest(friendshipId, true));
            rejectBtn.setOnAction(e -> handleRequest(friendshipId, false));

            HBox btnBox = new HBox(10, acceptBtn, rejectBtn);
            btnBox.setAlignment(Pos.CENTER_RIGHT);

            VBox box = new VBox(8, nameLbl, btnBox);
            box.setPadding(new Insets(10));
            box.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8;");

            setGraphic(box);
        }

        private void handleRequest(long friendshipId, boolean accept) {
            new Thread(() -> {
                try {
                    String endpoint = accept ? "/accept" : "/reject";
                    chatService.put("/friends/requests/" + friendshipId + endpoint + "?currentUserId=" + currentUserId, "");
                    Platform.runLater(() -> {
                        dialog.close();
                        loadFriendRequestCount(); // Cập nhật badge
                        new Thread(ChatMainController.this::loadFriendsSafe).start(); // Reload danh sách bạn bè
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Lỗi xử lý lời mời", ex.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-view.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();

            controller.setPrimaryStage(primaryStage);
            controller.setLogoutCallback(() -> {
                SessionStore.clearSession();
                showLoginScreen();
            });

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Profile - ZMNT");
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        SessionStore.clearSession();
        showLoginScreen();
    }

    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root, 1200, 800);
            var css = getClass().getResource("/styles/login.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle("ZMNT Chat - Đăng nhập");
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onToggleTheme() {
        String current = prefs.get("zmnt_theme", "light");
        String next = current.equals("light") ? "dark" : "light";
        prefs.put("zmnt_theme", next);
        applyTheme(next);
    }

    @FXML
    private void onSearchEnter(KeyEvent e) {
        if (e.getCode() != KeyCode.ENTER) return;
        String keyword = searchField.getText();
        if (keyword == null || keyword.isBlank()) {
            searchResultPanel.setVisible(false);
            searchResultPanel.setManaged(false);
            searchResultsList.setItems(FXCollections.emptyObservableList());
            return;
        }

        new Thread(() -> {
            try {
                List<JsonNode> users = searchUsers(keyword.trim());
                Platform.runLater(() -> {
                    if (users.isEmpty()) {
                        searchResultsList.setItems(FXCollections.observableArrayList());
                        searchResultsList.setPlaceholder(new Label("Không tìm thấy người dùng"));
                    } else {
                        searchResultsList.setItems(FXCollections.observableArrayList(users));
                    }
                    searchResultPanel.setVisible(true);
                    searchResultPanel.setManaged(true);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError("Search lỗi", ex.getMessage()));
            }
        }).start();
    }

    private List<JsonNode> searchUsers(String keyword) throws Exception {
        List<JsonNode> result = new ArrayList<>();

        if (keyword.matches("\\d+")) {
            JsonNode user = chatService.get("/users/" + URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            if (user != null && !user.isMissingNode()) {
                result.add(user);
            }
        } else {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            JsonNode users = chatService.get("/users/search?keyword=" + encoded);
            if (users != null && users.isArray()) {
                users.forEach(result::add);
            }
        }
        return result;
    }

    private void createGroup(String name, List<JsonNode> members) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("type", "GROUP");
            body.put("name", name);
            ArrayNode memberIds = body.putArray("memberIds");
            for (JsonNode n : members) {
                memberIds.add(n.path("userId").asLong());
            }
            chatService.post("/conversations/group", body);
            Platform.runLater(() -> {
                showInfo("Tạo nhóm", "Tạo nhóm thành công!");
                new Thread(this::loadGroupsSafe).start();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Tạo nhóm lỗi", e.getMessage()));
        }
    }

    private void showCreateGroupDialog() {
        try {
            JsonNode friends = chatService.get("/friends?currentUserId=" + currentUserId);

            Platform.runLater(() -> {
                Stage dialog = new Stage();
                dialog.initOwner(primaryStage);
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Tạo nhóm chat");

                VBox root = new VBox(15);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: white;");

                Label titleLabel = new Label("Tạo nhóm mới");
                titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

                Label nameLabel = new Label("Tên nhóm:");
                TextField nameField = new TextField();
                nameField.setPromptText("Nhập tên nhóm");
                nameField.setPrefHeight(35);

                Label selectLabel = new Label("Chọn thành viên (ít nhất 2):");
                ListView<JsonNode> friendsListView = new ListView<>();
                friendsListView.setPrefHeight(250);

                ObservableList<JsonNode> friendList = FXCollections.observableArrayList();
                if (friends != null && friends.isArray()) {
                    friends.forEach(friendList::add);
                }
                friendsListView.setItems(friendList);

                friendsListView.setCellFactory(param -> new ListCell<JsonNode>() {
                    private final CheckBox checkBox = new CheckBox();
                    private final HBox hbox = new HBox(10);
                    private final ImageView avatar = new ImageView();
                    private final Label nameLabel = new Label();

                    {
                        avatar.setFitWidth(30);
                        avatar.setFitHeight(30);
                        avatar.getStyleClass().add("friend-avatar");
                        checkBox.setStyle("-fx-font-size: 14px;");
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        hbox.getChildren().addAll(checkBox, avatar, nameLabel);
                    }

                    @Override
                    protected void updateItem(JsonNode friend, boolean empty) {
                        super.updateItem(friend, empty);
                        if (empty || friend == null) {
                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        String friendName = friend.path("displayName").asText("Không tên");
                        String avatarUrl = friend.path("avatarUrl").asText(null);

                        nameLabel.setText(friendName);

                        if (avatarUrl != null && !avatarUrl.isBlank()) {
                            loadImageAsync(avatarUrl, avatar);
                        } else {
                            avatar.setImage(null);
                        }

                        // ĐÃ XÓA checkBox.setSelected(false) → checkbox giữ trạng thái khi cuộn
                        setGraphic(hbox);
                        setText(null);
                    }
                });

                Label errorLabel = new Label();
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                errorLabel.setVisible(false);

                Button createBtn = new Button("Tạo nhóm");
                createBtn.setStyle(
                        "-fx-background-color: #ff7b1a;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 14px;" +
                                "-fx-pref-height: 35px;"
                );
                createBtn.setMaxWidth(Double.MAX_VALUE);

                Button cancelBtn = new Button("Hủy");
                cancelBtn.setStyle(
                        "-fx-background-color: #e0e0e0;" +
                                "-fx-text-fill: #333;" +
                                "-fx-font-size: 14px;" +
                                "-fx-pref-height: 35px;"
                );
                cancelBtn.setMaxWidth(Double.MAX_VALUE);

                HBox buttonBox = new HBox(10, createBtn, cancelBtn);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);

                root.getChildren().addAll(
                        titleLabel, nameLabel, nameField,
                        selectLabel, friendsListView, errorLabel, buttonBox
                );

                Scene scene = new Scene(root, 400, 500);
                dialog.setScene(scene);

                cancelBtn.setOnAction(e -> dialog.close());

                createBtn.setOnAction(e -> {
                    String groupName = nameField.getText().trim();

                    if (groupName.isEmpty()) {
                        errorLabel.setText("Tên nhóm không được để trống");
                        errorLabel.setVisible(true);
                        return;
                    }

                    List<JsonNode> selectedFriends = new ArrayList<>();
                    for (int i = 0; i < friendList.size(); i++) {
                        ListCell<JsonNode> cell = (ListCell<JsonNode>) friendsListView.lookup(
                                ".list-cell:nth-child(" + (i + 1) + ")"
                        );

                        if (cell != null && cell.getGraphic() instanceof HBox) {
                            HBox hbox = (HBox) cell.getGraphic();
                            CheckBox checkBox = (CheckBox) hbox.getChildren().get(0);

                            if (checkBox.isSelected()) {
                                selectedFriends.add(friendList.get(i));
                            }
                        }
                    }

                    if (selectedFriends.size() < 2) {
                        errorLabel.setText("Chọn ít nhất 2 thành viên");
                        errorLabel.setVisible(true);
                        return;
                    }

                    new Thread(() -> createGroup(groupName, selectedFriends)).start();
                    dialog.close();
                });

                dialog.showAndWait();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi", "Không thể tải danh sách bạn bè: " + e.getMessage());
        }
    }

    @FXML
    private void onCreateGroup() {
        new Thread(this::showCreateGroupDialog).start();
    }

    @FXML
    private void onCollection() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gallery-view.fxml"));
            Parent root = loader.load();

            // Nếu bạn có GalleryController và muốn truy cập nó
            GalleryController controller = loader.getController();

            Stage galleryStage = new Stage();
            galleryStage.setTitle("Bộ Sưu Tập Ảnh");
            galleryStage.initModality(Modality.NONE); // Không block cửa sổ chính
            galleryStage.initOwner(primaryStage);    // (tuỳ chọn) để dialog thuộc về cửa sổ chính

            Scene scene = new Scene(root, 1100, 750);

            // Áp dụng theme tương tự như cửa sổ chính
            String theme = prefs.get("zmnt_theme", "light");
            if ("dark".equals(theme)) {
                URL darkCss = getClass().getResource("/styles/gallery-dark.css");
                if (darkCss != null) {
                    scene.getStylesheets().add(darkCss.toExternalForm());
                }
            } else {
                URL lightCss = getClass().getResource("/styles/gallery.css");
                if (lightCss != null) {
                    scene.getStylesheets().add(lightCss.toExternalForm());
                }
            }

            galleryStage.setScene(scene);
            galleryStage.show();

            // Nếu GalleryController có method dispose() để dọn dẹp (ví dụ: shutdown scheduler)
            galleryStage.setOnCloseRequest(e -> {
                if (controller != null) {
                    controller.dispose();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi mở bộ sưu tập", "Không thể tải giao diện bộ sưu tập: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi", "Không thể mở bộ sưu tập: " + e.getMessage());
        }
    }

    @FXML
    private void onSettings() {
        showInfo("Cài đặt", "Settings chưa implement.");
    }

    @FXML
    private void onDonate() {
        ImageView qrView = new ImageView(
                new Image(getClass().getResourceAsStream("/images/mbbank-qr.png"))
        );
        qrView.setFitWidth(260);
        qrView.setPreserveRatio(true);

        Label text = new Label(
                "MB Bank\n" +
                        "STK: 0601200488889\n" +
                        "Cảm ơn bạn đã ủng hộ team gói mì tôm <3"
        );
        text.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");

        VBox content = new VBox(10, qrView, text);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(15));

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Ủng hộ team");
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().add(ButtonType.CLOSE);
        alert.show();
    }

    private record FriendInfo(long friendId, String friendName) {}

    private FriendInfo extractFriendInfo(JsonNode item) {
        if (item == null || item.isMissingNode()) return null;
        long friendId = item.path("userId").asLong(0);
        if (friendId == 0) return null;
        String friendName = item.path("displayName").asText("Không tên");
        return new FriendInfo(friendId, friendName);
    }

    private void openDirectChat(long friendId, String friendName) {
        Platform.runLater(() -> ChatWindowController.openDirectChat(friendId, friendName));
    }

    private void openGroupChat(long conversationId, String groupName) {
        Platform.runLater(() -> ChatWindowController.openGroupChat(conversationId, groupName));
    }

    private void applyTheme(String theme) {
        Scene scene = null;
        if (primaryStage != null) scene = primaryStage.getScene();
        if (scene == null && themeToggle != null && themeToggle.getScene() != null) {
            scene = themeToggle.getScene();
        }
        if (scene == null) return;

        URL lightCssUrl = getClass().getResource("/styles/chat-main.css");
        URL darkCssUrl = getClass().getResource("/styles/chat-main-dark.css");
        String lightCss = lightCssUrl != null ? lightCssUrl.toExternalForm() : "";
        String darkCss = darkCssUrl != null ? darkCssUrl.toExternalForm() : "";

        scene.getStylesheets().removeIf(s -> s.equals(lightCss) || s.equals(darkCss));
        scene.getRoot().getStyleClass().removeAll("theme-light", "theme-dark");

        if ("dark".equals(theme)) {
            if (!darkCss.isEmpty()) scene.getStylesheets().add(darkCss);
            scene.getRoot().getStyleClass().add("theme-dark");
            themeIcon.setText("☀️");
        } else {
            if (!lightCss.isEmpty()) scene.getStylesheets().add(lightCss);
            scene.getRoot().getStyleClass().add("theme-light");
            themeIcon.setText("🌙");
        }
        currentTheme = theme;
    }

    // ====== LIST CELL CLASSES ======
    private class PostListCell extends ListCell<Post> {
        // Trong class PostListCell của file ChatMainController.java
        @Override
        protected void updateItem(Post post, boolean empty) {
            super.updateItem(post, empty);

            if (empty || post == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            // === Tạo Header (tên tác giả, avatar, thời gian) ===
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            ImageView authorAvatar = new ImageView();
            authorAvatar.setFitWidth(40);
            authorAvatar.setFitHeight(40);
            authorAvatar.setPreserveRatio(true);
            if (post.getAuthorAvatar() != null && !post.getAuthorAvatar().isBlank()) {
                loadImageAsync(post.getAuthorAvatar(), authorAvatar);
            }

            VBox authorInfo = new VBox(2);
            Label authorName = new Label(post.getAuthorUsername() != null ? post.getAuthorUsername() : "Người dùng");
            authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label postTime = new Label(formatPostTime(post.getUploadedAt()));
            postTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            authorInfo.getChildren().addAll(authorName, postTime);
            header.getChildren().addAll(authorAvatar, authorInfo);

            // === Xử lý Caption (ĐÃ SỬA LỖI) ===
            // 1. Lấy chuỗi văn bản caption
            String captionText = post.getCaption();

            // 2. Chỉ tạo và thêm Label vào giao diện NẾU caption có nội dung
            Label captionLabel = null;
            if (captionText != null && !captionText.isBlank()) {
                captionLabel = new Label(captionText);
                captionLabel.setWrapText(true);
                captionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1f2937;");
            }

            // === Tạo ảnh bài đăng ===
            ImageView postImage = new ImageView();
            postImage.setPreserveRatio(true);
            postImage.setFitWidth(500);
            postImage.setStyle("-fx-background-radius: 8;");
            if (post.getFileUrl() != null && !post.getFileUrl().isBlank()) {
                loadImageAsync(post.getFileUrl(), postImage);
            }

            // === Tạo VBox chứa toàn bộ bài đăng ===
            VBox postCard = new VBox(10);
            postCard.setPadding(new Insets(15));
            postCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

            // === Thêm các thành phần vào postCard theo thứ tự ===
            postCard.getChildren().add(header);

            // Chỉ thêm captionLabel nếu nó không phải null (tức là có nội dung)
            if (captionLabel != null) {
                postCard.getChildren().add(captionLabel);
            }

            // Chỉ thêm ảnh nếu có URL
            if (post.getFileUrl() != null && !post.getFileUrl().isBlank()) {
                postCard.getChildren().add(postImage);
            }

            setGraphic(postCard);
            setText(null);
        }


        private String formatPostTime(String uploadedAtStr) {
            if (uploadedAtStr == null || uploadedAtStr.isBlank()) {
                return "";
            }

            try {
                // 1. Chuyển chuỗi thời gian từ server (kiểu ISO-8601) thành đối tượng Instant.
                //    Instant là một thời điểm cụ thể, không phụ thuộc vào múi giờ.
                Instant postInstant = Instant.parse(uploadedAtStr);

                // 2. Lấy thời điểm hiện tại.
                Instant now = Instant.now();

                // 3. Tính khoảng cách giữa hai thời điểm.
                long secondsElapsed = java.time.Duration.between(postInstant, now).getSeconds();

                // 4. Hiển thị dưới dạng tương đối (vừa xong, X phút trước)
                if (secondsElapsed < 60) {
                    return "Vừa xong";
                } else if (secondsElapsed < 3600) { // ít hơn 1 giờ
                    return (secondsElapsed / 60) + " phút trước";
                } else if (secondsElapsed < 86400) { // ít hơn 1 ngày
                    return (secondsElapsed / 3600) + " giờ trước";
                } else if (secondsElapsed < 604800) { // ít hơn 1 tuần
                    return (secondsElapsed / 86400) + " ngày trước";
                } else {
                    // 5. Nếu quá 1 tuần, hiển thị ngày tháng năm theo múi giờ của máy tính.
                    // Chuyển Instant về ZonedDateTime với múi giờ hệ thống.
                    ZonedDateTime localPostTime = postInstant.atZone(ZoneId.systemDefault());
                    return localPostTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi định dạng thời gian: " + e.getMessage());
                return "";
            }
        }
    }

    private class FriendCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            long friendId = item.path("userId").asLong();
            String friendName = item.path("displayName").asText("Không tên");
            String avatarUrl = item.path("avatarUrl").asText(null);
            boolean online = item.path("online").asBoolean(false);

            ImageView avatar = new ImageView();
            avatar.setFitWidth(36);
            avatar.setFitHeight(36);
            avatar.setPreserveRatio(true);
            avatar.getStyleClass().add("friend-avatar");
            if (avatarUrl != null && !avatarUrl.isBlank()) loadImageAsync(avatarUrl, avatar);

            Circle dot = new Circle(5);
            dot.getStyleClass().add(online ? "online-dot" : "offline-dot");

            StackPane avatarWrap = new StackPane(avatar, dot);
            avatarWrap.getStyleClass().add("avatar-container");
            StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);

            Label nameLbl = new Label(friendName);
            nameLbl.getStyleClass().add("friend-name");

            Label statusLbl = new Label(online ? "Đang hoạt động" : "Ngoại tuyến");
            statusLbl.getStyleClass().add("friend-status");

            VBox textBox = new VBox(nameLbl, statusLbl);
            textBox.getStyleClass().add("friend-text-box");

            Button chatBtn = new Button("Nhắn tin");
            chatBtn.getStyleClass().add("friend-action-btn");
            chatBtn.setOnAction(e -> openDirectChat(friendId, friendName));

            HBox root = new HBox(avatarWrap, textBox, chatBtn);
            root.getStyleClass().add("friend-cell-root");
            root.setSpacing(10);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            ContextMenu menu = new ContextMenu();
            MenuItem chatItem = new MenuItem("💬 Nhắn tin");
            MenuItem blockItem = new MenuItem("🚫 Chặn");
            MenuItem removeItem = new MenuItem("❌ Xóa bạn");

            chatItem.setOnAction(e -> openDirectChat(friendId, friendName));

            blockItem.setOnAction(e -> {
                if (confirm("Chặn bạn", "Bạn có chắc muốn chặn " + friendName + " ?")) {
                    new Thread(() -> blockFriend(friendId)).start();
                }
            });

            removeItem.setOnAction(e -> {
                if (confirm("Xóa bạn", "Xóa " + friendName + " khỏi danh sách bạn bè?")) {
                    new Thread(() -> removeFriend(friendId)).start();
                }
            });

            menu.getItems().addAll(chatItem, blockItem, removeItem);
            setContextMenu(menu);

            root.getStyleClass().add("list-cell-content");

            setGraphic(root);
            setText(null);
        }
    }

    private class GroupCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            String name = item.path("name").asText("Nhóm không tên");
            int memberCount = item.path("memberCount").asInt(0);
            boolean isCreator = item.path("createdBy").asLong() == currentUserId;

            ImageView avatar = new ImageView();
            avatar.setFitWidth(40);
            avatar.setFitHeight(40);
            avatar.getStyleClass().add("group-avatar");
            String avatarUrl = item.path("avatarUrl").asText(null);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                loadImageAsync(avatarUrl, avatar);
            }

            Label nameLbl = new Label(name);
            nameLbl.getStyleClass().add("group-name");

            String detailText = (memberCount > 0 ? memberCount + " thành viên" : "");
            if (isCreator) detailText += (detailText.isEmpty() ? "" : " • ") + "Bạn tạo";

            Label detailLbl = new Label(detailText);
            detailLbl.getStyleClass().add("group-details");

            VBox textBox = new VBox(nameLbl, detailLbl);
            textBox.setSpacing(2);

            HBox root = new HBox(avatar, textBox);
            root.getStyleClass().add("group-cell-root");
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);

            root.getStyleClass().add("list-cell-content");

            setGraphic(root);
            setText(null);
        }
    }

    private class SearchResultCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            long userId = item.path("userId").asLong(item.path("id").asLong(0));
            String displayName = item.path("displayName").asText(item.path("username").asText("Người dùng"));
            String username = item.path("username").asText("");
            String avatarUrl = item.path("avatarUrl").asText(null);

            ImageView avatar = new ImageView();
            avatar.getStyleClass().add("search-avatar");
            avatar.setFitWidth(32);
            avatar.setFitHeight(32);
            avatar.setPreserveRatio(true);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                loadImageAsync(avatarUrl, avatar);
            }

            Label nameLbl = new Label(displayName);
            nameLbl.getStyleClass().add("search-name");

            Label userLbl = new Label(
                    (username != null && !username.isBlank() ? username + " • " : "") + "ID: " + userId
            );
            userLbl.getStyleClass().add("search-username");

            VBox infoBox = new VBox(nameLbl, userLbl);
            infoBox.setSpacing(2);

            HBox left = new HBox(avatar, infoBox);
            left.setSpacing(10);
            left.setAlignment(Pos.CENTER_LEFT);

            Button addBtn = new Button("Kết bạn");
            addBtn.getStyleClass().add("btn-add-friend");

            addBtn.setOnAction(ev -> {
                if (currentUserId == 0) {
                    showError("Lỗi", "Không xác định được user hiện tại");
                    return;
                }
                if (userId == 0) {
                    showError("Lỗi", "ID người dùng không hợp lệ");
                    return;
                }
                if (currentUserId == userId) {
                    showInfo("Kết bạn", "Không thể kết bạn với chính mình");
                    return;
                }

                new Thread(() -> handleFriendRequestClick(userId, displayName)).start();
            });

            HBox root = new HBox(left, addBtn);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(10);

            setGraphic(root);
        }
    }

    private void handleFriendRequestClick(long targetUserId, String targetName) {
        try {
            JsonNode statusNode = null;
            try {
                statusNode = chatService.get("/friends/status?user1Id=" + currentUserId + "&user2Id=" + targetUserId);
            } catch (Exception ignored) {}

            if (statusNode != null && !statusNode.isMissingNode()) {
                String status = statusNode.path("status").asText("");
                long actionUserId = statusNode.path("actionUserId").asLong(0);

                switch (status) {
                    case "ACCEPTED" -> {
                        showInfo("Kết bạn", "Bạn và " + targetName + " đã là bạn bè");
                        return;
                    }
                    case "PENDING" -> {
                        showInfo("Kết bạn", "Đã gửi lời mời tới " + targetName);
                        return;
                    }
                    case "BLOCKED" -> {
                        if (actionUserId == currentUserId) {
                            showInfo("Kết bạn", "Bạn đã chặn người này");
                        } else {
                            showError("Kết bạn", "Bạn không thể gửi lời mời");
                        }
                        return;
                    }
                }
            }

            ObjectNode body = mapper.createObjectNode();
            body.put("targetUserId", targetUserId);

            chatService.post("/friends/requests?currentUserId=" + currentUserId, body);

            showInfo("Kết bạn", "Đã gửi lời mời tới " + targetName);

        } catch (Exception e) {
            showError("Gửi lời mời lỗi", e.getMessage());
        }
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void blockFriend(long targetUserId) {
        try {
            chatService.post("/friends/block?currentUserId=" + currentUserId + "&targetUserId=" + targetUserId, "");
            loadFriendsSafe();
        } catch (Exception e) {
            showError("Chặn thất bại", e.getMessage());
        }
    }

    private void removeFriend(long targetUserId) {
        try {
            chatService.delete("/friends/by-user?currentUserId=" + currentUserId + "&targetUserId=" + targetUserId);
            loadFriendsSafe();
        } catch (Exception e) {
            showError("Xóa bạn thất bại", e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(title);
            a.setContentText(msg);
            a.show();
        });
    }

    private void showInfo(String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(title);
            a.setContentText(msg);
            a.show();
        });
    }
}