package chat.client.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import chat.client.fx.service.ChatService;

import java.util.prefs.Preferences;

public class ChatClientFX extends Application {
    private static Stage primaryStage;
    private static ChatClientFX instance;
    
    public static ChatClientFX getInstance() {
        return instance;
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        primaryStage = stage;
        
        // Global exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });
        
        showLoginWindow();
    }
    
    public void showLoginWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());

        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        
        primaryStage.setTitle("ZMNT Chat - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
    
    public void openMainWindow() throws Exception {
        // Đóng login window
        primaryStage.close();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat-main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 800);
        
        // Load theme
            // Set theme after scene is created
    Preferences prefs = Preferences.userRoot().node("zmnt_chat_session");
    String theme = prefs.get("zmnt_theme", "light");
    System.out.println("Loading main window with theme: " + theme);
    
    // Apply theme through controller instead of direct CSS
    ChatMainController controller = loader.getController();
    controller.setPrimaryStage(primaryStage);
    
    primaryStage.setTitle("ZMNT Chat");
    primaryStage.setScene(scene);
    primaryStage.setMaximized(true);
        
        // Set close handler
        primaryStage.setOnCloseRequest(e -> {
            controller.dispose();
            ChatService.getInstance().disconnectWebSocket();
        });
        
        primaryStage.show();
        
        // Initialize data
        Platform.runLater(() -> {
            try {
                controller.initializeData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}