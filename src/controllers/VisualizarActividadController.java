package controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;

public class VisualizarActividadController implements Initializable {

    @FXML private ComboBox<Activity> actividadComboBox;
    @FXML private AnchorPane mapPane;
    @FXML private Label distanciaLabel;
    @FXML private Label duracionLabel;
    @FXML private Label velocidadLabel;
    @FXML private Label desnivelLabel;

    private final SportActivityApp app = SportActivityApp.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        actividadComboBox.setConverter(new StringConverter<Activity>() {
            @Override
            public String toString(Activity actividad) {
                if (actividad == null) return "";
                return actividad.getName();
            }

            @Override
            public Activity fromString(String string) {
                return null;
            }
        });

        List<Activity> actividades = app.getUserActivities();
        actividadComboBox.getItems().setAll(actividades);

        if (!actividades.isEmpty()) {
            actividadComboBox.setValue(actividades.get(0));
            mostrarActividad(actividades.get(0));
        }
    }

    @FXML
    private void cambiarActividad() {
        Activity actividad = actividadComboBox.getValue();

        if (actividad != null) {
            mostrarActividad(actividad);
        }
    }

    private void mostrarActividad(Activity actividad) {
        mapPane.getChildren().clear();

        MapRegion region = actividad.getSuggestedMap();
        if (region == null) return;

        File mapaFile = new File(region.getImagePath());
        if (!mapaFile.exists()) return;

        double ancho = mapPane.getPrefWidth();
        double alto = mapPane.getPrefHeight();

        Image mapa = new Image(mapaFile.toURI().toString());

        ImageView mapaView = new ImageView(mapa);
        mapaView.setFitWidth(ancho);
        mapaView.setFitHeight(alto);
        mapaView.setPreserveRatio(false);

        mapPane.getChildren().add(mapaView);

        MapProjection projection = new MapProjection(region, ancho, alto);

        Polyline ruta = new Polyline();
        ruta.setStroke(Color.web("#55d6b3"));
        ruta.setStrokeWidth(5);

        for (TrackPoint tp : actividad.getTrackPoints()) {
            Point2D p = projection.project(tp);
            ruta.getPoints().addAll(p.getX(), p.getY());
        }

        mapPane.getChildren().add(ruta);

        Point2D inicio = projection.project(actividad.getStartPoint());
        Point2D fin = projection.project(actividad.getEndPoint());

        Circle cInicio = new Circle(inicio.getX(), inicio.getY(), 7);
        cInicio.setFill(Color.LIMEGREEN);

        Circle cFin = new Circle(fin.getX(), fin.getY(), 7);
        cFin.setFill(Color.RED);

        mapPane.getChildren().addAll(cInicio, cFin);

        distanciaLabel.setText(String.format("Distancia: %.2f km", actividad.getTotalDistance() / 1000.0));
        duracionLabel.setText("Duración: " + formatearDuracion(actividad.getDuration().getSeconds()));
        velocidadLabel.setText(String.format("Velocidad: %.2f km/h", actividad.getAverageSpeed()));
        desnivelLabel.setText(String.format("Desnivel+: %.0f m", actividad.getElevationGain()));
    }

    private String formatearDuracion(long segundosTotales) {
        long horas = segundosTotales / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;

        if (horas > 0) {
            return String.format("%d h %02d min %02d s", horas, minutos, segundos);
        }

        return String.format("%d min %02d s", minutos, segundos);
    }

    @FXML
    private void abrirPerfilDesnivel() {
        Activity actividad = actividadComboBox.getValue();
        if (actividad == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PerfilDesnivel.fxml"));
            Parent root = loader.load();

            PerfilDesnivelController controller = loader.getController();
            controller.setActividad(actividad);

            Stage stage = new Stage();
            stage.setTitle("Perfil de desnivel");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void abrirVelocidad() {
        Activity actividad = actividadComboBox.getValue();
        if (actividad == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/VelocidadTrazado.fxml"));
            Parent root = loader.load();

            VelocidadTrazadoController controller = loader.getController();
            controller.setActividad(actividad);

            Stage stage = new Stage();
            stage.setTitle("Velocidad sobre trazado");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    

    @FXML
    private void handleVolver() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/MapaPrincipal.fxml"));

            Stage stage = (Stage) mapPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Running la Safor");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}