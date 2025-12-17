package chat.client.fx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import chat.client.fx.service.ChatService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ChatMainController implements Initializable {

    @FXML
    private Button btnHome, btnPicture, btnPlus, btnBell, btnUser, themeToggle;
    @FXML
    private Label themeIcon;
    @FXML
    private TextField searchField;
    @FXML
    private VBox searchResultPanel;
    @FXML
    private ListView<JsonNode> searchResultsList;
    @FXML
    private Button btnCreate, btnCollection, btnSettings;
    @FXML
    private ImageView profileAvatar;
    @FXML
    private Label profileName;
    @FXML
    private ImageView postAuthorAvatar;
    @FXML
    private Label postAuthorName;
    @FXML
    private StackPane mainPostFrame;
    @FXML
    private ImageView mainImage;
    @FXML
    private Label postCaption;
    @FXML
    private Button postPrevBtn, postNextBtn;
    @FXML
    private ListView<JsonNode> friendsList;
    @FXML
    private ListView<JsonNode> groupsList;

    private final ChatService chatService = ChatService.getInstance();
    private final Preferences prefs = Preferences.userRoot().node("zmnt_chat_session");
    private final ObjectMapper mapper = new ObjectMapper();
    private long currentUserId = 0L;
    private Stage primaryStage;

    private final List<FeedItem> feedItems = new ArrayList<>();
    private int currentFeedIndex = -1;
    private final Map<Long, ChatWindow> openChats = new HashMap<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private ScheduledExecutorService friendReloadScheduler;
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

        String theme = prefs.get("zmnt_theme", "light");
        applyTheme(theme);

        // Load data in background
        new Thread(this::loadProfileSafe).start();
        new Thread(this::loadFriendsSafe).start();
        new Thread(this::loadGroupsSafe).start();
        new Thread(this::loadFeedSafe).start();

        startFriendListAutoReload();

    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void initializeData() {
        // Additional initialization after window is shown
    }

    public void dispose() {
        // Close all chat windows
        for (ChatWindow window : openChats.values()) {
            window.close();
        }
        openChats.clear();

        // Disconnect WebSocket
        chatService.disconnectWebSocket();
        if (friendReloadScheduler != null && !friendReloadScheduler.isShutdown()) {
            friendReloadScheduler.shutdownNow();
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
                if (currentUserId == 0)
                    return;
                loadFriends(); // gọi lại API
            } catch (Exception e) {
                System.err.println("Reload friends failed: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS); // delay 5s, repeat every 5s
    }

    // ====== DATA LOADING ======

    private void loadProfileSafe() {
        try {
            loadProfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFriendsSafe() {
        try {
            loadFriends();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGroupsSafe() {
        try {
            loadGroups();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFeedSafe() {
        try {
            loadFeed();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProfile() throws Exception {
        JsonNode me = chatService.get("/users/me");
        if (me == null)
            return;

        long uid = me.path("userId").asLong(me.path("id").asLong(0));
        currentUserId = uid;
        prefs.putLong("zmnt_user_id", uid);

        String displayName = me.path("displayName").asText(me.path("username").asText("Bạn"));
        String avatarUrl = me.path("avatarUrl").asText(null);

        Platform.runLater(() -> {
            profileName.setText(displayName);
            chatService.connectWebSocket();
            postAuthorName.setText("Bạn");
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                loadImageAsync(avatarUrl, profileAvatar);
                loadImageAsync(avatarUrl, postAuthorAvatar);
            }
        });
    }

    private void loadFriends() throws Exception {
        if (currentUserId == 0) {
            currentUserId = prefs.getLong("zmnt_user_id", 0L);
            if (currentUserId == 0)
                return;
        }

        JsonNode arr = chatService.get("/friends?currentUserId=" + currentUserId);
        List<JsonNode> list = mapper.convertValue(arr, new TypeReference<>() {
        });

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
        feedItems.clear();

        try {
            JsonNode feed = chatService.get("/feed?page=0&size=20");
            if (feed != null && feed.isArray()) {
                for (JsonNode n : feed) {
                    FeedItem item = new FeedItem();
                    item.id = n.path("id").asLong();
                    item.imageUrl = n.path("fileUrl").asText(null);
                    item.caption = n.path("caption").asText("");
                    item.authorId = n.path("userId").asLong();
                    item.authorName = n.path("authorName").asText(null);
                    feedItems.add(item);
                }
            }
        } catch (Exception ignored) {
        }

        if (feedItems.isEmpty()) {
            try {
                JsonNode mediaList = chatService.get("/media/my-media?mediaType=PHOTO");
                if (mediaList != null && mediaList.isArray()) {
                    for (JsonNode m : mediaList) {
                        FeedItem item = new FeedItem();
                        item.id = m.path("id").asLong();
                        item.imageUrl = m.path("fileUrl").asText(null);
                        item.caption = m.path("caption").asText(m.path("fileName").asText(""));
                        item.authorId = m.path("userId").asLong();
                        item.authorName = "Bạn";
                        feedItems.add(item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Platform.runLater(() -> {
            if (!feedItems.isEmpty()) {
                currentFeedIndex = 0;
                showCurrentFeed();
            } else {
                currentFeedIndex = -1;
                mainImage.setImage(null);
                postCaption.setText("Chưa có bài đăng nào.");
                postPrevBtn.setDisable(true);
                postNextBtn.setDisable(true);
            }
        });
    }

    private void showCurrentFeed() {
        if (currentFeedIndex < 0 || currentFeedIndex >= feedItems.size())
            return;
        FeedItem p = feedItems.get(currentFeedIndex);

        if (p.imageUrl != null && !p.imageUrl.isBlank()) {
            loadImageAsync(p.imageUrl, mainImage);
        } else {
            mainImage.setImage(null);
        }

        postCaption.setText(p.caption != null ? p.caption : "");
        if (p.authorName != null && !p.authorName.isBlank()) {
            postAuthorName.setText(p.authorName);
        } else if (p.authorId == currentUserId) {
            postAuthorName.setText("Bạn");
        } else {
            postAuthorName.setText("Người dùng");
        }

        postPrevBtn.setDisable(currentFeedIndex <= 0);
        postNextBtn.setDisable(currentFeedIndex >= feedItems.size() - 1);
    }

    private void loadImageAsync(String url, ImageView target) {
        new Thread(() -> {
            try {
                String fullUrl = url.startsWith("http") ? url
                        : "http://localhost:8081" + (url.startsWith("/") ? url : "/" + url);
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
        new Thread(this::loadFeedSafe).start();
    }

    @FXML
    private void onPhotos() {
        new Thread(this::loadFeedSafe).start();
    }

    @FXML
    private void onCreatePost() {
        showPostDialog();
    }

    private void showPostDialog() {
        if (primaryStage == null && btnPlus != null && btnPlus.getScene() != null) {
            primaryStage = (Stage) btnPlus.getScene().getWindow();
        }

        // ====== Tạo Stage trong suốt kiểu overlay ======
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
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 24, 0.25, 0, 8);");

        // header
        HBox header = new HBox();
        Label title = new Label("Đăng ảnh chiu");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:700;");
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:16px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, btnClose);
        header.setAlignment(Pos.CENTER_LEFT);

        // khung ảnh đen
        StackPane imgWrap = new StackPane();
        imgWrap.setPrefHeight(260);
        imgWrap.setStyle(
                "-fx-background-color: #000;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 12;");

        ImageView preview = new ImageView();
        preview.setPreserveRatio(true);
        preview.fitWidthProperty().bind(imgWrap.widthProperty().subtract(24));
        preview.fitHeightProperty().bind(imgWrap.heightProperty().subtract(24));
        Label placeholder = new Label("Nhấn để chọn ảnh...");
        placeholder.setStyle("-fx-text-fill:#f9fafb;-fx-font-size:14px;");
        imgWrap.getChildren().addAll(preview, placeholder);
        StackPane.setAlignment(placeholder, Pos.CENTER);

        // caption
        Label capLabel = new Label("Cáp sừn");
        TextArea captionArea = new TextArea();
        captionArea.setPromptText("Viết gì đó dễ thương...");
        captionArea.setPrefRowCount(3);
        captionArea.setStyle(
                "-fx-background-radius: 16;" +
                        "-fx-border-radius:16;" +
                        "-fx-border-color:#d1d5db;" +
                        "-fx-padding:8 10;");

        // footer – nút Đăng tin cam
        Button btnSubmit = new Button("Đăng tin");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle(
                "-fx-background-color:#ff7b1a;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:16px;" +
                        "-fx-font-weight:700;" +
                        "-fx-background-radius:14;" +
                        "-fx-padding:10 0;");

        modal.getChildren().addAll(header, imgWrap, capLabel, captionArea, btnSubmit);
        overlayRoot.getChildren().add(modal);
        StackPane.setAlignment(modal, Pos.CENTER);

        Scene dialogScene = new Scene(overlayRoot,
                primaryStage.getWidth(), primaryStage.getHeight());
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.show();

        // ====== Logic chọn file + upload như cũ ======
        final File[] selectedFile = { null };

        imgWrap.setOnMouseClicked(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif"));
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
            if (e.getTarget() == overlayRoot)
                close.run();
        });

        btnSubmit.setOnAction(e -> {
            if (selectedFile[0] == null) {
                showError("Đăng tin", "Hãy chọn 1 ảnh trước đã!");
                return;
            }
            String caption = captionArea.getText() == null ? "" : captionArea.getText().trim();

            new Thread(() -> {
                try {
                    JsonNode media = chatService.uploadFile("/media/upload", selectedFile[0].toPath());
                    String fileUrl = media.path("fileUrl").asText(null);

                    FeedItem newItem = new FeedItem();
                    newItem.id = media.path("id").asLong();
                    newItem.imageUrl = fileUrl;
                    newItem.caption = caption;
                    newItem.authorId = currentUserId;
                    newItem.authorName = "Bạn";

                    feedItems.add(0, newItem);
                    currentFeedIndex = 0;

                    Platform.runLater(() -> {
                        showCurrentFeed();
                        close.run();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showError("Đăng tin lỗi", ex.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void onNotifications() {
        showInfo("Thông báo", "Popup lời mời kết bạn sẽ được port sau cho JavaFX.");
    }

    @FXML
    private void onProfile() {
        showInfo("Profile", "Màn profile chưa port sang JavaFX.");
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
        if (e.getCode() != KeyCode.ENTER)
            return;
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
            // Lấy danh sách bạn bè
            JsonNode friends = chatService.get("/friends?currentUserId=" + currentUserId);

            Platform.runLater(() -> {
                // Tạo dialog mới
                Stage dialog = new Stage();
                dialog.initOwner(primaryStage);
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Tạo nhóm chat");

                VBox root = new VBox(15);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: white;");

                // Tiêu đề
                Label titleLabel = new Label("Tạo nhóm mới");
                titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

                // Ô nhập tên nhóm
                Label nameLabel = new Label("Tên nhóm:");
                TextField nameField = new TextField();
                nameField.setPromptText("Nhập tên nhóm");
                nameField.setPrefHeight(35);

                // Danh sách bạn bè với checkbox
                Label selectLabel = new Label("Chọn thành viên (ít nhất 2):");
                ListView<JsonNode> friendsListView = new ListView<>();
                friendsListView.setPrefHeight(250);

                // Tạo danh sách bạn bè với checkbox
                ObservableList<JsonNode> friendList = FXCollections.observableArrayList();
                if (friends != null && friends.isArray()) {
                    friends.forEach(friendList::add);
                }

                friendsListView.setItems(friendList);

                // Cell factory cho checkbox
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
                        } else {
                            // Lấy thông tin bạn bè
                            String friendName = friend.path("displayName").asText("Không tên");
                            String avatarUrl = friend.path("avatarUrl").asText(null);

                            nameLabel.setText(friendName);

                            // Load ảnh đại diện
                            if (avatarUrl != null && !avatarUrl.isBlank()) {
                                loadImageAsync(avatarUrl, avatar);
                            } else {
                                avatar.setImage(null);
                            }

                            // Lưu trạng thái checkbox
                            checkBox.setSelected(false);

                            setGraphic(hbox);
                            setText(null);
                        }
                    }
                });

                // Thông báo lỗi
                Label errorLabel = new Label();
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                errorLabel.setVisible(false);

                // Nút tạo nhóm
                Button createBtn = new Button("Tạo nhóm");
                createBtn.setStyle(
                        "-fx-background-color: #ff7b1a;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 14px;" +
                                "-fx-pref-height: 35px;");
                createBtn.setMaxWidth(Double.MAX_VALUE);

                // Nút hủy
                Button cancelBtn = new Button("Hủy");
                cancelBtn.setStyle(
                        "-fx-background-color: #e0e0e0;" +
                                "-fx-text-fill: #333;" +
                                "-fx-font-size: 14px;" +
                                "-fx-pref-height: 35px;");
                cancelBtn.setMaxWidth(Double.MAX_VALUE);

                // Layout các nút
                HBox buttonBox = new HBox(10, createBtn, cancelBtn);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);

                // Thêm tất cả vào root
                root.getChildren().addAll(
                        titleLabel, nameLabel, nameField,
                        selectLabel, friendsListView, errorLabel, buttonBox);

                Scene scene = new Scene(root, 400, 500);
                dialog.setScene(scene);

                // Xử lý sự kiện nút hủy
                cancelBtn.setOnAction(e -> dialog.close());

                // Xử lý sự kiện nút tạo nhóm
                createBtn.setOnAction(e -> {
                    String groupName = nameField.getText().trim();

                    if (groupName.isEmpty()) {
                        errorLabel.setText("Tên nhóm không được để trống");
                        errorLabel.setVisible(true);
                        return;
                    }

                    // Lấy danh sách bạn bè được chọn
                    List<JsonNode> selectedFriends = new ArrayList<>();
                    for (int i = 0; i < friendList.size(); i++) {
                        ListCell<JsonNode> cell = (ListCell<JsonNode>) friendsListView.lookup(
                                ".list-cell:nth-child(" + (i + 1) + ")");

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

                    // Tạo nhóm
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/gallery-view.fxml"));
            Parent root = loader.load();

            GalleryController controller = loader.getController();

            Stage galleryStage = new Stage();
            galleryStage.setTitle("Bộ Sưu Tập Ảnh");
            galleryStage.initModality(Modality.NONE);

            Scene scene = new Scene(root, 1000, 700);

            // Apply theme styles
            if ("dark".equals(currentTheme)) {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/gallery-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(
                        getClass().getResource("/styles/gallery.css").toExternalForm());
            }

            galleryStage.setScene(scene);
            galleryStage.show();

            // Handle window close
            galleryStage.setOnCloseRequest(e -> {
                if (controller != null) {
                    controller.dispose();
                }
            });

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
        showInfo("Ủng hộ team", "Cảm ơn bạn đã ủng hộ team gói mì tôm 🧡.");
    }

    @FXML
    private void onPrevPost() {
        if (feedItems.isEmpty())
            return;
        currentFeedIndex = (currentFeedIndex - 1 + feedItems.size()) % feedItems.size();
        showCurrentFeed();
    }

    @FXML
    private void onNextPost() {
        if (feedItems.isEmpty())
            return;
        currentFeedIndex = (currentFeedIndex + 1) % feedItems.size();
        showCurrentFeed();
    }

    // ====== CHAT WINDOWS ======

    private record FriendInfo(long friendId, String friendName) {
    }

    private FriendInfo extractFriendInfo(JsonNode item) {
        if (item == null || item.isMissingNode())
            return null;

        long friendId = item.path("userId").asLong(0);
        if (friendId == 0)
            return null;

        String friendName = item.path("displayName").asText("Không tên");

        return new FriendInfo(friendId, friendName);
    }

    private void openDirectChat(long friendId, String friendName) {
        Platform.runLater(() -> {
            // Gọi ChatWindowController thay vì inner class
            ChatWindowController.openDirectChat(friendId, friendName);
        });
    }

    private void openGroupChat(long conversationId, String groupName) {
        Platform.runLater(() -> {
            // Gọi ChatWindowController thay vì inner class
            ChatWindowController.openGroupChat(conversationId, groupName);
        });
    }
    // ====== THEME ======

    private void applyTheme(String theme) {
        Scene scene = null;

        if (primaryStage != null) {
            scene = primaryStage.getScene();
        }
        if (scene == null && themeToggle != null && themeToggle.getScene() != null) {
            scene = themeToggle.getScene();
        }
        if (scene == null)
            return;

        URL lightCssUrl = getClass().getResource("/styles/chat-main.css");
        URL darkCssUrl = getClass().getResource("/styles/chat-main-dark.css");

        String lightCss = lightCssUrl.toExternalForm();
        String darkCss = darkCssUrl.toExternalForm();

        scene.getStylesheets().removeIf(s -> s.equals(lightCss) || s.equals(darkCss));

        scene.getRoot().getStyleClass().removeAll("theme-light", "theme-dark");

        if ("dark".equals(theme)) {
            scene.getStylesheets().add(darkCss);
            scene.getRoot().getStyleClass().add("theme-dark");
            themeIcon.setText("☀️");
        } else {
            scene.getStylesheets().add(lightCss);
            scene.getRoot().getStyleClass().add("theme-light");
            themeIcon.setText("🌙");
        }

        currentTheme = theme;
    }

    // ====== LIST CELL CLASSES ======
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

            // Avatar
            ImageView avatar = new ImageView();
            avatar.setFitWidth(36);
            avatar.setFitHeight(36);
            avatar.setPreserveRatio(true);
            avatar.getStyleClass().add("friend-avatar"); // ADD THIS
            if (avatarUrl != null && !avatarUrl.isBlank())
                loadImageAsync(avatarUrl, avatar);

            // Online dot
            Circle dot = new Circle(5);
            dot.getStyleClass().add(online ? "online-dot" : "offline-dot"); // USE CSS CLASS

            StackPane avatarWrap = new StackPane(avatar, dot);
            avatarWrap.getStyleClass().add("avatar-container"); // ADD THIS
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
            root.getStyleClass().add("friend-cell-root"); // ADD THIS
            root.setSpacing(10);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            // THÊM DÒNG NÀY ĐỂ ÁP DỤNG CSS
            root.getStyleClass().add("list-cell-content");

            setGraphic(root);
            setText(null); // QUAN TRỌNG: clear text
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
            if (isCreator)
                detailText += (detailText.isEmpty() ? "" : " • ") + "Bạn tạo";

            Label detailLbl = new Label(detailText);
            detailLbl.getStyleClass().add("group-details");

            VBox textBox = new VBox(nameLbl, detailLbl);
            textBox.setSpacing(2);

            HBox root = new HBox(avatar, textBox);
            root.getStyleClass().add("group-cell-root");
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);

            // THÊM DÒNG NÀY
            root.getStyleClass().add("list-cell-content");

            setGraphic(root);
            setText(null); // QUAN TRỌNG: clear text
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
                    (username != null && !username.isBlank() ? username + " • " : "") + "ID: " + userId);
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
                statusNode = chatService.get(
                        "/friends/status?user1Id=" + currentUserId +
                                "&user2Id=" + targetUserId);
            } catch (Exception ignored) {
            }

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

            // ====== CHƯA CÓ QUAN HỆ → GỬI ======
            ObjectNode body = mapper.createObjectNode();
            body.put("targetUserId", targetUserId);

            chatService.post(
                    "/friends/requests?currentUserId=" + currentUserId,
                    body);

            showInfo("Kết bạn", "Đã gửi lời mời tới " + targetName);

        } catch (Exception e) {
            showError("Gửi lời mời lỗi", e.getMessage());
        }
    }

    // ====== HELPER CLASSES ======

    private static class FeedItem {
        long id;
        String imageUrl;
        String caption;
        long authorId;
        String authorName;
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