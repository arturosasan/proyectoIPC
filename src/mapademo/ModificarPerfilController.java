/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.File;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.DatePicker;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.SportActivityApp; // asegúrate del paquete correcto

public class ModificarPerfilController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private DatePicker birthDatePicker;

    private String avatarPath = null;

    private SportActivityApp app = SportActivityApp.getInstance();

    // 🔹 Botón guardar
    @FXML
    private void handleGuardar() {

        String email = emailField.getText();
        String password = passwordField.getText();
        LocalDate birthDate = birthDatePicker.getValue();

        // Si no escribe contraseña → mantener la actual
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

    // 🔹 Botón elegir avatar
    @FXML
    private void handleElegirAvatar() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar avatar");

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            avatarPath = file.getAbsolutePath();
            System.out.println("Avatar seleccionado: " + avatarPath);
        }
    }
}