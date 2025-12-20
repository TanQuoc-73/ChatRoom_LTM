
package chat.client.fx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import chat.client.fx.service.ChatService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import chat.client.fx.ChatWindowController;


import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ChatWindowController {

    @FXML private HBox headerBox;
    @FXML private Label chatTitle;
    @FXML private ListView<JsonNode> messagesList;
    @FXML private TextArea messageInput;
    @FXML private Button btnSend;
    @FXML private Button btnMinimize;
    @FXML private Button btnClose;
    @FXML private Button btnManageGroup;
    @FXML private VBox chatContainer;
    @FXML private HBox inputBox;

    private final ChatService chatService = ChatService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    private long conversationId;
    private long currentUserId;
    private String conversationName;
    private boolean isGroup;
    private Stage stage;
    private String subscriptionId;
    private final Set<String> receivedClientCids = ConcurrentHashMap.newKeySet();


    private boolean minimized = false;
    private boolean isCreator;


    private double normalWidth = 330;
    private double normalHeight = 520;
    private double minimizedWidth = 200;
    private double minimizedHeight = 40;
    private double originalX, originalY;

    private int unreadCount = 0;
    private Label unreadBadge;

    private static final Map<Long, ChatWindowController> activeWindows = new ConcurrentHashMap<>();
    private static final List<ChatWindowController> minimizedWindows = new ArrayList<>();
    private static final int WINDOW_OFFSET = 30;

    // ====== PUBLIC STATIC METHODS ======


    public static void openDirectChat(long friendId, String friendName) {
        Platform.runLater(() -> {
            if (activeWindows.containsKey(friendId)) {
                ChatWindowController existing = activeWindows.get(friendId);
                existing.showWindow();
                return;
            }
            try {
                FXMLLoader loader = new FXMLLoader(ChatWindowController.class.getResource("/fxml/chat-window.fxml"));
                VBox root = loader.load();
                ChatWindowController controller = loader.getController();

                Stage stage = new Stage();
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(false);

                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setWidth(330);
                stage.setHeight(520);
                root.setStyle("-fx-background-radius: 10; -fx-background-color: white;");
                root.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0, 0.3)));
                stage.setScene(scene);
                controller.setupStage(stage);
                controller.initDirect(friendId, friendName);
                positionNewWindow(stage);
                stage.show();
                activeWindows.put(friendId, controller);
            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("Lỗi", "Không mở được cửa sổ chat");
            }
        });
    }

    public static void openGroupChat(long conversationId, String groupName) {
        Platform.runLater(() -> {
            if (activeWindows.containsKey(conversationId)) {
                ChatWindowController existing = activeWindows.get(conversationId);
                existing.showWindow();
                return;
            }
            try {
                FXMLLoader loader = new FXMLLoader(ChatWindowController.class.getResource("/fxml/chat-window.fxml"));
                VBox root = loader.load();
                ChatWindowController controller = loader.getController();

                Stage stage = new Stage();
                stage.initStyle(StageStyle.TRANSPARENT);
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setWidth(330);
                stage.setHeight(520);
                root.setStyle("-fx-background-radius: 10; -fx-background-color: white;");
                root.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0, 0.3)));

                stage.setScene(scene);
                controller.setupStage(stage);
                controller.initGroup(conversationId, groupName);

                positionNewWindow(stage);
                stage.show();
                activeWindows.put(conversationId, controller);

            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("Lỗi", "Không mở được cửa sổ nhóm chat");
            }
        });
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
    }

    private static void positionNewWindow(Stage stage) {
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();

        // Kích thước cửa sổ (trùng với web)
        double windowWidth = 330;
        double windowHeight = 520;

        // Vị trí: góc dưới phải, cách lề 20px (giống web: right=20px, bottom=20px)
        double marginRight = 320;
        double marginBottom = 20;

        double x = screenWidth - windowWidth - marginRight;
        double y = screenHeight - windowHeight - marginBottom;

        // Đảm bảo không ra khỏi màn hình
        if (x < 0) x = 20;
        if (y < 0) y = 20;

        stage.setX(x);
        stage.setY(y);

        // Đặt kích thước cửa sổ
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
    }    // ====== WINDOW SETUP ======

    private void setupStage(Stage stage) {
        this.stage = stage;
        setupDragHandlers();

        stage.setOnCloseRequest(e -> {
        });
    }
    private void setupDragHandlers() {
        headerBox.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !minimized) {
                originalX = stage.getX() - event.getScreenX();
                originalY = stage.getY() - event.getScreenY();
                headerBox.setCursor(Cursor.CLOSED_HAND);
            }
        });

        headerBox.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !minimized) {
                stage.setX(event.getScreenX() + originalX);
                stage.setY(event.getScreenY() + originalY);
            }
        });

        headerBox.setOnMouseReleased(event -> {
            headerBox.setCursor(Cursor.DEFAULT);
        });
    }

    @FXML
    public void initialize() {
        messagesList.setCellFactory(param -> new MessageCell());

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendMessage();
            }
        });

        setupUnreadBadge();
        loadCSS();
    }

    private void setupUnreadBadge() {
        unreadBadge = new Label("0");
        unreadBadge.getStyleClass().add("unread-badge");
        unreadBadge.setVisible(false);

        HBox badgeContainer = new HBox(unreadBadge);
        badgeContainer.setAlignment(Pos.CENTER);
        HBox.setMargin(badgeContainer, new Insets(0, 5, 0, 0));

        HBox titleContainer = new HBox(badgeContainer, chatTitle);
        titleContainer.setAlignment(Pos.CENTER_LEFT);
        titleContainer.setSpacing(5);

        headerBox.getChildren().remove(chatTitle);
        headerBox.getChildren().add(0, titleContainer);
    }

    private void loadCSS() {
        try {
            String css = ChatWindowController.class.getResource("/styles/chat-window.css").toExternalForm();
            chatContainer.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Không tải được CSS: " + e.getMessage());
        }
    }

    // ====== WINDOW CONTROLS ======

    @FXML
    private void onMinimize() {
        toggleMinimize();
    }

    @FXML
    private void onClose() {
        hideWindow();
    }


    @FXML
    private void onSendMessage() {
        sendMessage();
    }

    @FXML
    private void onManageGroup() {
        showManageMembersDialog();
    }

    private void showManageMembersDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Quản lý thành viên");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        ListView<JsonNode> membersView = new ListView<>();

        new Thread(() -> {
            try {
                JsonNode arr = chatService.get(
                        "/conversations/" + conversationId + "/members"
                );
                List<JsonNode> members = new ArrayList<>();
                if (arr != null && arr.isArray()) arr.forEach(members::add);

                Platform.runLater(() ->
                        membersView.setItems(FXCollections.observableArrayList(members))
                );
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        membersView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setGraphic(null);
                    return;
                }

                long uid = m.path("userId").asLong();
                String name = m.path("displayName").asText("Người dùng");
                String role = m.path("role").asText("");

                Label lbl = new Label(name + ("CREATOR".equals(role) ? " (Tạo nhóm)" : ""));

                Button kick = new Button("❌");
                kick.setDisable("CREATOR".equals(role));
                kick.setOnAction(e ->
                        new Thread(() -> removeMember(uid)).start()
                );

                HBox box = new HBox(10, lbl, kick);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        Button addBtn = new Button("➕ Thêm thành viên");
        addBtn.setOnAction(e -> showAddMemberDialog());

        root.getChildren().addAll(membersView, addBtn);
        dialog.setScene(new Scene(root, 320, 420));
        dialog.show();
    }

    private void removeMember(long userId) {
        try {
            chatService.delete(
                    "/conversations/" + conversationId + "/members/" + userId
            );
        } catch (Exception e) {
            showErrorAlert("Xóa thất bại", e.getMessage());
        }
    }
    private void showAddMemberDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Thêm thành viên");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label title = new Label("Chọn bạn bè để thêm vào nhóm");

        ListView<JsonNode> friendsView = new ListView<>();

        // Load danh sách bạn bè
        new Thread(() -> {
            try {
                JsonNode arr = chatService.get(
                        "/friends?currentUserId=" + currentUserId
                );
                List<JsonNode> friends = new ArrayList<>();
                if (arr != null && arr.isArray()) arr.forEach(friends::add);

                Platform.runLater(() ->
                        friendsView.setItems(FXCollections.observableArrayList(friends))
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        friendsView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setGraphic(null);
                    return;
                }

                long uid = f.path("userId").asLong();
                String name = f.path("displayName").asText("Người dùng");

                Label lbl = new Label(name);
                Button add = new Button("➕");

                add.setOnAction(e ->
                        new Thread(() -> addMember(uid)).start()
                );

                HBox box = new HBox(10, lbl, add);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        root.getChildren().addAll(title, friendsView);
        dialog.setScene(new Scene(root, 320, 420));
        dialog.show();
    }

    private void addMember(long userId) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("userId", userId);

            chatService.post(
                    "/conversations/" + conversationId + "/members",
                    body
            );

            Platform.runLater(() ->
                    showInfoAlert("Thành công", "Đã thêm thành viên")
            );

        } catch (Exception e) {
            Platform.runLater(() ->
                    showErrorAlert("Thêm thất bại", e.getMessage())
            );
        }
    }


    @FXML
    private void onAttachFile() {
        showInfoAlert("Chức năng", "Đính kèm file sẽ được thêm sau");
    }

    private void toggleMinimize() {
        if (!minimized) {
            minimizeWindow();
        } else {
            restoreWindow();
        }
    }

    private void minimizeWindow() {
        if (stage == null) return;

        // Lưu vị trí và kích thước hiện tại
        originalX = stage.getX();
        originalY = stage.getY();
        normalWidth = stage.getWidth();
        normalHeight = stage.getHeight();

        // Di chuyển xuống góc dưới cùng bên phải
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();

        double minX = screenWidth - minimizedWidth - 10; // Cách lề phải 10px
        double minY = screenHeight - minimizedHeight - 50; // Trên taskbar

        stage.setX(minX);
        stage.setY(minY);
        stage.setWidth(minimizedWidth);
        stage.setHeight(minimizedHeight);

        // Ẩn nội dung chat, chỉ hiển thị thanh tiêu đề
        messagesList.setVisible(false);
        inputBox.setVisible(false);

        // Thay đổi tiêu đề để biết đang minimized
        chatTitle.setText("➤ " + conversationName.substring(0, Math.min(15, conversationName.length())));

        minimized = true;

        // Khi minimized, cho phép click để restore
        headerBox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && minimized) {
                restoreWindow();
            }
        });
        chatContainer.getStyleClass().add("minimized");
        headerBox.getStyleClass().add("minimized");
        chatTitle.getStyleClass().add("minimized");
    }

    private void restoreWindow() {
        if (stage == null) return;

        // Khôi phục kích thước và vị trí
        stage.setX(originalX);
        stage.setY(originalY);
        stage.setWidth(normalWidth);
        stage.setHeight(normalHeight);

        // Hiện lại nội dung
        messagesList.setVisible(true);
        inputBox.setVisible(true);

        // Khôi phục tiêu đề
        chatTitle.setText(conversationName);

        minimized = false;

        // Xóa event click trên header (tránh xung đột với drag)
        headerBox.setOnMouseClicked(null);
        setupDragHandlers(); // Thiết lập lại drag handlers

        chatContainer.getStyleClass().remove("minimized");
        headerBox.getStyleClass().remove("minimized");
        chatTitle.getStyleClass().remove("minimized");
    }

    private void hideWindow() {
        if (stage != null) {
            stage.hide();
        }
    }

    private void showWindow() {
        if (stage != null) {
            if (stage.isIconified()) {
                stage.setIconified(false);
            }
            stage.show();
            stage.toFront();
            resetUnreadCount();
        }
    }

    private void bringToFront() {
        if (stage != null) {
            stage.toFront();
            stage.requestFocus();
        }
    }

    // ====== UNREAD COUNT ======

    private void incrementUnreadCount() {
        if (stage != null && !stage.isFocused()) {
            unreadCount++;
            updateUnreadBadge();
        }
    }

    private void resetUnreadCount() {
        unreadCount = 0;
        updateUnreadBadge();
    }

    private void updateUnreadBadge() {
        Platform.runLater(() -> {
            if (unreadCount > 0) {
                unreadBadge.setText(String.valueOf(unreadCount));
                unreadBadge.setVisible(true);

                if (stage != null) {
                    String baseTitle = isGroup ?
                            "Nhóm • " + conversationName :
                            "Chat • " + conversationName;
                    stage.setTitle("(" + unreadCount + ") " + baseTitle);
                }
            } else {
                unreadBadge.setVisible(false);
                if (stage != null) {
                    stage.setTitle(isGroup ?
                            "Nhóm • " + conversationName :
                            "Chat • " + conversationName);
                }
            }
        });
    }

    // ====== CHAT FUNCTIONALITY ======
    private void initDirect(long friendId, String friendName) {
        this.conversationName = friendName;
        this.isGroup = false;

        this.currentUserId = chatService.getCurrentUserIdSafe();

        Platform.runLater(() -> {
            chatTitle.setText(friendName);
            if (stage != null) stage.setTitle("Chat • " + friendName);
        });

        new Thread(() -> {
            try {
                JsonNode conv = chatService.post("/conversations/direct/" + friendId, "");
                this.conversationId = conv.path("id").asLong();

                loadMessages();
                subscribeRealtime();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initGroup(long convId, String groupName) {
        this.conversationId = convId;
        this.conversationName = groupName;
        this.isGroup = true;

        this.currentUserId = chatService.getCurrentUserIdSafe();

        Platform.runLater(() -> {
            chatTitle.setText(groupName);
            if (stage != null) stage.setTitle("Nhóm • " + groupName);
        });

        new Thread(() -> {
            try {
                // 1. Load tin nhắn cũ
                loadMessages();

                // 2. Subscribe realtime
                subscribeRealtime();

                // 3. Kiểm tra quyền CREATOR (chỉ cho GROUP)
                if (isGroup) {
                    JsonNode conv = chatService.get("/conversations/" + conversationId);
                    long creatorId = conv.path("createdBy").path("id").asLong(
                            conv.path("createdBy").asLong(0));

                    isCreator = creatorId == currentUserId;

                    Platform.runLater(() ->
                            btnManageGroup.setVisible(isCreator)
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }


    private void loadMessages() {
        if (conversationId <= 0) return;

        new Thread(() -> {
            try {
                JsonNode res = chatService.get("/messages/conversation/" + conversationId + "?page=0&size=50");
                List<JsonNode> messages = new ArrayList<>();

                if (res != null) {
                    if (res.isArray()) {
                        res.forEach(messages::add);
                    } else if (res.has("content") && res.get("content").isArray()) {
                        res.get("content").forEach(messages::add);
                    }
                }

                Platform.runLater(() -> {
                    Collections.reverse(messages);
                    messagesList.getItems().setAll(messages);
                    if (!messages.isEmpty()) {
                        messagesList.scrollTo(messages.size() - 1);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void subscribeRealtime() {
        if (conversationId <= 0) return;

        chatService.connectWebSocket(); // đảm bảo connect

        subscriptionId = chatService.subscribeConversation(conversationId, payload -> {
            Platform.runLater(() -> {

                // ✅ copy để không mutate payload gốc
                ObjectNode msg = payload.deepCopy();

                // ===== FIX LẶP TIN NHẮN =====
                String clientCid = msg.path("clientCid").asText(null);

                if (clientCid != null) {
                    // nếu đã render rồi → bỏ
                    if (receivedClientCids.contains(clientCid)) {
                        return;
                    }
                    receivedClientCids.add(clientCid);
                }
                // ===========================

                messagesList.getItems().add(msg);
                messagesList.scrollTo(messagesList.getItems().size() - 1);
            });
        });
    }


    private void sendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty() || conversationId <= 0) return;

        String content = text.trim();
        messageInput.clear();

        new Thread(() -> {
            try {
                // ✅ chỉ tạo, KHÔNG add vào receivedClientCids
                String clientCid = UUID.randomUUID().toString();

                ObjectNode body = mapper.createObjectNode();
                body.put("conversationId", conversationId);
                body.put("content", content);
                body.put("messageType", "TEXT");
                body.put("clientCid", clientCid);

                chatService.post("/messages", body);

            } catch (Exception e) {
                Platform.runLater(() ->
                        showErrorAlert("Lỗi gửi tin", e.getMessage())
                );
            }
        }).start();
    }


    // ====== MESSAGE CELL ======

    private class MessageCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            long senderId = item.path("senderId").asLong();
            String senderName;

            if (senderId == currentUserId) {
                senderName = "Bạn";
            } else {
                senderName = item.hasNonNull("senderName")
                        ? item.get("senderName").asText()
                        : "Người dùng";
            }

            String content = item.path("content").asText("");
            String sentAtRaw = item.path("sentAt").asText(item.path("createdAt").asText(null));

            boolean isMe = senderId == ChatWindowController.this.currentUserId;


            String timeStr = "";
            if (sentAtRaw != null && !sentAtRaw.isBlank()) {
                try {
                    OffsetDateTime dt = OffsetDateTime.parse(sentAtRaw);

                    timeStr = dt
                            .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"));

                } catch (Exception e) {
                    timeStr = "";
                }
            }

            VBox messageBox = new VBox();
            messageBox.setSpacing(2);

            if (isGroup && !isMe) {
                Label nameLabel = new Label(senderName);
                nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4b5563;");
                messageBox.getChildren().add(nameLabel);
            }

            HBox bubbleBox = new HBox();
            bubbleBox.setAlignment(Pos.CENTER_LEFT);

            Label contentLabel = new Label(content);
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(260);

            VBox bubble = new VBox(contentLabel);
            bubble.setPadding(new Insets(10, 15, 10, 15));
            bubble.setStyle(isMe ?
                    "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed); " +
                            "-fx-background-radius: 18 18 4 18; -fx-text-fill: white;" :
                    "-fx-background-color: #f3f4f6; " +
                            "-fx-background-radius: 18 18 18 4; -fx-text-fill: #111827;"
            );

            if (isMe) {
                contentLabel.setTextFill(Color.WHITE);
            }

            Label timeLabel = new Label(timeStr);
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");

            VBox bubbleWithTime = new VBox(bubble, timeLabel);
            bubbleWithTime.setSpacing(3);

            if (isMe) {
                bubbleBox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setMargin(bubbleWithTime, new Insets(0, 0, 0, 40));
            } else {
                HBox.setMargin(bubbleWithTime, new Insets(0, 40, 0, 0));
            }

            bubbleBox.getChildren().add(bubbleWithTime);
            messageBox.getChildren().add(bubbleBox);

            setGraphic(messageBox);
        }
    }

    // ====== UTILITY ======

    private static void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.show();
        });
    }

    private static void showInfoAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.show();
        });
    }

    public static void cleanup() {
        for (ChatWindowController controller : activeWindows.values()) {
            if (controller.subscriptionId != null) {
                controller.chatService.unsubscribe(controller.subscriptionId);
            }
            if (controller.stage != null) {
                controller.stage.close();
            }
        }
        activeWindows.clear();
        minimizedWindows.clear();
    }
}
