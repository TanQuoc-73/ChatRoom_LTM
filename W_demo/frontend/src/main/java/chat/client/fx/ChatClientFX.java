package chat.client.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClientFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login-view.fxml")
        );

        // Set kích thước lớn hơn để thấy được video nền đẹp
        Scene scene = new Scene(loader.load(), 1200, 800);
        scene.getStylesheets().add(
                getClass().getResource("/styles/login.css").toExternalForm()
        );

        // Get controller and set primary stage
        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle("ZMNT Chat - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            controller.dispose();
        });
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}