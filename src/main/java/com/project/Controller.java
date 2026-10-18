package com.project;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;

public class Controller implements Initializable {

    private final ChatService chatService = new ChatService();
    private boolean isWaiting = false;
    private File selectedImage = null;
    private String base64 = null;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Text textInfo;

    @FXML
    private TextField textField;

    @FXML
    private Button buttonSend;

    @FXML
    private Button buttonImage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textField.setOnAction(this::sendMessage);
    }

    @FXML
    private void selectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen");

        // Filtrar extensiones permitidas
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                "Imágenes (*.png, *.jpg, *.jpeg)",
                "*.png", "*.jpg", "*.jpeg"
            )
        );

        // Abrir el selector
        File archivo = fileChooser.showOpenDialog(buttonImage.getScene().getWindow());

        // Guardar el archivo seleccionado
        if (archivo != null) {
            selectedImage = archivo;
            try {
                byte[] bytes = Files.readAllBytes(selectedImage.toPath());
                base64 = Base64.getEncoder().encodeToString(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

            // Respuesta de la IA asincrona
            if (base64 != null) {
                chatService.sendMessageWithImage(msg, base64).whenComplete((reply, ex) ->
                        Platform.runLater(() -> {
                            if (ex != null) {
                                textInfo.setText(textInfo.getText() + "No se pudo obtener respuesta.\n");
                            } else {
                                textInfo.setText(textInfo.getText() + "IETI: " + reply + "\n");
                            }
                            scrollPane.setVvalue(1.0);
                            base64 = null;
                            selectedImage = null;
                            isWaiting = false;
                            buttonSend.setDisable(false);
                        }));
            } else {
                // Array boolean[] porque Java no permite reasignar una variable local normal dentro de un lambda
                boolean[] primeraVez = {true};

                chatService.streamMessage(msg, fragment -> 
                    Platform.runLater(() -> {
                            if (primeraVez[0]) {
                                textInfo.setText(textInfo.getText() + "IETI: " + fragment);
                                primeraVez[0] = false;
                            } else {
                                textInfo.setText(textInfo.getText() + fragment);
                            }
                            scrollPane.setVvalue(1.0);
                        })
                ).whenComplete((reply, ex) ->
                        Platform.runLater(() -> {
                            if (ex != null) {
                                textInfo.setText(textInfo.getText() + "No se pudo obtener respuesta.\n");
                            } else {
                                textInfo.setText(textInfo.getText() + "\n");
                            }
                            scrollPane.setVvalue(1.0);
                            isWaiting = false;
                            buttonSend.setDisable(false);
                        }));
            }



        } else {
            textField.requestFocus();
        }
    }

}
