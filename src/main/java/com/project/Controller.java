package com.project;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;

public class Controller implements Initializable {

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
        String msg = textField.getText().trim();
        if (!msg.isEmpty()) {
            textInfo.setText(textInfo.getText() + msg + "\n");
            scrollPane.setVvalue(1.0);
            textField.clear();
        }
        textField.requestFocus();
    }

}
