package chat.client.fx;

import chat.client.fx.model.MediaDTO;
import chat.client.fx.service.AdminService;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class AdminController {

    @FXML private TableView<MediaDTO> table;
    @FXML private TableColumn<MediaDTO, Long> colId;
    @FXML private TableColumn<MediaDTO, String> colUser;
    @FXML private TableColumn<MediaDTO, String> colCaption;
    @FXML private TableColumn<MediaDTO, Integer> colViolation;
    @FXML private TableColumn<MediaDTO, Void> colAction;

    private final AdminService adminService = AdminService.getInstance();
    private final ObservableList<MediaDTO> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getId()));
        colUser.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAuthorUsername()));
        colCaption.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCaption()));
        colViolation.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getViolationCount()));

        setupActions();
        loadData();
    }

    @FXML
    public void loadData() {
        try {
            data.setAll(adminService.getHiddenMedia());
            table.setItems(data);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void setupActions() {
        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btnDelete = new Button("🗑 Xóa");
            private final Button btnBlock = new Button("⛔ Khoá user");

            {
                btnDelete.setOnAction(e -> {
                    MediaDTO m = getTableView().getItems().get(getIndex());
                    try {
                        adminService.deleteMedia(m.getId());
                        loadData();
                    } catch (Exception ex) {
                        showError(ex.getMessage());
                    }
                });

                btnBlock.setOnAction(e -> {
                    MediaDTO m = getTableView().getItems().get(getIndex());
                    try {
                        adminService.blockUser(m.getUserId());
                    } catch (Exception ex) {
                        showError(ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(10, btnDelete, btnBlock));
            }
        });
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
