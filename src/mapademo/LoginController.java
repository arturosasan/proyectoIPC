package mapademo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import javafx.scene.layout.RowConstraints;
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

    private SportActivityApp app = SportActivityApp.getInstance();
    // LA LINEA MÁS IMPORTANTE DEL CONTROLADOR, SIRVE PARA GESTIONAR LA BD

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //CREACION DEL TEXTFIELD NUEVO PARA MOSTRAR LA CONTRASEÑA
        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Tu contraseña"); // tiene que ser igual al que ya está creado en SceneBuilder
        passwordVisibleField.setPrefHeight(28);
        passwordVisibleField.setPrefWidth(126);
        passwordVisibleField.setVisible(false); // oculto por defecto

        // Lo colocamos en la misma celda que el PasswordField (col 1, fila 1)
        GridPane.setColumnIndex(passwordVisibleField, 1);
        GridPane.setRowIndex(passwordVisibleField, 1);
        GridPane.setMargin(passwordVisibleField, new Insets(-10, 0, 0, 0)); // mismo margen que el FXML

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
            cargarPantalla("/mapademo/FXMLDocument.fxml", nick);
        } else {
            mostrarError("Usuario o contraseña incorrectos.");
        }
    }
    
    private void mostrarError(String mensaje) {
        if (errorLabel != null) {
            errorLabel.setText(mensaje);
            return;
        }

        errorLabel = new Label(mensaje); // CREACIÓN LABEL DE ERROR
        errorLabel.setStyle(                // COMO ES CREADO POR CÓDIGO, HAY QUE PONER LOS ESTILOS ASÍ 
            "-fx-text-fill: #cc0000;" +     // COLOR => ROJO
            "-fx-font-size: 11px;" +        // TAMAÑO FUENTE
            "-fx-font-weight: bold;" +      // NEGTRITA
            "-fx-wrap-text: true;"          // ALINEACION 
        );
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // LÓGICA DE CREACIÓN DE ERROR
        int nuevaFila = gridPane.getRowCount(); // fila 6, justo después del botón
        GridPane.setRowIndex(errorLabel, nuevaFila);
        GridPane.setColumnIndex(errorLabel, 0);
        GridPane.setColumnSpan(errorLabel, 3);   // las 3 columnas del FXML
        GridPane.setMargin(errorLabel, new Insets(4, 0, 0, 0));

        // Añadimos la RowConstraint para que la fila tenga altura
        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(25);
        rc.setPrefHeight(25);
        rc.setVgrow(Priority.NEVER);
        gridPane.getRowConstraints().add(rc);

        // Añadimos el nodo al GridPane
        gridPane.getChildren().add(errorLabel);
    }

    private void limpiarError() { // PARA ELIMINAR LA FILA DEL ERROR SI YA EXISTE
        if (errorLabel != null) {
            gridPane.getChildren().remove(errorLabel);
            int lastIndex = gridPane.getRowConstraints().size() - 1; // ME CARGO LA FILA QUE HE AÑADIDO AL MOSTRAR EL ERROR
            if (lastIndex >= 0) {
                gridPane.getRowConstraints().remove(lastIndex);
            }
            errorLabel = null;
        }
    }
    
    
    private void cargarPantalla(String fxmlDestino, String nickname) { // MÉTODO PARA CARGAR LA PAGINA DE REGISTRO O LA PRINCIPAL
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
            Parent root = loader.load();
            
            if (fxmlDestino.equals("/mapademo/FXMLDocument.fxml")) {
                FXMLDocumentController controller = loader.getController();
                controller.setNickname(nickname);
            }
            
            Stage stage = (Stage) nicknameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            if (fxmlDestino.equals("/mapademo/Register.fxml")) {
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
            // OCULTAR PasswordField MOSTRAR TextField
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.requestFocus(); // MANTENER FOCO EN EL CAMPO DE TEXTO DE CONTRASEÑA, PARA NO HACER 2 CLICKS
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length()); // PARA QUE EL FOCO VUELVA AL FINAL DE LA CONTRASEÑA ESCRITA

            // Cambiamos la imagen a eyeOFF
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/eyeOFF.png")));

        } else {
            // OCULTAR TextField MOSTRAR PasswordField
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordField.setVisible(true);
            passwordField.requestFocus(); // MANTENER FOCO EN EL CAMPO DE TEXTO DE CONTRASEÑA, PARA NO HACER 2 CLICKS
            passwordField.positionCaret(passwordField.getText().length()); // PARA QUE EL FOCO VUELVA AL FINAL DE LA CONTRASEÑA ESCRITA

            // Cambiamos la imagen a eyeON
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/eyeON.png")));
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        cargarPantalla("/mapademo/Register.fxml", null);
    }
}