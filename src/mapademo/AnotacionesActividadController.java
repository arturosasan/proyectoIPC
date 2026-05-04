package mapademo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import upv.ipc.sportlib.Activity;

public class AnotacionesActividadController implements Initializable {

    @FXML private Pane mapPane;

    @FXML private Label labelDistancia;
    @FXML private Label labelDuracion;
    @FXML private Label labelVelocidad;

    @FXML private Button btnAnotacion;

    private Activity actividadActual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        labelDistancia.setText("Distancia: -");
        labelDuracion.setText("Duración: -");
        labelVelocidad.setText("Velocidad: -");

        btnAnotacion.setOnAction(e -> {
            System.out.println("Botón añadir anotación pulsado");
        });
    }

    public void mostrarActividad(Activity actividad) {
        this.actividadActual = actividad;

        labelDistancia.setText(
                String.format("Distancia: %.2f km", actividad.getTotalDistance() / 1000.0)
        );

        labelDuracion.setText(
                "Duración: " + actividad.getDuration().toString()
        );

        labelVelocidad.setText(
                String.format("Velocidad: %.2f km/h", actividad.getAverageSpeed())
        );

        System.out.println("Actividad cargada correctamente");
    }
}