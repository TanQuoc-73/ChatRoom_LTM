package chat.client.fx;

import chat.client.fx.model.MediaDTO;
import chat.client.fx.service.MediaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for Gallery View
 */
public class GalleryController {

    @FXML
    private BorderPane root;
    @FXML
    private TilePane mediaGrid;
    @FXML
    private VBox emptyState;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Button btnFilterAll, btnFilterPhotos, btnFilterVideos;
    @FXML
    private Button btnUpload, btnRefresh;
    @FXML
    private HBox statusBar;

    private MediaService mediaService;
    private ExecutorService executor;
    private List<MediaDTO> allMedia = new ArrayList<>();
    private String currentFilter = null; // null = ALL, "PHOTO", "VIDEO"
    private HttpClient httpClient;

    @FXML
    public void initialize() {
        mediaService = MediaService.getInstance();
        executor = Executors.newCachedThreadPool();
        httpClient = HttpClient.newHttpClient();

        // Load media on startup
        loadMediaAsync();
    }

    @FXML
    private void onUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh để upload");

        // Add file filters
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("Video", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("Tất cả", "*.*"));

        // Allow multiple selection
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(root.getScene().getWindow());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            uploadFiles(selectedFiles);
        }
    }

    private void uploadFiles(List<File> files) {
        setStatus("Đang upload " + files.size() + " file...", true);

        executor.submit(() -> {
            int success = 0;
            int failed = 0;

            for (File file : files) {
                try {
                    // Determine media type based on file extension
                    String fileName = file.getName().toLowerCase();
                    String mediaType = "FILE";

                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                            fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                        mediaType = "PHOTO";
                    } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") ||
                            fileName.endsWith(".mov")) {
                        mediaType = "VIDEO";
                    }

                    MediaDTO uploaded = mediaService.uploadMedia(file, mediaType, null);
                    success++;

                    System.out.println("Uploaded: " + uploaded.getFileName());

                } catch (Exception e) {
                    failed++;
                    System.err.println("Upload failed for " + file.getName() + ": " + e.getMessage());
                }
            }

            final int finalSuccess = success;
            final int finalFailed = failed;

            Platform.runLater(() -> {
                String message = "Upload hoàn tất: " + finalSuccess + " thành công";
                if (finalFailed > 0) {
                    message += ", " + finalFailed + " thất bại";
                }
                setStatus(message, false);

                // Reload media
                loadMediaAsync();
            });
        });
    }

    @FXML
    private void onRefresh() {
        loadMediaAsync();
    }

    @FXML
    private void onFilterAll() {
        currentFilter = null;
        updateFilterButtons();
        filterAndDisplay();
    }

    @FXML
    private void onFilterPhotos() {
        currentFilter = "PHOTO";
        updateFilterButtons();
        filterAndDisplay();
    }

    @FXML
    private void onFilterVideos() {
        currentFilter = "VIDEO";
        updateFilterButtons();
        filterAndDisplay();
    }

    private void updateFilterButtons() {
        btnFilterAll.getStyleClass().remove("filter-btn-active");
        btnFilterPhotos.getStyleClass().remove("filter-btn-active");
        btnFilterVideos.getStyleClass().remove("filter-btn-active");

        if (currentFilter == null) {
            btnFilterAll.getStyleClass().add("filter-btn-active");
        } else if ("PHOTO".equals(currentFilter)) {
            btnFilterPhotos.getStyleClass().add("filter-btn-active");
        } else if ("VIDEO".equals(currentFilter)) {
            btnFilterVideos.getStyleClass().add("filter-btn-active");
        }
    }

    private void loadMediaAsync() {
        setStatus("Đang tải...", true);

        executor.submit(() -> {
            try {
                List<MediaDTO> media = mediaService.getMyMedia(null); // Get all

                Platform.runLater(() -> {
                    allMedia = media;
                    filterAndDisplay();
                    setStatus(media.size() + " ảnh/video", false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("Lỗi: " + e.getMessage(), false);
                    showError("Lỗi tải dữ liệu", e.getMessage());
                });
            }
        });
    }

    private void filterAndDisplay() {
        List<MediaDTO> filtered = new ArrayList<>();

        for (MediaDTO media : allMedia) {
            if (currentFilter == null || currentFilter.equals(media.getMediaType())) {
                filtered.add(media);
            }
        }

        displayMedia(filtered);
    }

    private void displayMedia(List<MediaDTO> mediaList) {
        mediaGrid.getChildren().clear();

        if (mediaList.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            for (MediaDTO media : mediaList) {
                VBox tile = createMediaTile(media);
                mediaGrid.getChildren().add(tile);
            }
        }
    }

    private VBox createMediaTile(MediaDTO media) {
        VBox tile = new VBox(8);
        tile.setPrefWidth(200);
        tile.setPrefHeight(240);
        tile.getStyleClass().add("media-tile");
        tile.setPadding(new Insets(10));
        tile.setAlignment(Pos.TOP_CENTER);

        // Image placeholder
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(180, 180);
        imageContainer.getStyleClass().add("image-container");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("media-thumbnail");

        // Placeholder while loading
        Label placeholder = new Label("📷");
        placeholder.setStyle("-fx-font-size: 48px; -fx-text-fill: #999;");

        imageContainer.getChildren().addAll(placeholder, imageView);

        // Load image asynchronously
        loadImageForTile(media, imageView, placeholder);

        // File name label
        Label nameLabel = new Label(truncateFileName(media.getFileName(), 20));
        nameLabel.getStyleClass().add("media-name");
        nameLabel.setMaxWidth(180);

        // Date label
        String dateStr = "";
        if (media.getUploadedAt() != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    media.getUploadedAt(),
                    ZoneId.systemDefault());
            dateStr = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("media-date");

        tile.getChildren().addAll(imageContainer, nameLabel, dateLabel);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewItem = new MenuItem("Xem lớn");
        MenuItem deleteItem = new MenuItem("Xóa");

        viewItem.setOnAction(e -> showFullImage(media));
        deleteItem.setOnAction(e -> confirmAndDelete(media));

        contextMenu.getItems().addAll(viewItem, deleteItem);
        tile.setOnContextMenuRequested(e -> contextMenu.show(tile, e.getScreenX(), e.getScreenY()));

        // Click to view
        tile.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showFullImage(media);
            }
        });

        return tile;
    }

    private void loadImageForTile(MediaDTO media, ImageView imageView, Label placeholder) {
        executor.submit(() -> {
            try {
                String imageUrl = mediaService.getMediaUrl(media);

                if (imageUrl != null) {
                    // Download image
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URL(imageUrl).toURI())
                            .GET()
                            .build();

                    HttpResponse<byte[]> response = httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() == 200) {
                        byte[] imageData = response.body();
                        Image image = new Image(new ByteArrayInputStream(imageData));

                        Platform.runLater(() -> {
                            imageView.setImage(image);
                            placeholder.setVisible(false);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to load image: " + e.getMessage());
            }
        });
    }

    private void showFullImage(MediaDTO media) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(media.getFileName());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("image-viewer");

        ImageView fullImageView = new ImageView();
        fullImageView.setPreserveRatio(true);
        fullImageView.setFitWidth(800);
        fullImageView.setFitHeight(600);

        ProgressIndicator loading = new ProgressIndicator();
        StackPane centerPane = new StackPane(loading, fullImageView);

        root.setCenter(centerPane);

        // Load full image
        executor.submit(() -> {
            try {
                String imageUrl = mediaService.getMediaUrl(media);

                if (imageUrl != null) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URL(imageUrl).toURI())
                            .GET()
                            .build();

                    HttpResponse<byte[]> response = httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofByteArray());

                    if (response.statusCode() == 200) {
                        Image image = new Image(new ByteArrayInputStream(response.body()));

                        Platform.runLater(() -> {
                            fullImageView.setImage(image);
                            loading.setVisible(false);
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loading.setVisible(false);
                    showError("Lỗi", "Không thể tải ảnh: " + e.getMessage());
                });
            }
        });

        Scene scene = new Scene(root, 850, 650);
        dialog.setScene(scene);
        dialog.show();
    }

    private void confirmAndDelete(MediaDTO media) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa " + media.getFileName() + "?");
        alert.setContentText("Bạn có chắc muốn xóa ảnh này? Hành động này không thể hoàn tác.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteMedia(media);
            }
        });
    }

    private void deleteMedia(MediaDTO media) {
        setStatus("Đang xóa...", true);

        executor.submit(() -> {
            try {
                mediaService.deleteMedia(media.getId());

                Platform.runLater(() -> {
                    setStatus("Đã xóa " + media.getFileName(), false);
                    allMedia.remove(media);
                    filterAndDisplay();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("Lỗi xóa", false);
                    showError("Lỗi", "Không thể xóa: " + e.getMessage());
                });
            }
        });
    }

    private void setStatus(String text, boolean showProgress) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            progressIndicator.setVisible(showProgress);
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String truncateFileName(String fileName, int maxLength) {
        if (fileName == null)
            return "";
        if (fileName.length() <= maxLength)
            return fileName;

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            String name = fileName.substring(0, extensionIndex);
            String ext = fileName.substring(extensionIndex);
            int availableLength = maxLength - ext.length() - 3; // -3 for "..."

            if (availableLength > 0) {
                return name.substring(0, Math.min(name.length(), availableLength)) + "..." + ext;
            }
        }

        return fileName.substring(0, maxLength - 3) + "...";
    }

    public void dispose() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}