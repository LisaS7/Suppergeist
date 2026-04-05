module com.example.suppergeist {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;


    opens com.example.suppergeist to javafx.fxml;
    exports com.example.suppergeist;
}