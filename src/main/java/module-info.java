module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.suppergeist to javafx.fxml;
    exports com.example.suppergeist;
}