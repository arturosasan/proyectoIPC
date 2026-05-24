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
import javafx.stage.FileChooser;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.Node;
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
    
    @FXML
    private Label messageLabel;
    
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
            mostrarExito("Perfil actualizado correctamente.");
        
            savingInProgress = false;
        } catch (java.time.format.DateTimeParseException e) {
            mostrarError("Fecha de nacimiento inválida.");
        } catch (Exception e) {
            mostrarError("Error al actualizar perfil: " + e.getMessage());
        }
    }
    
    /**
     * Muestra un mensaje de error en el formulario de modificación.
     *
     * @param mensaje texto del error a mostrar
     */
    private void mostrarError(String mensaje) {
        limpiarExito();
        messageLabel.setText(mensaje);
        messageLabel.setStyle("-fx-text-fill: #cc0000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-wrap-text: true;");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
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
        
        messageLabel.setText(mensaje);
        messageLabel.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-wrap-text: true;");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
        
        new Thread(() -> {
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
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }
    
    /**
     * Elimina el mensaje de error del formulario si existe.
     */
    private void limpiarError() {
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
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
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
        }
    }