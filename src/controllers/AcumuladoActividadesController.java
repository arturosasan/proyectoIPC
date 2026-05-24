package controllers;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;

public class AcumuladoActividadesController implements Initializable {

    @FXML private ComboBox<String> comboMes;
    @FXML private Label lblTotalActividades;
    @FXML private Label lblTiempoTotal;
    @FXML private Label lblDistanciaTotal;
    @FXML private Label lblAscensoTotal;
    @FXML private Label lblDescensoTotal;
    @FXML private ListView<String> listaActividades;

    private final SportActivityApp app = SportActivityApp.getInstance();
    private final List<Activity> actividadesDelMes = new ArrayList<>();
    private MapaPrincipalController parentController;
    private static final String TODOS = "Todos";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<String> meses = new ArrayList<>();
        meses.add(TODOS);
        for (String m : List.of("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")) {
            meses.add(m);
        }
        comboMes.setItems(FXCollections.observableArrayList(meses));
        comboMes.getSelectionModel().select(TODOS);

        comboMes.setOnAction(e -> calcularAcumulado());

        listaActividades.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int index = listaActividades.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < actividadesDelMes.size()) {
                    abrirVisualizador(actividadesDelMes.get(index));
                }
            }
        });

        calcularAcumulado();
    }

    private void calcularAcumulado() {
        if (comboMes.getValue() == null) return;

        String mesSeleccionado = comboMes.getValue();

        int totalActividades = 0;
        double distanciaTotal = 0;
        double ascensoTotal = 0;
        double descensoTotal = 0;
        long segundosTotales = 0;

        listaActividades.getItems().clear();
        actividadesDelMes.clear();

        for (Activity actividad : app.getUserActivities()) {
            if (actividad.getStartTime() == null) continue;

            if (!TODOS.equals(mesSeleccionado)) {
                int mes = comboMes.getSelectionModel().getSelectedIndex();
                if (actividad.getStartTime().getMonthValue() != mes) continue;
            }

            totalActividades++;
            actividadesDelMes.add(actividad);

            distanciaTotal += actividad.getTotalDistance();
            ascensoTotal += actividad.getElevationGain();
            descensoTotal += actividad.getElevationLoss();

            if (actividad.getDuration() != null) {
                segundosTotales += actividad.getDuration().getSeconds();
            }

            LocalDate fecha = actividad.getStartTime().toLocalDate();
            java.time.Duration dur = actividad.getDuration();
            long segs = dur != null ? dur.getSeconds() : 0;
            String durStr = String.format("%d:%02d:%02d", segs / 3600, (segs % 3600) / 60, segs % 60);

            listaActividades.getItems().add(
                    String.format("%02d/%02d", fecha.getDayOfMonth(), fecha.getMonthValue())
                    + "  "
                    + actividad.getName()
                    + "  |  "
                    + String.format("%.2f km", actividad.getTotalDistance() / 1000.0)
                    + "  |  "
                    + durStr
                    + "  |  "
                    + String.format("%.1f km/h", actividad.getAverageSpeed())
            );
        }

        lblTotalActividades.setText(String.valueOf(totalActividades));
        lblDistanciaTotal.setText(String.format("%.2f km", distanciaTotal / 1000.0));
        lblAscensoTotal.setText(String.format("%.0f m", ascensoTotal));
        lblDescensoTotal.setText(String.format("%.0f m", descensoTotal));
        lblTiempoTotal.setText(formatearTiempo(segundosTotales));

        if (totalActividades == 0) {
            listaActividades.getItems().add("No hay actividades registradas en este período.");
        }
    }

    private String formatearTiempo(long segundosTotales) {
        long horas = segundosTotales / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;
        return String.format("%02d h %02d min %02d s", horas, minutos, segundos);
    }

    public void setParentController(MapaPrincipalController controller) {
        this.parentController = controller;
    }

    private void abrirVisualizador(Activity actividad) {
        if (parentController != null) {
            parentController.cargarActividad(actividad);
        }
        handleVolver();
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) ((Node) comboMes).getScene().getWindow();
        stage.close();
    }

}
