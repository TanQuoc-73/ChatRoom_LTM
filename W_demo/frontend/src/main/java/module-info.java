
module chat.client.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires java.prefs;
    requires Java.WebSocket;
    requires com.google.gson;

    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.core;

    opens chat.client.fx to javafx.fxml;
    opens chat.client.fx.service to javafx.fxml, Java.WebSocket;

    exports chat.client.fx;
    exports chat.client.fx.service;
    opens chat.client.fx.dto to com.fasterxml.jackson.databind, javafx.base;
    opens chat.client.fx.model to com.fasterxml.jackson.databind;
    exports chat.client.fx.dto to com.fasterxml.jackson.databind;
}