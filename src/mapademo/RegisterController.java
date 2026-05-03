package mapademo;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import upv.ipc.sportlib.SportActivityApp; // LIBRERIA IPC 2026, NO TOCAR
import upv.ipc.sportlib.User;             // LIBRERIA IPC 2026, NO TOCAR

public class RegisterController implements Initializable {

    @FXML
    private GridPane gridPane;
    
    @FXML
    private TextField nicknameField;
    
    @FXML
    private PasswordField passwordField;
    
    private TextField passwordVisibleField; // NO SE PONE @FXML PORQUE SE CREA MEDIANTE CÓDIGO
    
    private Label errorLabel = null;
    private int errorRowIndex = -1; // NO SE PONE @FXML Y SE INICIALIZA A NULL PORQUE SE CREA MEDIANTE CÓDIGO
    
    @FXML
    private ImageView eye;
    
    private SportActivityApp app = SportActivityApp.getInstance();
    
    @FXML
    private TextField emailField;
    
    @FXML
    private DatePicker fechaNac;
    
    @FXML
    private ImageView logoImg;
    
    // VARIABLE AUXILIAR AVATAR
    private String avatarPath;
    
    private List<String> errores = new ArrayList<>(); // Variable auxiliar para los errores
    @FXML
    private StackPane stackPane;
    @FXML
    private ImageView avatarView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
//        logoImg.setVisible(true);
//        stackPane.setVisible(false);
        
        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Tu contraseña*");
        passwordVisibleField.setPrefHeight(36);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);

        // Misma celda que el PasswordField (col 1, fila 2)
        GridPane.setColumnIndex(passwordVisibleField, 1);
        GridPane.setRowIndex(passwordVisibleField, 2);
        GridPane.setHalignment(passwordVisibleField, HPos.CENTER);

        gridPane.getChildren().add(passwordVisibleField);

        // Sincronización mutua entre ambos campos de contraseña
        passwordField.textProperty().bindBidirectional(passwordVisibleField.textProperty());
    }    

    @FXML
    private void toggleEye(ActionEvent event) {
        if (passwordField.isVisible()) {
            // PASAR A MODO VISIBLE
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true); // ESTA LINEA HACE QUE NO SE VAYA A TOMAR POR CULO LA CONTRASEÑA
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/icons/eyeOFF.png")));

        } else {
            // PASAR A MODO OCULTO
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
    private void handleNewUser(ActionEvent event) {
        try {
            limpiarError();

            String nick = nicknameField.getText().trim();
            String email = emailField.getText().trim();
            String pass = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();                
            LocalDate birthDate = fechaNac.getValue();

            String avatar_path = avatarPath;
        
            // COMPROBACIÓN DE CAMPOS VACÍOS
            if (nick.isEmpty() || email.isEmpty() || pass.isEmpty() || birthDate == null) {
                mostrarError("Completa los campos obligatorios por favor.");
                return;
            }
        
        
        
            errores.clear();
        
            // Uso un Array de Strings para gestionar bien los errores, si no es imposible hacerlo como en el Login
        
            if (!User.checkNickName(nick)) {
                errores.add("• Nickname: 6-15 caracteres.");
            }
        
            if (!User.checkEmail(email)) {
                errores.add("• Email: formato inválido.");
            }
        
            if (!User.checkPassword(pass)) { // TODO -> No se ve bien todo el texto del error (hacerlo más corto? modificar tamaño gridpane?)
                errores.add("• Contraseña: 8-20 chars, 1 mayúscula, 1 minúscula, 1 dígito, 1 símbolo (!@#$%&*()-+=).");
            }
        
            if (!User.isOlderThan(birthDate, 12)) { // TODO -> No detecta bien si es mayor de 12 años, solo cuando todos los otros campos están correctos
                errores.add("• Debes ser mayor de 12 años.");
            }
        
            if (!errores.isEmpty()) {
                mostrarError(errores);
                return; 
            }
        
            if (app.registerUser(nick, email, pass, birthDate, avatar_path)) {
                app.login(nick, pass);
                cargarPantalla("FXMLDocument.fxml", nick);
            } else {
                mostrarError("Error al registrar. El nickname o email puede estar en uso.");
            }
        } catch (java.time.format.DateTimeParseException e) { // error en fecha
            errores.add("Error de tipo en fecha de nacimiento");
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
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        errorLabel.setAlignment(javafx.geometry.Pos.CENTER);

        errorRowIndex = gridPane.getRowCount();
        GridPane.setRowIndex(errorLabel, errorRowIndex);
        GridPane.setColumnIndex(errorLabel, 0);
        GridPane.setColumnSpan(errorLabel, 3);
        GridPane.setHalignment(errorLabel, HPos.CENTER);
        GridPane.setValignment(errorLabel, javafx.geometry.VPos.CENTER);
        GridPane.setMargin(errorLabel, new Insets(8, 10, 0, 10));

        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(25);
        rc.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rc.setMaxHeight(Double.MAX_VALUE);
        rc.setVgrow(Priority.SOMETIMES);
        gridPane.getRowConstraints().add(rc);

        gridPane.getChildren().add(errorLabel);
    }
    
    private void mostrarError(List<String> errores) {
        mostrarError(String.join("\n", errores));
    }
    
    private void limpiarError() {
        if (errorLabel != null) {
            gridPane.getChildren().remove(errorLabel);
            if (errorRowIndex >= 0 && errorRowIndex < gridPane.getRowConstraints().size()) {
                gridPane.getRowConstraints().remove(errorRowIndex);
            }
            errorLabel = null;
            errorRowIndex = -1;
        }
    }

    @FXML
    private void backLogin(ActionEvent event) {
        cargarPantalla("/mapademo/Login.fxml", (String) null);
    }

    @FXML
    private void selectAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir fichero");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Imágenes", "*.png", "*.jpg"));
        File selectedFile = fileChooser.showOpenDialog(((Node)event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            avatarPath = selectedFile.getAbsolutePath();
            Image avatarImage = new Image(selectedFile.toURI().toString(), 42, 42, true, true);
            avatarView.setImage(avatarImage);
            stackPane.setVisible(true);
            logoImg.setVisible(false);
        }
    }
    
    private void cargarPantalla(String fxmlDestino, String nickname) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
            Parent root = loader.load();
            
            if (fxmlDestino.equals("FXMLDocument.fxml")) {
                FXMLDocumentController controller = loader.getController();
                controller.setNickname(nickname);
            }
            
            Stage stage = (Stage) nicknameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            if (fxmlDestino.equals("/mapademo/Login.fxml")) {
                stage.setTitle("Login");
            } else {
                stage.setTitle("Pantalla principal");
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}