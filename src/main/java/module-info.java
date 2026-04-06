module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;


    opens com.example.suppergeist to javafx.fxml;
    exports com.example.suppergeist;
    exports com.example.suppergeist.ui;
    opens com.example.suppergeist.ui to javafx.fxml;
}