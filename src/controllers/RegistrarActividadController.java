/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Si esto sale en rojo → Alt + Enter
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.Activity;

public class RegistrarActividadController implements Initializable {

    @FXML private Label archivoSeleccionadoLabel;

    @FXML private Label distanciaLabel;
    @FXML private Label duracionLabel;
    @FXML private Label velocidadLabel;
    @FXML private Label ritmoLabel;
    @FXML private Label desnivelPositivoLabel;
    @FXML private Label desnivelNegativoLabel;
    @FXML private Label altitudMinimaLabel;
    @FXML private Label altitudMaximaLabel;

    private File archivoGPX;

    private SportActivityApp app = SportActivityApp.getInstance();

    /**
     * Inicializa el controlador de registro de actividad.
     * No requiere configuración adicional.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // no hace falta nada aquí
    }

    /**
     * Abre un selector de archivos para elegir un archivo GPX.
     * Almacena el archivo seleccionado y muestra su nombre.
     */
    @FXML
    private void handleSeleccionarGPX() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo GPX");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos GPX", "*.gpx")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            archivoGPX = file;
            archivoSeleccionadoLabel.setText(file.getName());
        }
    }

    /**
     * Importa la actividad desde el archivo GPX seleccionado.
     * Muestra las estadísticas calculadas por la librería SportActivityApp.
     */
    @FXML
    private void handleImportarActividad() {

        if (archivoGPX == null) {
            archivoSeleccionadoLabel.setText("Selecciona un archivo primero");
            return;
        }

        try {
            Activity actividad = app.importActivity(archivoGPX);

            distanciaLabel.setText(String.format("%.2f km", actividad.getTotalDistance()/1000));
            duracionLabel.setText(actividad.getDuration().toString());
            velocidadLabel.setText(String.format("%.2f km/h", actividad.getAverageSpeed()));
            ritmoLabel.setText(String.format("%.2f min/km", actividad.getAveragePace()));
            desnivelPositivoLabel.setText(String.format("%.0f m", actividad.getElevationGain()));
            desnivelNegativoLabel.setText(String.format("%.0f m", actividad.getElevationLoss()));
            altitudMinimaLabel.setText(String.format("%.0f m", actividad.getMinElevation()));
            altitudMaximaLabel.setText(String.format("%.0f m", actividad.getMaxElevation()));

        } catch (Exception e) {
            archivoSeleccionadoLabel.setText("Error al importar GPX");
        }
    }

    /**
     * Navega de vuelta a la pantalla principal del mapa.
     */
    @FXML
    private void handleVolver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) archivoSeleccionadoLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pantalla principal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}