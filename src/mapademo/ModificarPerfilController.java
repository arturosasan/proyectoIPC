package mapademo;
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
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;
public class ModificarPerfilController implements Initializable {
    
    private String currentUserNick;
    @FXML
    private Button salirButton;
    @FXML
    private TextField nicknameField;
    
    public void setNickname(String nick) {
        this.currentUserNick = nick;
    }
    
    @FXML
    private GridPane gridPane;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    @FXML
    private ImageView eye;
    @FXML
    private Button togglePasswordBtn;
    @FXML
    private DatePicker birthDatePicker;
    
    @FXML
    private ImageView avatarView;
    private String avatarPath = null;
    private SportActivityApp app = SportActivityApp.getInstance();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Tu contraseña");
        passwordVisibleField.setPrefHeight(28);
        passwordVisibleField.setPrefWidth(165);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        GridPane.setColumnIndex(passwordVisibleField, 1);
        GridPane.setRowIndex(passwordVisibleField, 1);
        GridPane.setMargin(passwordVisibleField, new Insets(-5, 0, 0, 0));
        gridPane.getChildren().add(passwordVisibleField);
        passwordField.textProperty().bindBidirectional(passwordVisibleField.textProperty());
        
        // Cargar avatar actual al abrir la ventana
        
        cargarDatosActuales();
    }
    @FXML
    private void toggleEye(ActionEvent event) {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/eyeOFF.png")));
        } else {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
            eye.setImage(new Image(getClass().getResourceAsStream("/resources/eyeON.png")));
        }
    }
    @FXML
    private void handleGuardar() {
        String email = emailField.getText();
        String password = passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
        LocalDate birthDate = birthDatePicker.getValue();
        if (password == null || password.isEmpty()) {
            password = app.getCurrentUser().getPassword();
        }
        try {
            app.updateCurrentUser(email, password, birthDate, avatarPath);
            System.out.println("Perfil actualizado correctamente");
        } catch (Exception e) {
            System.out.println("Error al actualizar perfil: " + e.getMessage());
        }
    }
    @FXML
    private void handleElegirAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar avatar");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            avatarPath = file.getAbsolutePath();
            // Actualizar la vista del avatar con la nueva imagen
            Image avatarImage = new Image(file.toURI().toString(), 50, 50, true, true);
            avatarView.setImage(avatarImage);
            System.out.println("Avatar seleccionado: " + avatarPath);
        }
    }
    

    
    public void cargarDatosActuales() {
        User user = app.getCurrentUser();
        if (user != null) {
            nicknameField.setText(user.getNickName());
            nicknameField.setEditable(false);
            emailField.setText(user.getEmail());
            birthDatePicker.setValue(user.getBirthDate());
            avatarPath = user.getAvatarPath();  // Guardar la ruta actual
        
            // Cargar imagen del avatar
            if (user.getAvatarPath() != null) {
                File avatarFile = new File(user.getAvatarPath());
                if (avatarFile.exists()) {
                    avatarView.setImage(new Image(avatarFile.toURI().toString(), 57, 54, true, true));
                }
            }
        }
    }
    
    
    @FXML
    private void handleSalir(ActionEvent event) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
        Parent root = loader.load();
        
        FXMLDocumentController controller = loader.getController();
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