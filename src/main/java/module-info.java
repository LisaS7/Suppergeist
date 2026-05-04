module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires static lombok;
    requires java.sql;
    requires org.apache.commons.csv;
    requires jdk.compiler;
    requires java.net.http;
    requires com.google.gson;

    opens com.example.suppergeist.ui to javafx.fxml;
    opens com.example.suppergeist.service to com.google.gson;

    exports com.example.suppergeist;
    exports com.example.suppergeist.model;
    exports com.example.suppergeist.service;
    exports com.example.suppergeist.database;
    exports com.example.suppergeist.repository;
}
