package controllers;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;

public class RegistrarActividadController implements Initializable {

    @FXML private Label estadoLabel;
    @FXML private Label mapaLabel;

    @FXML private Label distanciaLabel;
    @FXML private Label duracionLabel;
    @FXML private Label velocidadLabel;
    @FXML private Label ritmoLabel;
    @FXML private Label desnivelPositivoLabel;
    @FXML private Label desnivelNegativoLabel;
    @FXML private Label altitudMinimaLabel;
    @FXML private Label altitudMaximaLabel;

    @FXML private ScrollPane mapScrollPane;

    private Pane mapPane;
    private Group zoomGroup;

    private final SportActivityApp app = SportActivityApp.getInstance();
    private MapaPrincipalController parentController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        limpiarEstadisticas();

        mapPane = new Pane();
        zoomGroup = new Group(mapPane);
        mapScrollPane.setContent(zoomGroup);
        mapScrollPane.setPannable(true);
    }

    public void setParentController(MapaPrincipalController controller) {
        this.parentController = controller;
    }

    @FXML
    private void handleImportarActividad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo GPX");
        fileChooser.setInitialDirectory(new File("./gpx"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos GPX", "*.gpx")
        );

        File archivo = fileChooser.showOpenDialog(estadoLabel.getScene().getWindow());

        if (archivo == null) return;

        estadoLabel.setText("Importando...");

        try {
            Activity actividad = app.importActivity(archivo);

            if (actividad == null) {
                estadoLabel.setText("No se ha podido importar la actividad.");
                mostrarAlerta("Error", "No se ha podido importar el GPX.");
                return;
            }

            String nombreSinExtension = archivo.getName().replaceAll("(?i)\\.gpx$", "");
            app.renameActivity(actividad, nombreSinExtension);

            estadoLabel.setText("Actividad importada: " + archivo.getName());
            mostrarEstadisticas(actividad);
            dibujarActividad(actividad);

            if (parentController != null) {
                parentController.cargarActividad(actividad);
            }
        } catch (Exception e) {
            e.printStackTrace();
            estadoLabel.setText("Error al importar GPX.");
            mostrarAlerta("Error al importar", "Revisa que el GPX sea válido y que exista el mapa correspondiente.");
        }
    }

    private void mostrarEstadisticas(Activity actividad) {
        distanciaLabel.setText(String.format("%.2f km", actividad.getTotalDistance() / 1000.0));
        duracionLabel.setText(formatearDuracion(actividad.getDuration()));
        velocidadLabel.setText(String.format("%.2f km/h", actividad.getAverageSpeed()));
        ritmoLabel.setText(String.format("%.2f min/km", actividad.getAveragePace()));
        desnivelPositivoLabel.setText(String.format("%.0f m", actividad.getElevationGain()));
        desnivelNegativoLabel.setText(String.format("%.0f m", actividad.getElevationLoss()));
        altitudMinimaLabel.setText(String.format("%.0f m", actividad.getMinElevation()));
        altitudMaximaLabel.setText(String.format("%.0f m", actividad.getMaxElevation()));
    }

    private void dibujarActividad(Activity actividad) {
        mapPane.getChildren().clear();

        MapRegion region = actividad.getSuggestedMap();
        if (region == null) {
            region = app.findMapForActivity(actividad);
        }

        if (region == null) {
            mapaLabel.setText("Sin mapa disponible");
            return;
        }

        File imagenMapa = new File(region.getImagePath());

        if (!imagenMapa.exists()) {
            mapaLabel.setText("No se encuentra: " + region.getImagePath());
            mostrarAlerta("Mapa no encontrado", "Falta la imagen del mapa: " + region.getImagePath());
            return;
        }

        Image image = new Image(imagenMapa.toURI().toString());
        ImageView imageView = new ImageView(image);

        mapPane.setPrefSize(image.getWidth(), image.getHeight());
        mapPane.setMinSize(image.getWidth(), image.getHeight());
        mapPane.setMaxSize(image.getWidth(), image.getHeight());

        mapPane.getChildren().add(imageView);

        MapProjection projection = new MapProjection(region, image.getWidth(), image.getHeight());

        Polyline ruta = new Polyline();
        ruta.setStroke(Color.web("#55d6b3"));
        ruta.setStrokeWidth(4);

        List<TrackPoint> puntos = actividad.getTrackPoints();

        for (TrackPoint punto : puntos) {
            Point2D p = projection.project(punto);
            ruta.getPoints().addAll(p.getX(), p.getY());
        }

        mapPane.getChildren().add(ruta);

        if (!puntos.isEmpty()) {
            Point2D inicio = projection.project(actividad.getStartPoint());
            Point2D fin = projection.project(actividad.getEndPoint());

            Circle inicioCircle = new Circle(inicio.getX(), inicio.getY(), 7);
            inicioCircle.setFill(Color.LIMEGREEN);
            inicioCircle.setStroke(Color.WHITE);
            inicioCircle.setStrokeWidth(2);

            Circle finCircle = new Circle(fin.getX(), fin.getY(), 7);
            finCircle.setFill(Color.RED);
            finCircle.setStroke(Color.WHITE);
            finCircle.setStrokeWidth(2);

            mapPane.getChildren().addAll(inicioCircle, finCircle);
        }

        mapaLabel.setText(region.getName());
    }

    @FXML
    private void handleVolver() {
        Stage stage = (Stage) estadoLabel.getScene().getWindow();
        stage.close();
    }

    private void limpiarEstadisticas() {
        distanciaLabel.setText("-");
        duracionLabel.setText("-");
        velocidadLabel.setText("-");
        ritmoLabel.setText("-");
        desnivelPositivoLabel.setText("-");
        desnivelNegativoLabel.setText("-");
        altitudMinimaLabel.setText("-");
        altitudMaximaLabel.setText("-");
    }

    private String formatearDuracion(Duration duration) {
        long segundos = duration.getSeconds();
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long seg = segundos % 60;

        return String.format("%02d:%02d:%02d", horas, minutos, seg);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
