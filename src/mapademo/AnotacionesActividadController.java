package mapademo;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;

public class AnotacionesActividadController implements Initializable {

    @FXML private Pane mapPane;
    @FXML private ImageView mapImageView;

    @FXML private Label lblDistancia;
    @FXML private Label lblDuracion;
    @FXML private Label lblVelocidad;
    @FXML private Label lblSinAnotaciones;

    @FXML private Button btnAnadirAnotacion;
    @FXML private Button btnZoomMas;
    @FXML private Button btnZoomMenos;

    @FXML private ListView<String> listaAnotaciones;

    private Activity actividadActual;
    private final SportActivityApp app = SportActivityApp.getInstance();

    private MapProjection projection;
    private AnnotationType tipoPendiente;
    private String textoPendiente;
    private String colorPendiente;
    private GeoPoint primerPuntoPendiente;

    private double escala = 1.0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        lblDistancia.setText("Distancia: -");
        lblDuracion.setText("Duración: -");
        lblVelocidad.setText("Velocidad: -");
        lblSinAnotaciones.setVisible(true);

        btnAnadirAnotacion.setOnAction(e -> prepararNuevaAnotacion());

        btnZoomMas.setOnAction(e -> {
            escala += 0.1;
            mapPane.setScaleX(escala);
            mapPane.setScaleY(escala);
        });

