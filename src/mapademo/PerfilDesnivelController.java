/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package mapademo;

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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

public class PerfilDesnivelController implements Initializable {

    @FXML
    private StackPane mapContainer;

    @FXML
    private LineChart<Number, Number> graficaDesnivel;

    @FXML
    private NumberAxis ejeDistancia;

    @FXML
    private NumberAxis ejeAltitud;

    @FXML
    private Label lblInfoPunto;

    private Activity actividad;
    private MapProjection projection;
    private Circle puntoMapa;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarGrafica();
    }

    public void setActividad(Activity actividad) {
        this.actividad = actividad;

        if (actividad != null) {
            cargarMapa();
            cargarGraficaDesnivel();
        }
    }

    private void configurarGrafica() {
        graficaDesnivel.setTitle("Perfil de altitud");
        graficaDesnivel.setLegendVisible(false);
        graficaDesnivel.setAnimated(false);

        ejeDistancia.setLabel("Distancia (km)");
        ejeAltitud.setLabel("Altitud (m)");
    }

    private void cargarMapa() {
        try {
            MapRegion region = actividad.getSuggestedMap();

            Image mapa = new Image(new File(region.getImagePath()).toURI().toString());
            ImageView mapView = new ImageView(mapa);

            projection = new MapProjection(region, mapa.getWidth(), mapa.getHeight());

            Polyline ruta = new Polyline();

            for (TrackPoint tp : actividad.getTrackPoints()) {
                Point2D p = projection.project(tp);
                ruta.getPoints().addAll(p.getX(), p.getY());
            }

            ruta.setStroke(Color.web("#55D6B2"));
            ruta.setStrokeWidth(3);

            puntoMapa = new Circle(7);
            puntoMapa.setFill(Color.web("#EF4264"));
            puntoMapa.setStroke(Color.WHITE);
            puntoMapa.setStrokeWidth(2);
            puntoMapa.setVisible(false);

            mapContainer.getChildren().clear();
            mapContainer.getChildren().addAll(mapView, ruta, puntoMapa);

        } catch (Exception e) {
            lblInfoPunto.setText("Error al cargar el mapa");
            e.printStackTrace();
        }
    }

    private void cargarGraficaDesnivel() {
        List<TrackPoint> puntos = actividad.getTrackPoints();

        if (puntos == null || puntos.size() < 2) {
            lblInfoPunto.setText("No hay suficientes puntos en la actividad");
            return;
        }

        XYChart.Series<Number, Number> serie = new XYChart.Series<>();

        double distanciaAcumulada = 0;

        for (int i = 0; i < puntos.size(); i++) {
            TrackPoint actual = puntos.get(i);

            if (i > 0) {
                TrackPoint anterior = puntos.get(i - 1);
                distanciaAcumulada += anterior.distanceTo(actual);
            }

            double distanciaKm = distanciaAcumulada / 1000.0;
            double altitud = actual.getElevation();

            XYChart.Data<Number, Number> dato =
                    new XYChart.Data<>(distanciaKm, altitud);

            final int indice = i;
            final double distanciaFinal = distanciaKm;
            final double altitudFinal = altitud;

            dato.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setOnMouseEntered(event -> {
                        destacarPuntoEnMapa(puntos.get(indice));

                        lblInfoPunto.setText(
                                String.format(
                                        "Distancia: %.2f km | Altitud: %.0f m",
                                        distanciaFinal,
                                        altitudFinal
                                )
                        );
                    });

                    newNode.setOnMouseExited(event -> {
                        if (puntoMapa != null) {
                            puntoMapa.setVisible(false);
                        }

                        lblInfoPunto.setText("Pasa el ratón por la gráfica");
                    });
                }
            });

            serie.getData().add(dato);
        }

        graficaDesnivel.getData().clear();
        graficaDesnivel.getData().add(serie);
    }

    private void destacarPuntoEnMapa(TrackPoint tp) {
        if (projection == null || puntoMapa == null) {
            return;
        }

        Point2D p = projection.project(tp);

        puntoMapa.setTranslateX(p.getX() - mapContainer.getWidth() / 2);
        puntoMapa.setTranslateY(p.getY() - mapContainer.getHeight() / 2);
        puntoMapa.setVisible(true);
    }

    @FXML
    private void volver() {
        mapContainer.getScene().getWindow().hide();
    }
}