module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires static lombok;
    requires java.sql;
    requires org.apache.commons.csv;
    
    opens com.example.suppergeist.ui to javafx.fxml;

    exports com.example.suppergeist;
    exports com.example.suppergeist.model;
    exports com.example.suppergeist.service;
    exports com.example.suppergeist.database;
    exports com.example.suppergeist.repository;
}