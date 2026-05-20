/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // no hace falta nada aquí
    }

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
}