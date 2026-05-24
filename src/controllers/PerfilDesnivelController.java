package controllers;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

public class PerfilDesnivelController implements Initializable {

    @FXML private ScrollPane mapScrollPane;
    @FXML private Slider zoomSlider;
    @FXML private LineChart<Number, Number> graficaDesnivel;
    @FXML private NumberAxis ejeDistancia;
    @FXML private NumberAxis ejeAltitud;
    @FXML private Label lblInfoPunto;

    private Activity actividad;
    private MapProjection projection;
    private Group zoomGroup;
    private Pane mapPane;
    private Circle puntoMapa;
    private double escala = 1.0;

    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;
    private static final double ZOOM_STEP = 0.15;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graficaDesnivel.setTitle("Perfil de altitud");
        graficaDesnivel.setLegendVisible(false);
        graficaDesnivel.setAnimated(false);

        ejeDistancia.setLabel("Distancia (km)");
        ejeAltitud.setLabel("Altitud (m)");

        zoomSlider.setMin(ZOOM_MIN);
        zoomSlider.setMax(ZOOM_MAX);
        zoomSlider.setValue(escala);

        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            escala = newVal.doubleValue();
            aplicarZoom();
        });

        Platform.runLater(() -> {
            Node title = graficaDesnivel.lookup(".chart-title");
            if (title instanceof Label label) {
                label.setTextFill(Color.WHITE);
            }
        });
    }

    public void setActividad(Activity actividad) {
        this.actividad = actividad;

        if (actividad == null) {
            lblInfoPunto.setText("No hay actividad seleccionada");
            return;
        }

        Platform.runLater(() -> {
            cargarMapa();
            cargarGrafica();
        });
    }

    @FXML
    private void zoomIn() {
        if (zoomGroup == null) return;
        zoomSlider.setValue(Math.min(escala + ZOOM_STEP, ZOOM_MAX));
    }

    @FXML
    private void zoomOut() {
        if (zoomGroup == null) return;
        zoomSlider.setValue(Math.max(escala - ZOOM_STEP, ZOOM_MIN));
    }

    private void aplicarZoom() {
        if (zoomGroup == null) return;
        double h = mapScrollPane.getHvalue();
        double v = mapScrollPane.getVvalue();
        zoomGroup.setScaleX(escala);
        zoomGroup.setScaleY(escala);
        mapScrollPane.setHvalue(h);
        mapScrollPane.setVvalue(v);
    }

    private void onMapScroll(ScrollEvent event) {
        if (zoomGroup == null) return;
        event.consume();
        double oldScale = zoomGroup.getScaleX();
        double newScale = oldScale * Math.exp(event.getDeltaY() * 0.01);
        newScale = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, newScale));
        if (Math.abs(newScale - oldScale) < 0.001) return;

        double mx = event.getX(), my = event.getY();
        double vw = mapScrollPane.getViewportBounds().getWidth();
        double vh = mapScrollPane.getViewportBounds().getHeight();
        double cw = mapPane.getWidth() * oldScale;
        double ch = mapPane.getHeight() * oldScale;
        double sx = (cw - vw) > 0 ? mapScrollPane.getHvalue() * (cw - vw) : 0;
        double sy = (ch - vh) > 0 ? mapScrollPane.getVvalue() * (ch - vh) : 0;

        double mmx = sx + mx, mmy = sy + my;

        zoomGroup.setScaleX(newScale);
        zoomGroup.setScaleY(newScale);
        mapScrollPane.layout();

        double ratio = newScale / oldScale;
        double ncw = mapPane.getWidth() * newScale;
        double nch = mapPane.getHeight() * newScale;
        double nsx = mmx * ratio - mx;
        double nsy = mmy * ratio - my;

        if (ncw - vw > 0)
            mapScrollPane.setHvalue(Math.max(0, Math.min(1, nsx / (ncw - vw))));
        if (nch - vh > 0)
            mapScrollPane.setVvalue(Math.max(0, Math.min(1, nsy / (nch - vh))));

        escala = newScale;
        zoomSlider.setValue(newScale);
    }

    private void cargarMapa() {
        mapScrollPane.setContent(null);
        zoomGroup = null;
        mapPane = null;
        projection = null;

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
        double W = mapa.getWidth();
        double H = mapa.getHeight();

        Pane innerPane = new Pane();
        innerPane.setPrefSize(W, H);
        innerPane.setMinSize(W, H);
        innerPane.setMaxSize(W, H);

        Rectangle clip = new Rectangle(W, H);
        innerPane.setClip(clip);

        ImageView imageView = new ImageView(mapa);
        imageView.setFitWidth(W);
        imageView.setFitHeight(H);
        imageView.setPreserveRatio(false);

        projection = new MapProjection(region, W, H);

        Polyline ruta = new Polyline();
        ruta.setStroke(Color.web("#55D6B2"));
        ruta.setStrokeWidth(4);

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        List<TrackPoint> puntos = actividad.getTrackPoints();

        if (puntos != null) {
            for (TrackPoint tp : puntos) {
                Point2D punto = projection.project(tp);
                ruta.getPoints().addAll(punto.getX(), punto.getY());
                minX = Math.min(minX, punto.getX());
                maxX = Math.max(maxX, punto.getX());
                minY = Math.min(minY, punto.getY());
                maxY = Math.max(maxY, punto.getY());
            }
        }

        puntoMapa = new Circle(0, 0, 8);
        puntoMapa.setFill(Color.web("#EF4264"));
        puntoMapa.setStroke(Color.WHITE);
        puntoMapa.setStrokeWidth(2);
        puntoMapa.setVisible(false);

        innerPane.getChildren().addAll(imageView, ruta, puntoMapa);
        innerPane.setOnScroll(this::onMapScroll);
        mapPane = innerPane;

        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);
        mapScrollPane.setContent(contentGroup);

        double centroX = (minX + maxX) / 2;
        double centroY = (minY + maxY) / 2;
        double routeW = Math.max(maxX - minX, 1);
        double routeH = Math.max(maxY - minY, 1);

        double displayW = mapScrollPane.getViewportBounds().getWidth() > 0
            ? mapScrollPane.getViewportBounds().getWidth() : 700;
        double displayH = mapScrollPane.getViewportBounds().getHeight() > 0
            ? mapScrollPane.getViewportBounds().getHeight() : 500;

        double padding = 1.3;
        double escalaRuta = Math.min(displayW / (routeW * padding), displayH / (routeH * padding));
        escala = Math.max(ZOOM_MIN, Math.min(escalaRuta, ZOOM_MAX));
        zoomSlider.setValue(escala);

        Platform.runLater(() -> {
            if (mapPane == null) return;
            double vW = mapScrollPane.getViewportBounds().getWidth();
            double vH = mapScrollPane.getViewportBounds().getHeight();
            double mW = mapPane.getWidth() * escala;
            double mH = mapPane.getHeight() * escala;

            if (mW > vW) {
                double hv = Math.max(0, Math.min(1,
                    (centroX * escala - vW / 2) / (mW - vW)));
                mapScrollPane.setHvalue(hv);
            }
            if (mH > vH) {
                double vv = Math.max(0, Math.min(1,
                    (centroY * escala - vH / 2) / (mH - vH)));
                mapScrollPane.setVvalue(vv);
            }
        });
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
        mapScrollPane.getScene().getWindow().hide();
    }
}
