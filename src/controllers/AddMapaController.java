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

    /**
     * Inicializa el controlador de añadir mapa.
     * No requiere configuración adicional.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /**
     * Abre un selector de archivos para elegir la imagen del mapa.
     * Muestra el nombre del archivo seleccionado.
     *
     * @param event evento de acción del botón de selección
     */
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
            imagePathLabel.setStyle("-fx-text-fill: #55d6b3; -fx-font-weight: bold");
            
        }
    }

    /**
     * Guarda el nuevo mapa en la base de datos con sus coordenadas.
     * Valida que se haya seleccionado imagen, nombre y coordenadas.
     *
     * @param event evento de acción del botón de guardar
     */
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

    /**
     * Vuelve a la pantalla principal cerrando la ventana actual.
     *
     * @param event evento de acción del botón de volver
     */
    @FXML
    private void handleVolver(ActionEvent event) {
        volverPrincipal();
    }

    /**
     * Cierra la ventana actual del diálogo.
     */
    private void volverPrincipal() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Muestra un diálogo de error con título y mensaje personalizados.
     *
     * @param titulo  título del diálogo
     * @param mensaje contenido del mensaje de error
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
