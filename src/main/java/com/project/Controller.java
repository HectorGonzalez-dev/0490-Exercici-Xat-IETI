package com.project;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;

public class Controller implements Initializable {

    private final ChatService chatService = new ChatService();
    private boolean isWaiting = false;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Text textInfo;

    @FXML
    private TextField textField;

    @FXML
    private Button buttonSend;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textField.setOnAction(this::sendMessage);
    }

    @FXML
    private void sendMessage(ActionEvent event) {

        if (isWaiting) {
            return;
        }

        String msg = textField.getText().trim();
        if (!msg.isEmpty()) {
            textInfo.setText(textInfo.getText() + "Tú: " + msg + "\n");
            scrollPane.setVvalue(1.0);
            textField.clear();
            textField.requestFocus();
            isWaiting = true;
            buttonSend.setDisable(true);

            // Respuesta de la IA
            chatService.sendMessage(msg).whenComplete((reply, ex) ->
                    Platform.runLater(() -> {
                        if (ex != null || reply == null) {
                            textInfo.setText(textInfo.getText() + "No se pudo obtener respuesta.\n");
                        } else {
                            textInfo.setText(textInfo.getText() + "IETI: " + reply + "\n");
                        }
                        scrollPane.setVvalue(1.0);

                        isWaiting = false;
                        buttonSend.setDisable(false);
                        textField.requestFocus();
                    }));

        } else {
            textField.requestFocus();
        }
    }

}
