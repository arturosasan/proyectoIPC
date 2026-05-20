package controllers;

import java.io.File;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.DatePicker;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class ModificarPerfilController implements Initializable {
    
    private String currentUserNick;
    
    @FXML
    private Button salirButton;
    
    @FXML
    private Label nicknameField;
    
    /**
     * Almacena el nickname del usuario actual.
     *
     * @param nick nickname del usuario
     */
    public void setNickname(String nick) {
        this.currentUserNick = nick;
    }
    
    @FXML
    private GridPane gridPane;
    
    private Label errorLabel = null;
    private int errorRowIndex = -1;
    private Label successLabel = null;
    private int successRowIndex = -1;
    private boolean savingInProgress = false;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    
    @FXML
    private ImageView eye;
        
    @FXML
    private DatePicker birthDatePicker;
    
    @FXML
    private ImageView avatarView;
    
    private String avatarPath = null;
    
    private List<String> errores = new ArrayList<>(); // Variable auxiliar para los errores

    
    private SportActivityApp app = SportActivityApp.getInstance();
        
    /**
     * Inicializa el controlador de modificación de perfil.
     * Crea el campo de texto para la contraseña visible, sincroniza
     * ambos campos y carga los datos actuales del usuario.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Nueva contraseña");
        passwordVisibleField.setPrefHeight(36);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        
        GridPane.setColumnIndex(passwordVisibleField, 1);
        GridPane.setRowIndex(passwordVisibleField, 2);
        GridPane.setHalignment(passwordVisibleField, HPos.CENTER);
        gridPane.getChildren().add(passwordVisibleField);
        
        passwordField.textProperty().bindBidirectional(passwordVisibleField.textProperty());
        cargarDatosActuales();
    }
    
    /**
     * Alterna la visibilidad de la contraseña entre modo oculto y visible.
     *
     * @param event evento de acción del botón del ojo
     */
    @FXML
    private void toggleEye(ActionEvent event) {
       
        if (passwordField.isVisible()) {
            
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/icons/eyeOFF.png")));
        
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
            
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/icons/eyeON.png")));
            
        }
    }
    
    /**
     * Guarda los cambios del perfil del usuario.
     * Valida email, contraseña y fecha de nacimiento, y actualiza
     * los datos a través de SportActivityApp.
     */
    @FXML
    private void handleGuardar() {
        try {
            if (savingInProgress) return;
            savingInProgress = true;
        
            limpiarError();
            limpiarExito();
        
            String email = emailField.getText().trim();
            String password = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
            LocalDate birthDate = birthDatePicker.getValue();
        
            if (password == null || password.isEmpty()) {
                password = app.getCurrentUser().getPassword();
            }
            
            // INICIO DE LÓGICA ERRORES
            
            errores.clear(); // .clear() es método propio de la lista de Strings
            
            if (email.isEmpty()) {
                errores.add("• Email: el campo no puede estar vacío.");
            } else if (!User.checkEmail(email)) {
                errores.add("• Email: formato inválido.");
            }
        
            if (!User.checkPassword(password)) {
                errores.add("• Contraseña: 8-20 chars, 1 mayúscula, 1 minúscula, 1 dígito, 1 símbolo (!@#$%&*()-+=).");
            }
        
            if (birthDate == null || !User.isOlderThan(birthDate, 12)) {
                errores.add("• Debes ser mayor de 12 años.");
            }
        
            if (!errores.isEmpty()) {
                mostrarError(errores);
                return;
            }
        
            app.updateCurrentUser(email, password, birthDate, avatarPath);
            app.getCurrentUser().setEmail(email);
            app.getCurrentUser().setPassword(password);
            app.getCurrentUser().setBirthDate(birthDate);
            app.getCurrentUser().setAvatarPath(avatarPath);
            mostrarExito("Perfil actualizado correctamente.");
        
            savingInProgress = false;
        } catch (java.time.format.DateTimeParseException e) { // error en fecha
            errores.add("Error de tipo en fecha de nacimiento");
        } catch (Exception e) {
            errores.add("Error al actualizar perfil" + e.getMessage());
        }
    }
    
    /**
     * Muestra un mensaje de error en el formulario de modificación.
     * Crea dinámicamente una fila de error en el GridPane.
     *
     * @param mensaje texto del error a mostrar
     */
    private void mostrarError(String mensaje) {
        limpiarExito();
        
        if (errorLabel != null) {
            errorLabel.setText(mensaje);
            savingInProgress = false;
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
        savingInProgress = false;
    }
    
    /**
     * Muestra múltiples errores combinados en una sola línea.
     *
     * @param errores lista de mensajes de error a mostrar
     */
    private void mostrarError(List<String> errores) {
        mostrarError(String.join("\n", errores));
    }

    /**
     * Muestra un mensaje de éxito tras guardar los cambios.
     * El mensaje se autodestruye tras 2 segundos.
     *
     * @param mensaje texto de éxito a mostrar
     */
    private void mostrarExito(String mensaje) {
        limpiarError();
        limpiarExito();
        
        successLabel = new Label(mensaje);
        successLabel.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-wrap-text: true;");
        successLabel.setMaxWidth(Double.MAX_VALUE);
        successLabel.setWrapText(true);
        successLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        successLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        successRowIndex = gridPane.getRowCount();
        GridPane.setRowIndex(successLabel, successRowIndex);
        GridPane.setColumnIndex(successLabel, 0);
        GridPane.setColumnSpan(successLabel, 3);
        GridPane.setHalignment(successLabel, HPos.CENTER);
        GridPane.setValignment(successLabel, javafx.geometry.VPos.CENTER);
        GridPane.setMargin(successLabel, new Insets(8, 10, 0, 10));
        
        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(Region.USE_COMPUTED_SIZE);
        rc.setPrefHeight(Region.USE_COMPUTED_SIZE);
        rc.setMaxHeight(Region.USE_COMPUTED_SIZE);
        rc.setVgrow(Priority.NEVER);
        
        gridPane.getRowConstraints().add(rc);
        gridPane.getChildren().add(successLabel);
        
        new Thread(() -> { // CSD siendo útil?
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> {
                limpiarExito();
                savingInProgress = false;
            });
        }).start();
    }
    
    /**
     * Elimina el mensaje de éxito del formulario si existe.
     */
    private void limpiarExito() {
        if (successLabel != null) {
            gridPane.getChildren().remove(successLabel);
            if (successRowIndex >= 0 && successRowIndex < gridPane.getRowConstraints().size()) {
                gridPane.getRowConstraints().remove(successRowIndex);
            }
            successLabel = null;
            successRowIndex = -1;
        }
    }
    
    /**
     * Elimina el mensaje de error del formulario si existe.
     */
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
    
    /**
     * Abre un selector de archivos para elegir una nueva imagen de avatar.
     */
    @FXML
    private void handleElegirAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar avatar");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            avatarPath = file.getAbsolutePath();
            Image avatarImage = new Image(file.toURI().toString(), 42, 42, true, true);
            avatarView.setImage(avatarImage);
        }
    }
    
    /**
     * Carga y muestra los datos actuales del usuario en el formulario.
     * Rellena nickname, email, fecha de nacimiento y avatar.
     */
    public void cargarDatosActuales() {
        User user = app.getCurrentUser();
        if (user != null) {
            nicknameField.setText(user.getNickName());
            emailField.setText(user.getEmail());
            birthDatePicker.setValue(user.getBirthDate());
            avatarPath = user.getAvatarPath();
            if (user.getAvatarPath() != null) {
                File avatarFile = new File(user.getAvatarPath());
                if (avatarFile.exists()) {
                    avatarView.setImage(new Image(avatarFile.toURI().toString(), 42, 42, true, true));
                }
            }
        }
    }
    
    /**
     * Navega de vuelta a la pantalla principal del mapa.
     *
     * @param event evento de acción del botón de salir
     */
    @FXML
    private void handleSalir(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            MapaPrincipalController controller = loader.getController();
            controller.setNickname(currentUserNick);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Pantalla principal");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
