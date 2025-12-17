module chat.client.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires java.prefs;
    requires Java.WebSocket;
    opens chat.client.fx to javafx.fxml,com.fasterxml.jackson.databind;
    opens chat.client.fx.service to javafx.fxml,Java.WebSocket,com.fasterxml.jackson.databind;


    exports chat.client.fx;
    exports chat.client.fx.service;
}