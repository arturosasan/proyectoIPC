package controllers;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Point2D;
import javafx.scene.Group;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
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

    private Group contenidoMapa;
    private double escala = 1.0;

    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 5.0;
    private static final double ZOOM_STEP = 0.1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        contenidoMapa = new Group();
        mapPane.getChildren().add(0, contenidoMapa);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapPane.widthProperty());
        clip.heightProperty().bind(mapPane.heightProperty());
        mapPane.setClip(clip);

        lblDistancia.setText("Distancia: -");
        lblDuracion.setText("Duración: -");
        lblVelocidad.setText("Velocidad: -");
        lblSinAnotaciones.setVisible(true);

        btnAnadirAnotacion.setOnAction(e -> prepararNuevaAnotacion());

        btnZoomMas.setOnAction(e -> {
            if (escala < ZOOM_MAX) {
                escala = Math.min(escala + ZOOM_STEP, ZOOM_MAX);
                contenidoMapa.setScaleX(escala);
                contenidoMapa.setScaleY(escala);
            }
        });

        btnZoomMenos.setOnAction(e -> {
            if (escala > ZOOM_MIN) {
                escala = Math.max(escala - ZOOM_STEP, ZOOM_MIN);
                contenidoMapa.setScaleX(escala);
                contenidoMapa.setScaleY(escala);
            }
        });

        mapPane.widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() != o.doubleValue() && n.doubleValue() > 0 && actividadActual != null) {
                dibujarMapaRutaYAnotaciones();
            }
        });
        mapPane.heightProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() != o.doubleValue() && n.doubleValue() > 0 && actividadActual != null) {
                dibujarMapaRutaYAnotaciones();
            }
        });

        mapPane.setFocusTraversable(true);
        mapPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && tipoPendiente != null) {
                limpiarAnotacionPendiente();
                mostrarAviso("Anotación cancelada.");
                mapPane.requestFocus();
            }
        });

        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY && tipoPendiente != null) {
                procesarClickMapa(e.getX() / escala, e.getY() / escala);
                mapPane.requestFocus();
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

        if (mapPane.getWidth() > 0 && mapPane.getHeight() > 0) {
            dibujarMapaRutaYAnotaciones();
        } else {
            Platform.runLater(() -> dibujarMapaRutaYAnotaciones());
        }

        cargarListaAnotaciones();
        mapPane.requestFocus();

        System.out.println("Actividad cargada correctamente");
    }

    private void prepararNuevaAnotacion() {
        if (tipoPendiente != null) {
            limpiarAnotacionPendiente();
        }

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

        mapPane.requestFocus();
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
                return new DatosAnotacion(comboTipo.getValue(), texto, colorHex);
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
                mapPane.requestFocus();
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
            mostrarError("No se pudo guardar la anotación.");
        }

        mapPane.requestFocus();
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

    private void dibujarMapaRutaYAnotaciones() {
        contenidoMapa.getChildren().clear();

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

        double ancho = mapPane.getWidth() > 0 ? mapPane.getWidth() : mapPane.getPrefWidth();
        double alto = mapPane.getHeight() > 0 ? mapPane.getHeight() : mapPane.getPrefHeight();

        Image mapa = new Image(mapaFile.toURI().toString());

        ImageView fondo = new ImageView(mapa);
        fondo.setFitWidth(ancho);
        fondo.setFitHeight(alto);
        fondo.setPreserveRatio(false);

        contenidoMapa.getChildren().add(fondo);

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

        contenidoMapa.getChildren().add(ruta);
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

        contenidoMapa.getChildren().addAll(cInicio, cFin);
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

        contenidoMapa.getChildren().addAll(c, texto);
    }

    private void dibujarAnotacionTexto(Annotation ann, List<GeoPoint> puntos, Color color) {
        Point2D p = projection.project(puntos.get(0));

        Text texto = new Text(p.getX(), p.getY(), ann.getText());
        texto.setFill(color);
        texto.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        contenidoMapa.getChildren().add(texto);
    }

    private void dibujarAnotacionLinea(Annotation ann, List<GeoPoint> puntos, Color color) {
        Point2D p1 = projection.project(puntos.get(0));
        Point2D p2 = projection.project(puntos.get(1));

        Line linea = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        linea.setStroke(color);
        linea.setStrokeWidth(ann.getStrokeWidth());

        contenidoMapa.getChildren().add(linea);
    }

    private void dibujarAnotacionCirculo(Annotation ann, List<GeoPoint> puntos, Color color) {
        Point2D centro = projection.project(puntos.get(0));
        Point2D borde = projection.project(puntos.get(1));

        double radio = centro.distance(borde);

        Circle circulo = new Circle(centro.getX(), centro.getY(), radio);
        circulo.setStroke(color);
        circulo.setStrokeWidth(ann.getStrokeWidth());
        circulo.setFill(Color.TRANSPARENT);

        contenidoMapa.getChildren().add(circulo);
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

    private void mostrarTextoEnMapa(String mensaje) {
        Label label = new Label(mensaje);
        label.setStyle("-fx-text-fill: white;");
        label.setLayoutX(40);
        label.setLayoutY(40);
        contenidoMapa.getChildren().add(label);
    }

    private void mostrarAviso(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Anotaciones");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
        mapPane.requestFocus();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Anotaciones");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
        mapPane.requestFocus();
    }

    @FXML
    private void handleVolver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) btnAnadirAnotacion).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pantalla principal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al volver a la pantalla principal: " + e.getMessage());
        }
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
