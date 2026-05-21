package controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;

public class AddMapaController implements Initializable {

    @FXML
    private GridPane gridPane;

    @FXML
    private TextField nameField;

    @FXML
    private TextField latMinField;

    @FXML
    private TextField latMaxField;

    @FXML
    private TextField lonMinField;

    @FXML
    private TextField lonMaxField;

    @FXML
    private Label imagePathLabel;

    private File selectedImage;

    private final SportActivityApp app = SportActivityApp.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    private void handleSelectImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen del mapa");
        fc.setInitialDirectory(new File("./maps"));
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Mapas", "*.png", "*.jpg")
        );
        File file = fc.showOpenDialog(imagePathLabel.getScene().getWindow());
        if (file != null) {
            selectedImage = file;
            imagePathLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSaveMap(ActionEvent event) {
        if (selectedImage == null) {
            mostrarAlerta("Falta la imagen", "Selecciona una imagen de mapa primero.");
            return;
        }

        String nombre = nameField.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAlerta("Nombre vacío", "Introduce un nombre para el mapa.");
            return;
        }

        try {
            double latMin = Double.parseDouble(latMinField.getText().trim());
            double latMax = Double.parseDouble(latMaxField.getText().trim());
            double lonMin = Double.parseDouble(lonMinField.getText().trim());
            double lonMax = Double.parseDouble(lonMaxField.getText().trim());

            MapRegion region = app.addMapRegion(nombre, selectedImage, latMin, latMax, lonMin, lonMax);
            if (region != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Mapa \"" + nombre + "\" añadido correctamente.");
                alert.showAndWait();
                volverPrincipal();
            } else {
                mostrarAlerta("Error", "No se pudo añadir el mapa. El nombre puede existir o la imagen no es válida.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Coordenadas inválidas", "Revisa que todas las coordenadas sean números válidos.");
        }
    }

    @FXML
    private void handleVolver(ActionEvent event) {
        volverPrincipal();
    }

    private void volverPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pantalla principal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
