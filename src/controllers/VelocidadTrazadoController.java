package controllers;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

public class VelocidadTrazadoController implements Initializable {

    @FXML
    private ScrollPane mapScrollPane;

    @FXML
    private Label infoLabel;

    @FXML
    private Slider zoomSlider;

    private Activity actividad;
    private MapProjection projection;
    private Group zoomGroup;
    private Pane mapPane;
    private double escala = 1.0;
    private boolean resetZoom = true;

    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;
    private static final double ZOOM_STEP = 0.15;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoomSlider.setMin(ZOOM_MIN);
        zoomSlider.setMax(ZOOM_MAX);
        zoomSlider.setValue(escala);

        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            escala = newVal.doubleValue();
            aplicarZoom();
        });
    }

    public void setActividad(Activity actividad) {
        this.actividad = actividad;

        if (actividad != null) {
            Platform.runLater(this::dibujarVelocidadSobreMapa);
        }
    }

    private void dibujarVelocidadSobreMapa() {
        mapScrollPane.setContent(null);
        zoomGroup = null;
        mapPane = null;
        projection = null;

        MapRegion region = actividad.getSuggestedMap();

        if (region == null) {
            infoLabel.setText("No hay mapa disponible.");
            return;
        }

        File mapaFile = new File(region.getImagePath());

        if (!mapaFile.exists()) {
            infoLabel.setText("No se encuentra el mapa.");
            return;
        }

        Image mapa = new Image(mapaFile.toURI().toString());
        double W = mapa.getWidth();
        double H = mapa.getHeight();

        Pane innerPane = new Pane();
        innerPane.setPrefSize(W, H);
        innerPane.setMinSize(W, H);
        innerPane.setMaxSize(W, H);

        Rectangle clip = new Rectangle(W, H);
        innerPane.setClip(clip);

        ImageView mapaView = new ImageView(mapa);
        mapaView.setFitWidth(W);
        mapaView.setFitHeight(H);
        mapaView.setPreserveRatio(false);
        innerPane.getChildren().add(mapaView);

        projection = new MapProjection(region, W, H);

        List<TrackPoint> puntos = actividad.getTrackPoints();

        if (puntos == null || puntos.size() < 2) {
            infoLabel.setText("La actividad no tiene suficientes puntos.");
            return;
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double velocidadMaxima = calcularVelocidadMaxima(puntos);

        for (int i = 0; i < puntos.size() - 1; i++) {
            TrackPoint p1 = puntos.get(i);
            TrackPoint p2 = puntos.get(i + 1);

            Point2D punto1 = projection.project(p1);
            Point2D punto2 = projection.project(p2);

            minX = Math.min(minX, Math.min(punto1.getX(), punto2.getX()));
            maxX = Math.max(maxX, Math.max(punto1.getX(), punto2.getX()));
            minY = Math.min(minY, Math.min(punto1.getY(), punto2.getY()));
            maxY = Math.max(maxY, Math.max(punto1.getY(), punto2.getY()));

            double velocidad = calcularVelocidad(p1, p2);

            Line tramo = new Line(
                    punto1.getX(), punto1.getY(),
                    punto2.getX(), punto2.getY()
            );

            tramo.setStroke(colorPorVelocidad(velocidad, velocidadMaxima));
            tramo.setStrokeWidth(7);
            tramo.setOpacity(0.95);

            final double velocidadFinal = velocidad;

            tramo.setOnMouseEntered(event -> {
                infoLabel.setText(String.format("Velocidad: %.2f km/h", velocidadFinal));
                tramo.setStrokeWidth(11);
            });

            tramo.setOnMouseExited(event -> {
                infoLabel.setText("Pasa el ratón sobre un tramo para ver su velocidad.");
                tramo.setStrokeWidth(7);
            });

            innerPane.getChildren().add(tramo);
        }

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

        double finalCentroX = centroX;
        double finalCentroY = centroY;
        Platform.runLater(() -> {
            if (mapPane == null) return;
            double vW = mapScrollPane.getViewportBounds().getWidth();
            double vH = mapScrollPane.getViewportBounds().getHeight();
            double mW = mapPane.getWidth() * escala;
            double mH = mapPane.getHeight() * escala;

            if (mW > vW) {
                double hv = Math.max(0, Math.min(1,
                    (finalCentroX * escala - vW / 2) / (mW - vW)));
                mapScrollPane.setHvalue(hv);
            }
            if (mH > vH) {
                double vv = Math.max(0, Math.min(1,
                    (finalCentroY * escala - vH / 2) / (mH - vH)));
                mapScrollPane.setVvalue(vv);
            }
        });

        infoLabel.setText("Pasa el ratón sobre un tramo para ver su velocidad.");
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

    private double calcularVelocidadMaxima(List<TrackPoint> puntos) {
        double max = 0;

        for (int i = 0; i < puntos.size() - 1; i++) {
            double velocidad = calcularVelocidad(puntos.get(i), puntos.get(i + 1));

            if (velocidad > max) {
                max = velocidad;
            }
        }

        return max;
    }

    private double calcularVelocidad(TrackPoint p1, TrackPoint p2) {
        try {
            double distanciaMetros = p1.distanceTo(p2);
            Duration tiempo = p1.timeTo(p2);

            if (tiempo == null || tiempo.toSeconds() <= 0) {
                return 0;
            }

            double kilometros = distanciaMetros / 1000.0;
            double horas = tiempo.toSeconds() / 3600.0;

            return kilometros / horas;

        } catch (Exception e) {
            return 0;
        }
    }

    private Color colorPorVelocidad(double velocidad, double velocidadMaxima) {

        if (velocidad < 6) {
            return Color.web("#16c7e8");
        }

        if (velocidad < 12) {
            return Color.web("#55d6b3");
        }

        return Color.web("#ef4264");
    }

    @FXML
    private void volver() {
        mapScrollPane.getScene().getWindow().hide();
    }
}
