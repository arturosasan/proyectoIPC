/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

public class VisualizarActividadController implements Initializable {

    @FXML private ComboBox<Activity> actividadComboBox;
    @FXML private AnchorPane mapPane;

    @FXML private Label distanciaLabel;
    @FXML private Label duracionLabel;
    @FXML private Label velocidadLabel;
    @FXML private Label ritmoLabel;
    @FXML private Label desnivelPositivoLabel;
    @FXML private Label desnivelNegativoLabel;
    @FXML private Label altitudMinimaLabel;
    @FXML private Label altitudMaximaLabel;

    private final SportActivityApp app = SportActivityApp.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        actividadComboBox.setItems(
                FXCollections.observableArrayList(app.getUserActivities())
        );
    }

    @FXML
    private void handleSeleccionarActividad() {
        Activity actividad = actividadComboBox.getValue();

        if (actividad == null) {
            return;
        }

        distanciaLabel.setText(String.format("%.2f km", actividad.getTotalDistance() / 1000.0));
        duracionLabel.setText(actividad.getDuration().toString());
        velocidadLabel.setText(String.format("%.2f km/h", actividad.getAverageSpeed()));
        ritmoLabel.setText(String.format("%.2f min/km", actividad.getAveragePace()));
        desnivelPositivoLabel.setText(String.format("%.0f m", actividad.getElevationGain()));
        desnivelNegativoLabel.setText(String.format("%.0f m", actividad.getElevationLoss()));
        altitudMinimaLabel.setText(String.format("%.0f m", actividad.getMinElevation()));
        altitudMaximaLabel.setText(String.format("%.0f m", actividad.getMaxElevation()));

        mapPane.getChildren().clear();

        Label placeholder = new Label("Aquí se mostrará el mapa y la ruta");
        placeholder.setStyle("-fx-text-fill: #e8edf5;");
        placeholder.setLayoutX(105);
        placeholder.setLayoutY(105);

        mapPane.getChildren().add(placeholder);
    }

    @FXML
    private void handleVolver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mapPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pantalla principal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}