module chat.client.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires java.prefs;
    
    opens chat.client.fx to javafx.fxml;
    exports chat.client.fx;
}