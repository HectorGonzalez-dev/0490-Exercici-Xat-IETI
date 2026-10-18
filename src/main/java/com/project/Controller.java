package com.project;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
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

    // Referencia al Label del cuerpo durante el streaming de la IA
    private Label cuerpoIETIActual = null;

    // Referencia al Label "Thinking..." de la rama de mensaje + imagen
    private Label labelThinkingActual = null;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox historial;

    @FXML
    private TextField textField;

    @FXML
    private Button buttonSend;

    @FXML
    private Button buttonImage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        textField.setOnAction(this::sendMessage);

        // Autoscroll: cada vez que la altura del historial cambie, bajar el scroll al final
        historial.heightProperty().addListener((obs, oldVal, newVal) ->
            Platform.runLater(() -> scrollPane.setVvalue(1.0))
        );
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

    private void agregarMensaje(String autor, String texto) {
        agregarMensaje(autor, texto, null);
    }

    private void agregarMensaje(String autor, String texto, Image imagen) {
        VBox contenedor = new VBox(4.0);

        Label lblAutor = new Label(autor);
        lblAutor.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblTexto = new Label(texto);
        lblTexto.setStyle("-fx-font-size: 13px;");
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(Double.MAX_VALUE);

        contenedor.getChildren().addAll(lblAutor, lblTexto);

        if (imagen != null) {
            ImageView imageView = new ImageView(imagen);
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);
            contenedor.getChildren().add(imageView);
        }

        historial.getChildren().add(contenedor);
        scrollPane.setVvalue(1.0);
    }

    @FXML
    private void sendMessage(ActionEvent event) {

        if (isWaiting) {
            return;
        }

        String msg = textField.getText().trim();
        if (!msg.isEmpty()) {
            // Mensaje del usuario (con miniatura si hay imagen seleccionada)
            Image imagen = null;
            if (selectedImage != null) {
                imagen = new Image(selectedImage.toURI().toString());
            }
            agregarMensaje("Tú:", msg, imagen);
            scrollPane.setVvalue(1.0);
            textField.clear();
            textField.requestFocus();
            isWaiting = true;
            buttonSend.setDisable(true);
            cuerpoIETIActual = null;

            // Respuesta de la IA asincrona
            if (base64 != null) {
                // Mostrar "Thinking..." grisáceo antes de enviar la petición
                labelThinkingActual = new Label("Thinking...");
                labelThinkingActual.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");
                labelThinkingActual.setWrapText(true);
                labelThinkingActual.setMaxWidth(Double.MAX_VALUE);
                VBox contenedorThinking = new VBox(4.0);
                Label lblAutorThinking = new Label("IETI:");
                lblAutorThinking.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
                contenedorThinking.getChildren().addAll(lblAutorThinking, labelThinkingActual);
                historial.getChildren().add(contenedorThinking);
                scrollPane.setVvalue(1.0);

                // Se envía la imagen
                chatService.sendMessageWithImage(msg, base64).whenComplete((reply, ex) ->
                        Platform.runLater(() -> {
                            if (labelThinkingActual != null) {
                                if (ex != null) {
                                    labelThinkingActual.setText("No se pudo obtener respuesta.");
                                } else {
                                    labelThinkingActual.setText(reply != null ? reply : "");
                                }
                                labelThinkingActual.setStyle("-fx-font-size: 13px;");
                                labelThinkingActual = null;
                            } else {
                                if (ex != null) {
                                    agregarMensaje("IETI:", "No se pudo obtener respuesta.\n");
                                } else {
                                    agregarMensaje("IETI:", reply + "\n");
                                }
                            }
                            scrollPane.setVvalue(1.0);
                            base64 = null;
                            selectedImage = null;
                            isWaiting = false;
                            buttonSend.setDisable(false);
                        }));
            } else {
                // Streaming de respuesta solo texto
                chatService.streamMessage(msg, fragment ->
                    Platform.runLater(() -> {
                        if (cuerpoIETIActual == null) {
                            VBox contenedor = new VBox(4.0);

                            Label lblAutor = new Label("IETI:");
                            lblAutor.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

                            Label lblCuerpo = new Label(fragment);
                            lblCuerpo.setStyle("-fx-font-size: 13px;");
                            lblCuerpo.setWrapText(true);
                            lblCuerpo.setMaxWidth(Double.MAX_VALUE);

                            contenedor.getChildren().addAll(lblAutor, lblCuerpo);
                            historial.getChildren().add(contenedor);

                            cuerpoIETIActual = lblCuerpo;
                        } else {
                            cuerpoIETIActual.setText(cuerpoIETIActual.getText() + fragment);
                        }
                        scrollPane.setVvalue(1.0);
                    })
                ).whenComplete((reply, ex) ->
                        Platform.runLater(() -> {
                            if (ex != null) {
                                if (cuerpoIETIActual == null) {
                                    agregarMensaje("IETI:", "No se pudo obtener respuesta.\n");
                                } else {
                                    cuerpoIETIActual.setText(cuerpoIETIActual.getText() + "No se pudo obtener respuesta.\n");
                                }
                            } else {
                                if (cuerpoIETIActual != null) {
                                    cuerpoIETIActual.setText(cuerpoIETIActual.getText() + "\n");
                                }
                            }
                            cuerpoIETIActual = null;
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
