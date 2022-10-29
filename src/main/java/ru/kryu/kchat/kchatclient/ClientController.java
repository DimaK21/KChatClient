package ru.kryu.kchat.kchatclient;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private final String host = "localhost";
    private final int port = 9966;
    @FXML
    public TextArea textChat;
    @FXML
    public HBox boxMessage;
    @FXML
    public TextField fieldMessage;
    @FXML
    public Button buttonMessage;
    @FXML
    private Label labelTop;
    @FXML
    private HBox boxAuth;
    @FXML
    private TextField fieldLogin;
    @FXML
    private PasswordField fieldPass;
    @FXML
    private Button buttonAuth;
    @FXML
    private ListView listMembers;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean isAuth;
    private ObservableList<String> clientsList;

    @FXML
    private void authClick() {
        if ((socket == null) || (socket.isClosed())) {
            connect();
        }
        try {
            if (fieldLogin.getText().isEmpty() || fieldPass.getText().isEmpty()){
                showAlert("Введите логин и пароль");
                return;
            }
            output.writeUTF("/auth " + fieldLogin.getText() + " " + fieldPass.getText());
            fieldLogin.clear();
            fieldPass.clear();
            fieldLogin.requestFocus();
        } catch (IOException e) {
            showAlert("Соединение разорвано");
        }
    }

    @FXML
    private void messageClick() {
        String s = fieldMessage.getText();
        try {
            output.writeUTF(s);
            fieldMessage.clear();
            fieldMessage.requestFocus();
        } catch (IOException e) {
            showAlert("Соединение разорвано");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setAuth(false);
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            clientsList = FXCollections.observableArrayList();
            listMembers.setItems(clientsList);
            Thread t = new Thread(() -> {
                String s = null;
                try {
                    while (true) {
                        s = input.readUTF();
                        if (s.startsWith("/authok")) {
                            setAuth(true);
                            break;
                        }
                        textChat.appendText(s + "\n");
                    }
                    while (true) {
                        s = input.readUTF();
                        if (s.startsWith("/")) {
                            if (s.startsWith("/clientslist ")) {
                                String[] data = s.split("\\s");
                                Platform.runLater(() -> {
                                    clientsList.clear();
                                    for (int i = 1; i < data.length; i++) {
                                        clientsList.addAll(data[i]);
                                    }
                                });
                            }
                        } else {
                            textChat.appendText(s + "\n");
                        }
                    }
                } catch (IOException e) {
                    if (!s.equals("/end")) showAlert("Соединение разорвано");
                } finally {
                    setAuth(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            showAlert("Невозможно подключиться к серверу");
        }
    }

    public void setAuth(boolean auth) {
        isAuth = auth;
        if (auth) {
            boxAuth.setVisible(false);
            boxAuth.setManaged(false);
            boxMessage.setVisible(true);
            boxMessage.setManaged(true);
            listMembers.setVisible(true);
        } else {
            boxAuth.setVisible(true);
            boxAuth.setManaged(true);
            boxMessage.setVisible(false);
            boxMessage.setManaged(false);
            listMembers.setVisible(false);
        }
    }

    public void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setTitle("Что-то пошло не так");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    public void sendPrivateMessage(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String string = listMembers.getSelectionModel().getSelectedItem().toString();
            fieldMessage.setText("/w " + string + " ");
            fieldMessage.requestFocus();
            fieldMessage.selectEnd();
        }
    }
}