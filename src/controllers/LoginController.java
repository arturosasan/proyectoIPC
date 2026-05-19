package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

import upv.ipc.sportlib.SportActivityApp; // LIBRERIA IPC 2026, NO TOCAR
import upv.ipc.sportlib.User;             // LIBRERIA IPC 2026, NO TOCAR

public class LoginController implements Initializable {

    @FXML
    private TextField nicknameField;

    @FXML
    private PasswordField passwordField;
    
    private TextField passwordVisibleField; // NO SE PONE @FXML PORQUE SE CREA MEDIANTE CÓDIGO (SE INICIALIZA EN initializa())

    private Label errorLabel = null; // NO SE PONE @FXML Y SE INICIALIZA A NULL PORQUE SE CREA MEDIANTE CÓDIGO
    
    @FXML
    private Button enterButton;
    
    @FXML
    private GridPane gridPane;
    
    @FXML
    private ImageView eye;

    @FXML
    private TextFlow bottomText;

    private SportActivityApp app = SportActivityApp.getInstance();
    // LA LINEA MÁS IMPORTANTE DEL CONTROLADOR, SIRVE PARA GESTIONAR LA BD

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //CREACION DEL TEXTFIELD NUEVO PARA MOSTRAR LA CONTRASEÑA
        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Tu contraseña"); // tiene que ser igual al que ya está creado en SceneBuilder
        passwordVisibleField.setPrefHeight(36);
        passwordVisibleField.setVisible(false); // oculto por defecto
        passwordVisibleField.setManaged(false); // no ocupa espacio en el layout

        // Lo colocamos en la misma celda que el PasswordField (col 1, fila 3)
        GridPane.setColumnIndex(passwordVisibleField, 1);
        GridPane.setRowIndex(passwordVisibleField, 3); // se me olvidó cambiar este pequeñito detalle ayer :)
        GridPane.setHalignment(passwordVisibleField, javafx.geometry.HPos.CENTER);
        GridPane.setMargin(passwordVisibleField, new Insets(0, 0, 20, 0)); // esto para que se quede igual visualmente que en SB

        gridPane.getChildren().add(passwordVisibleField);

        // SINCRONIZACIÓN MUTUA AMBOS CAMPOS DE CONTRASEÑA
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordField.isVisible()) passwordVisibleField.setText(newVal);
        });
        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordVisibleField.isVisible()) passwordField.setText(newVal);
        });
    }
    
    @FXML
    public void handleLogin(ActionEvent event) {
        limpiarError(); // SE QUITA EL ERROR POR SI ACASO YA HAY UNO ANTERIOR
        
        String nick = nicknameField.getText().trim();
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();

        if (nick.isEmpty() || pass.isEmpty()) {
            mostrarError("Por favor, completa todos los campos.");
            return; // SE AÑADE ESTE RETURN VACIO PARA QUE LA APLICACION NO CONTINUE SI LOS CAMPOS ESTÁN VACÍOS
        }

        if (app.login(nick, pass)) {
            User user = app.getCurrentUser();
            cargarPantalla("/views/MapaPrincipal.fxml", nick);
        } else {
            mostrarError("Usuario o contraseña incorrectos.");
        }
    }
    
    private void mostrarError(String mensaje) {
        if (errorLabel != null) {
            errorLabel.setText(mensaje);
            return;
        }

        errorLabel = new Label(mensaje);
        errorLabel.setStyle(
            "-fx-text-fill: #cc0000;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-wrap-text: true;"
        );
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        errorLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Insertar error en el VBox raíz, entre el VBox interior y el footer
        if (bottomText.getParent() instanceof VBox rootVBox) {
            int footerIndex = rootVBox.getChildren().indexOf(bottomText);
            rootVBox.getChildren().add(footerIndex, errorLabel);
            VBox.setMargin(errorLabel, new Insets(5, 40, 5, 40));
            VBox.setVgrow(errorLabel, Priority.NEVER);
        }
    }

    private void limpiarError() { // PARA ELIMINAR LA FILA DEL ERROR SI YA EXISTE
        if (errorLabel != null) {
            if (bottomText.getParent() instanceof VBox rootVBox) {
                rootVBox.getChildren().remove(errorLabel);
            }
            errorLabel = null;
        }
    }
    
    
    private void cargarPantalla(String fxmlDestino, String nickname) { // MÉTODO PARA CARGAR LA PAGINA DE REGISTRO O LA PRINCIPAL
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
            Parent root = loader.load();
            
            if (fxmlDestino.equals("/views/MapaPrincipal.fxml")) {
                MapaPrincipalController controller = loader.getController();
                controller.setNickname(nickname);
            }
            
            Stage stage = (Stage) nicknameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            if (fxmlDestino.equals("/views/Register.fxml")) {
                stage.setTitle("Registro");
            } else {
                stage.setTitle("Pantalla principal");
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleEye(ActionEvent event) {
        if (passwordField.isVisible()) {
            passwordVisibleField.setText(passwordField.getText());
            
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());

            eye.setImage(new Image(getClass().getResourceAsStream("/resources/icons/eyeOFF.png")));

        } else {
            passwordField.setText(passwordVisibleField.getText());
            
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());

            eye.setImage(new Image(getClass().getResourceAsStream("/resources/icons/eyeON.png")));
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        cargarPantalla("/views/Register.fxml", null);
    }
}