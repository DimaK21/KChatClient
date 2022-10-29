module ru.kryu.kchat.kchatclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.kryu.kchat.kchatclient to javafx.fxml;
    exports ru.kryu.kchat.kchatclient;
}