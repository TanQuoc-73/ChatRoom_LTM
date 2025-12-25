package chat.client.fx;

import chat.client.fx.dto.UserManagementDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminController implements Initializable {

    // ==================== CÁC THUỘC TÍNH CHÍNH ====================
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final ExecutorService executor;
    private static final String API_BASE = "http://192.192.192.192:8081/api";

    // ==================== FXML ELEMENTS ====================
    @FXML private TableView<UserManagementDTO> userTable;
    @FXML private TableColumn<UserManagementDTO, Long> colId;
    @FXML private TableColumn<UserManagementDTO, String> colUsername;
    @FXML private TableColumn<UserManagementDTO, String> colEmail;
    @FXML private TableColumn<UserManagementDTO, String> colRole;
    @FXML private TableColumn<UserManagementDTO, Boolean> colActive;
    @FXML private TableColumn<UserManagementDTO, String> colCreatedAt;

    private final ObservableList<UserManagementDTO> userList = FXCollections.observableArrayList();

    // ==================== CONSTRUCTOR ====================
    public AdminController() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Admin-Worker-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    // ==================== INITIALIZE ====================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        userTable.setItems(userList);
        loadUsers();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    // ==================== PHƯƠNG THỨC TẢI DỮ LIỆU ====================
    private void loadUsers() {
        String token = SessionStore.getSessionToken();
        if (token == null || token.isEmpty()) {
            showError("Lỗi xác thực", "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
            return;
        }

        executor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/admin/users?page=0&size=100"))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode rootNode = mapper.readTree(response.body());
                            if (rootNode.has("content")) {
                                JsonNode content = rootNode.get("content");
                                List<UserManagementDTO> users = mapper.convertValue(content, new TypeReference<>() {});
                                userList.setAll(users);
                            } else {
                                showError("Lỗi dữ liệu", "Response không chứa nội dung người dùng.");
                            }
                        } catch (Exception e) {
                            showError("Lỗi parse dữ liệu", "Không thể đọc JSON từ server.");
                        }
                    } else {
                        showError("Lỗi server", "Không thể tải danh sách người dùng. Mã lỗi: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi kết nối", "Không thể kết nối đến server."));
            }
        });
    }

    // ==================== CÁC PHƯƠNG THỨC CRUD (CHỈ CÒN XÓA VÀ KHÓA/MỞ KHÓA) ====================
    public void onDeleteUserButtonClick(ActionEvent event) {
        UserManagementDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một người dùng để xóa.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Bạn có chắc chắn muốn xóa người dùng '" + selectedUser.getUsername() + "'?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String url = API_BASE + "/admin/users/" + selectedUser.getId();
            sendApiRequest("DELETE", url, null, "Xóa người dùng thành công!");
        }
    }

    public void onLockUserButtonClick(ActionEvent event) {
        toggleUserStatus(false);
    }

    public void onUnlockUserButtonClick(ActionEvent event) {
        toggleUserStatus(true);
    }

    private void toggleUserStatus(boolean active) {
        UserManagementDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một người dùng.");
            return;
        }

        String statusText = active ? "mở khóa" : "khóa";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText("Bạn có chắc chắn muốn " + statusText + " người dùng '" + selectedUser.getUsername() + "'?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String url = API_BASE + "/admin/users/" + selectedUser.getId() + "/status?active=" + active;
            sendApiRequest("PATCH", url, null, statusText + " người dùng thành công!");
        }
    }

    // ==================== PHƯƠNG THỨC GỌI API CHUNG ====================
    private void sendApiRequest(String method, String url, Object bodyObject, String successMessage) {
        String token = SessionStore.getSessionToken();
        if (token == null) {
            showError("Lỗi xác thực", "Phiên đăng nhập hết hạn.");
            return;
        }

        executor.submit(() -> {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(15));

                if ("DELETE".equals(method)) {
                    requestBuilder.DELETE();
                } else if ("PATCH".equals(method)) {
                    requestBuilder.method("PATCH", HttpRequest.BodyPublishers.noBody());
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", successMessage);
                        loadUsers(); // Tải lại danh sách
                    } else {
                        showError("Lỗi", "Thao tác thất bại. Mã lỗi: " + response.statusCode() + "\n" + response.body());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage()));
            }
        });
    }

    // ==================== CÁC PHƯƠNG THỤC HỖ TRỢ ====================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    public void dispose() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}