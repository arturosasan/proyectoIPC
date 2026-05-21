package controllers;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

public class PerfilDesnivelController implements Initializable {

    @FXML private Pane mapContainer;
    @FXML private LineChart<Number, Number> graficaDesnivel;
    @FXML private NumberAxis ejeDistancia;
    @FXML private NumberAxis ejeAltitud;
    @FXML private Label lblInfoPunto;

    private Activity actividad;
    private MapProjection projection;
    private Circle puntoMapa;

    private static final double MAP_WIDTH = 1400;
    private static final double MAP_HEIGHT = 500;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graficaDesnivel.setTitle("Perfil de altitud");
        graficaDesnivel.setLegendVisible(false);
        graficaDesnivel.setAnimated(false);

        ejeDistancia.setLabel("Distancia (km)");
        ejeAltitud.setLabel("Altitud (m)");
    }

    public void setActividad(Activity actividad) {
        this.actividad = actividad;

        if (actividad == null) {
            lblInfoPunto.setText("No hay actividad seleccionada");
            return;
        }

        cargarMapa();
        cargarGrafica();
    }

    private void cargarMapa() {
        try {
            MapRegion region = actividad.getSuggestedMap();

            if (region == null) {
                lblInfoPunto.setText("No hay mapa disponible");
                return;
            }

            File archivoMapa = new File(region.getImagePath());

            if (!archivoMapa.exists()) {
                lblInfoPunto.setText("Mapa no encontrado");
                return;
            }

            Image mapa = new Image(archivoMapa.toURI().toString());

            ImageView imageView = new ImageView(mapa);
            imageView.setFitWidth(MAP_WIDTH);
            imageView.setFitHeight(MAP_HEIGHT);
            imageView.setPreserveRatio(false);

            projection = new MapProjection(region, MAP_WIDTH, MAP_HEIGHT);

            Polyline ruta = new Polyline();
            ruta.setStroke(Color.web("#55D6B2"));
            ruta.setStrokeWidth(4);

            List<TrackPoint> puntos = actividad.getTrackPoints();

            if (puntos != null) {
                for (TrackPoint tp : puntos) {
                    Point2D punto = projection.project(tp);
                    ruta.getPoints().addAll(punto.getX(), punto.getY());
                }
            }

            puntoMapa = new Circle(0, 0, 8);
            puntoMapa.setFill(Color.web("#EF4264"));
            puntoMapa.setStroke(Color.WHITE);
            puntoMapa.setStrokeWidth(2);
            puntoMapa.setVisible(false);

            mapContainer.getChildren().clear();
            mapContainer.getChildren().addAll(imageView, ruta, puntoMapa);

        } catch (Exception e) {
            lblInfoPunto.setText("Error al cargar mapa");
            e.printStackTrace();
        }
    }

    private void cargarGrafica() {
        List<TrackPoint> puntos = actividad.getTrackPoints();

        if (puntos == null || puntos.size() < 2) {
            lblInfoPunto.setText("No hay suficientes datos");
            return;
        }

        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        double distancia = 0;

        for (int i = 0; i < puntos.size(); i++) {
            TrackPoint actual = puntos.get(i);

            if (i > 0) {
                TrackPoint anterior = puntos.get(i - 1);
                distancia += anterior.distanceTo(actual);
            }

            double distanciaKm = distancia / 1000.0;
            double altitud = actual.getElevation();

            XYChart.Data<Number, Number> dato = new XYChart.Data<>(distanciaKm, altitud);

            final int indice = i;
            final double distanciaFinal = distanciaKm;
            final double altitudFinal = altitud;

            dato.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setOnMouseEntered(event -> {
                        TrackPoint tp = puntos.get(indice);
                        destacarPunto(tp);

                        lblInfoPunto.setText(
                                String.format("Distancia: %.2f km | Altitud: %.0f m",
                                        distanciaFinal, altitudFinal)
                        );
                    });

                    newNode.setOnMouseExited(event -> {
                        if (puntoMapa != null) {
                            puntoMapa.setVisible(false);
                        }

                        lblInfoPunto.setText("Pasa el ratón sobre la gráfica");
                    });
                }
            });

            serie.getData().add(dato);
        }

        graficaDesnivel.getData().clear();
        graficaDesnivel.getData().add(serie);
    }

    private void destacarPunto(TrackPoint tp) {
        if (projection == null || puntoMapa == null || tp == null) {
            return;
        }

        Point2D p = projection.project(tp);

        puntoMapa.setCenterX(p.getX());
        puntoMapa.setCenterY(p.getY());
        puntoMapa.setVisible(true);
    }

    @FXML
    private void volver() {
        Stage stage = (Stage) mapContainer.getScene().getWindow();
        stage.close();
    }
}