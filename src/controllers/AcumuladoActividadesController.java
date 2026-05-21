package controllers;

import java.net.URL;
import java.time.Month;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

public class AcumuladoActividadesController implements Initializable {

    @FXML private ComboBox<String> comboMes;
    @FXML private ComboBox<Integer> comboAnio;

    @FXML private Label lblTotalActividades;
    @FXML private Label lblTiempoTotal;
    @FXML private Label lblDistanciaTotal;
    @FXML private Label lblAscensoTotal;
    @FXML private Label lblDescensoTotal;

    @FXML private ListView<String> listaActividades;

    private final SportActivityApp app = SportActivityApp.getInstance();

    /**
     * Inicializa el controlador.
     * Configura los ComboBox de mes y año con valores por defecto y
     * calcula el acumulado inicial automáticamente.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        comboMes.setItems(FXCollections.observableArrayList(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        ));

        comboAnio.setItems(FXCollections.observableArrayList(
                2024, 2025, 2026, 2027
        ));

        comboMes.getSelectionModel().select(java.time.LocalDate.now().getMonthValue() - 1);
        comboAnio.setValue(java.time.LocalDate.now().getYear());

        comboMes.setOnAction(e -> calcularAcumulado());
        comboAnio.setOnAction(e -> calcularAcumulado());

        calcularAcumulado();
    }

    /**
     * Calcula y muestra las estadísticas acumuladas del mes y año seleccionados.
     * Filtra las actividades del usuario por mes/año y actualiza las etiquetas
     * con totales de distancia, tiempo, ascenso y descenso.
     */
    private void calcularAcumulado() {

        if (comboMes.getValue() == null || comboAnio.getValue() == null) {
            return;
        }

        int mes = comboMes.getSelectionModel().getSelectedIndex() + 1;
        int anio = comboAnio.getValue();

        int totalActividades = 0;
        double distanciaTotal = 0;
        double ascensoTotal = 0;
        double descensoTotal = 0;
        long segundosTotales = 0;

        listaActividades.getItems().clear();

        for (Activity actividad : app.getUserActivities()) {

            if (actividad.getStartTime() == null) {
                continue;
            }

            int mesActividad = actividad.getStartTime().getMonthValue();
            int anioActividad = actividad.getStartTime().getYear();

            if (mesActividad == mes && anioActividad == anio) {

                totalActividades++;

                distanciaTotal += actividad.getTotalDistance();
                ascensoTotal += actividad.getElevationGain();
                descensoTotal += actividad.getElevationLoss();

                if (actividad.getDuration() != null) {
                    segundosTotales += actividad.getDuration().getSeconds();
                }

                listaActividades.getItems().add(
                        actividad.getName()
                        + "  |  "
                        + String.format("%.2f km", actividad.getTotalDistance() / 1000.0)
                        + "  |  "
                        + actividad.getDuration()
                );
            }
        }

        lblTotalActividades.setText(String.valueOf(totalActividades));
        lblDistanciaTotal.setText(String.format("%.2f km", distanciaTotal / 1000.0));
        lblAscensoTotal.setText(String.format("%.0f m", ascensoTotal));
        lblDescensoTotal.setText(String.format("%.0f m", descensoTotal));
        lblTiempoTotal.setText(formatearTiempo(segundosTotales));

        if (totalActividades == 0) {
            listaActividades.getItems().add("No hay actividades registradas en este mes.");
        }
    }

    /**
     * Convierte una cantidad de segundos a formato legible "HH h MM min SS s".
     *
     * @param segundosTotales total de segundos a formatear
     * @return cadena formateada con horas, minutos y segundos
     */
    private String formatearTiempo(long segundosTotales) {

        long horas = segundosTotales / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;

        return String.format("%02d h %02d min %02d s", horas, minutos, segundos);
    }

    @FXML
    private void handleVolver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) comboMes).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pantalla principal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}