        btnZoomMenos.setOnAction(e -> {
            if (escala > 0.3) {
                escala -= 0.1;
                mapPane.setScaleX(escala);
                mapPane.setScaleY(escala);
            }
        });

        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                procesarClickMapa(e.getX(), e.getY());
            }
        });
    }

    public void mostrarActividad(Activity actividad) {

        this.actividadActual = actividad;

        lblDistancia.setText(
                String.format("Distancia: %.2f km",
                        actividad.getTotalDistance() / 1000.0)
        );

        lblDuracion.setText(
                "Duración: " + actividad.getDuration().toString()
        );

        lblVelocidad.setText(
                String.format("Velocidad: %.2f km/h",
                        actividad.getAverageSpeed())
        );

        dibujarMapaRutaYAnotaciones();
        cargarListaAnotaciones();

        System.out.println("Actividad cargada correctamente");
    }

    private void prepararNuevaAnotacion() {

        if (actividadActual == null) {
            mostrarAviso("Primero debes cargar una actividad.");
            return;
        }

        Optional<DatosAnotacion> resultado = mostrarDialogoAnotacion();

        if (resultado.isEmpty()) {
            return;
        }

        DatosAnotacion datos = resultado.get();

        tipoPendiente = datos.tipo;
        textoPendiente = datos.texto;
        colorPendiente = datos.color;
        primerPuntoPendiente = null;

        if (tipoPendiente == AnnotationType.POINT || tipoPendiente == AnnotationType.TEXT) {
            mostrarAviso("Ahora haz clic derecho sobre el mapa donde quieras poner la anotación.");
        } else {
            mostrarAviso("Ahora haz clic derecho sobre el mapa para marcar el primer punto.");
        }
    }

    private Optional<DatosAnotacion> mostrarDialogoAnotacion() {

        Dialog<DatosAnotacion> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotación");
        dialog.setHeaderText("Introduce los datos de la anotación");

        ComboBox<AnnotationType> comboTipo = new ComboBox<>();
        comboTipo.getItems().addAll(
                AnnotationType.POINT,
                AnnotationType.TEXT,
                AnnotationType.LINE,
                AnnotationType.CIRCLE
        );
        comboTipo.setValue(AnnotationType.POINT);

        TextField campoTexto = new TextField();
        campoTexto.setPromptText("Texto de la anotación");

        ColorPicker colorPicker = new ColorPicker(Color.RED);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Tipo:"), 0, 0);
        grid.add(comboTipo, 1, 0);

        grid.add(new Label("Texto:"), 0, 1);
        grid.add(campoTexto, 1, 1);

        grid.add(new Label("Color:"), 0, 2);
        grid.add(colorPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node botonOK = dialog.getDialogPane().lookupButton(ButtonType.OK);
        botonOK.setDisable(false);

        dialog.setResultConverter(boton -> {
            if (boton == ButtonType.OK) {
                String texto = campoTexto.getText();

                if (texto == null || texto.trim().isEmpty()) {
                    texto = "Anotación";
                }

                String colorHex = convertirColorAHex(colorPicker.getValue());

                return new DatosAnotacion(
                        comboTipo.getValue(),
                        texto,
                        colorHex
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void procesarClickMapa(double x, double y) {

        if (tipoPendiente == null || projection == null || actividadActual == null) {
            return;
        }

        GeoPoint punto = projection.unproject(x, y);

        if (tipoPendiente == AnnotationType.POINT || tipoPendiente == AnnotationType.TEXT) {
            guardarAnotacion(Arrays.asList(punto));
            limpiarAnotacionPendiente();
            return;
        }

        if (tipoPendiente == AnnotationType.LINE || tipoPendiente == AnnotationType.CIRCLE) {

            if (primerPuntoPendiente == null) {
                primerPuntoPendiente = punto;
                mostrarAviso("Primer punto marcado. Ahora haz clic derecho para marcar el segundo punto.");
            } else {
                guardarAnotacion(Arrays.asList(primerPuntoPendiente, punto));
                limpiarAnotacionPendiente();
            }
        }
    }

    private void guardarAnotacion(List<GeoPoint> puntos) {

        Annotation ann = new Annotation(
                tipoPendiente,
                textoPendiente,
                colorPendiente,
                2.0,
                puntos
        );

        Annotation guardada = app.addAnnotation(actividadActual, ann);

        if (guardada != null) {
            cargarListaAnotaciones();
            dibujarMapaRutaYAnotaciones();
            mostrarAviso("Anotación guardada correctamente.");
        } else {
            mostrarAviso("No se pudo guardar la anotación.");
        }
    }

    private void cargarListaAnotaciones() {

        listaAnotaciones.getItems().clear();

        if (actividadActual == null) {
            lblSinAnotaciones.setVisible(true);
            return;
        }

        for (Annotation ann : actividadActual.getAnnotations()) {
            String texto = ann.getText();

            if (texto == null || texto.isEmpty()) {
                texto = "(Sin texto)";
            }

            listaAnotaciones.getItems().add(ann.getType() + " - " + texto);
        }

        lblSinAnotaciones.setVisible(listaAnotaciones.getItems().isEmpty());
    }

    private void limpiarNodosDinamicosMapa() {
        mapPane.getChildren().removeIf(node
                -> node instanceof ImageView
                || node instanceof Polyline
                || node instanceof Line
                || node instanceof Circle
                || node instanceof Text);
    }

    private void dibujarMapaRutaYAnotaciones() {

        limpiarNodosDinamicosMapa();

        if (actividadActual == null) {
            return;
        }

        MapRegion region = actividadActual.getSuggestedMap();

        if (region == null) {
            mostrarTextoEnMapa("No hay mapa disponible para esta actividad");
            return;
        }

        File mapaFile = new File(region.getImagePath());

        if (!mapaFile.exists()) {
            mostrarTextoEnMapa("No se encuentra el mapa: " + region.getImagePath());
            return;
        }

        double ancho = obtenerAnchoMapa();
        double alto = obtenerAltoMapa();

        Image mapa = new Image(mapaFile.toURI().toString());

        ImageView fondo = new ImageView(mapa);
        fondo.setFitWidth(ancho);
        fondo.setFitHeight(alto);
        fondo.setPreserveRatio(false);

        mapPane.getChildren().add(fondo);

        projection = new MapProjection(region, ancho, alto);

        dibujarRuta();
        dibujarInicioFin();
        dibujarAnotaciones();
    }

    private void dibujarRuta() {

        Polyline ruta = new Polyline();
        ruta.setStroke(Color.BLUE);
        ruta.setStrokeWidth(3);

        for (TrackPoint tp : actividadActual.getTrackPoints()) {
            Point2D p = projection.project(tp);
            ruta.getPoints().addAll(p.getX(), p.getY());
        }

        mapPane.getChildren().add(ruta);
    }

    private void dibujarInicioFin() {

        TrackPoint inicio = actividadActual.getStartPoint();
        TrackPoint fin = actividadActual.getEndPoint();

        Point2D pInicio = projection.project(inicio);
        Point2D pFin = projection.project(fin);

        Circle cInicio = new Circle(pInicio.getX(), pInicio.getY(), 6);
        cInicio.setFill(Color.GREEN);

        Circle cFin = new Circle(pFin.getX(), pFin.getY(), 6);
        cFin.setFill(Color.RED);

        mapPane.getChildren().add(cInicio);
        mapPane.getChildren().add(cFin);
    }

    private void dibujarAnotaciones() {

        for (Annotation ann : actividadActual.getAnnotations()) {

            List<GeoPoint> puntos = ann.getGeoPoints();

            if (puntos == null || puntos.isEmpty()) {
                continue;
            }

            Color color = Color.web(ann.getColor());

            if (ann.getType() == AnnotationType.POINT) {
                dibujarAnotacionPunto(ann, puntos, color);
            } else if (ann.getType() == AnnotationType.TEXT) {
                dibujarAnotacionTexto(ann, puntos, color);
            } else if (ann.getType() == AnnotationType.LINE && puntos.size() >= 2) {
                dibujarAnotacionLinea(ann, puntos, color);
            } else if (ann.getType() == AnnotationType.CIRCLE && puntos.size() >= 2) {
                dibujarAnotacionCirculo(ann, puntos, color);
            }
        }
    }

    private void dibujarAnotacionPunto(Annotation ann, List<GeoPoint> puntos, Color color) {

        Point2D p = projection.project(puntos.get(0));

        Circle c = new Circle(p.getX(), p.getY(), 7);
        c.setFill(color);

        Text texto = new Text(p.getX() + 10, p.getY() - 10, ann.getText());
        texto.setFill(color);
        texto.setStyle("-fx-font-weight: bold;");

        mapPane.getChildren().addAll(c, texto);
    }

    private void dibujarAnotacionTexto(Annotation ann, List<GeoPoint> puntos, Color color) {

        Point2D p = projection.project(puntos.get(0));

        Text texto = new Text(p.getX(), p.getY(), ann.getText());
        texto.setFill(color);
        texto.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        mapPane.getChildren().add(texto);
    }

    private void dibujarAnotacionLinea(Annotation ann, List<GeoPoint> puntos, Color color) {

        Point2D p1 = projection.project(puntos.get(0));
        Point2D p2 = projection.project(puntos.get(1));

        Line linea = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        linea.setStroke(color);
        linea.setStrokeWidth(ann.getStrokeWidth());

        mapPane.getChildren().add(linea);
    }

    private void dibujarAnotacionCirculo(Annotation ann, List<GeoPoint> puntos, Color color) {

        Point2D centro = projection.project(puntos.get(0));
        Point2D borde = projection.project(puntos.get(1));

        double radio = centro.distance(borde);

        Circle circulo = new Circle(centro.getX(), centro.getY(), radio);
        circulo.setStroke(color);
        circulo.setStrokeWidth(ann.getStrokeWidth());
        circulo.setFill(Color.TRANSPARENT);

        mapPane.getChildren().add(circulo);
    }

    private void limpiarAnotacionPendiente() {
        tipoPendiente = null;
        textoPendiente = null;
        colorPendiente = null;
        primerPuntoPendiente = null;
    }

    private String convertirColorAHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);

        return String.format("#%02X%02X%02X", r, g, b);
    }

    private double obtenerAnchoMapa() {
        if (mapPane.getWidth() > 0) {
            return mapPane.getWidth();
        }

        if (mapPane.getPrefWidth() > 0) {
            return mapPane.getPrefWidth();
        }

        return 600;
    }

    private double obtenerAltoMapa() {
        if (mapPane.getHeight() > 0) {
            return mapPane.getHeight();
        }

        if (mapPane.getPrefHeight() > 0) {
            return mapPane.getPrefHeight();
        }

        return 400;
    }

    private void mostrarTextoEnMapa(String mensaje) {
        Label label = new Label(mensaje);
        label.setStyle("-fx-text-fill: white;");
        label.setLayoutX(40);
        label.setLayoutY(40);
        mapPane.getChildren().add(label);
    }

    private void mostrarAviso(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Anotaciones");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static class DatosAnotacion {

        AnnotationType tipo;
        String texto;
        String color;

        DatosAnotacion(AnnotationType tipo, String texto, String color) {
            this.tipo = tipo;
            this.texto = texto;
            this.color = color;
        }
    }
}