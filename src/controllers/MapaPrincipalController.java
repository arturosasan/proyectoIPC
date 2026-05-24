package controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import controllers.ModificarPerfilController;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
import upv.ipc.sportlib.User;

public class MapaPrincipalController implements Initializable {

    private final SportActivityApp app = SportActivityApp.getInstance();

    private Group zoomGroup;
    private Pane mapPane;

    private Activity actividadActual;
    private MapProjection projection;
    private boolean activityMode = false;
    private boolean resetZoom = true;
    private double escala = 1.0;

    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;
    private static final double ZOOM_STEP = 0.15;

    
    private AnnotationType tipoPendiente;
    private String textoPendiente;
    private String colorPendiente;
    private GeoPoint primerPuntoPendiente;

    @FXML private TabPane tabPane;
    @FXML private StackPane emptyStatePane;
    @FXML private VBox activityContent;
    @FXML private Label distanciaLabel;
    @FXML private Label duracionLabel;
    @FXML private Label velocidadLabel;
    @FXML private Label ritmoLabel;
    @FXML private Label desnivelPositivoLabel;
    @FXML private Label desnivelNegativoLabel;
    @FXML private Label altitudMinimaLabel;
    @FXML private Label altitudMaximaLabel;
    @FXML private Label lblSinAnotaciones;
    @FXML private ListView<String> listaAnotaciones;
    @FXML private Button btnEliminarAnotacion;
    @FXML private ScrollPane map_scrollpane;
    @FXML private Slider zoom_slider;
    @FXML private Label mousePosition;
    @FXML private Text nicknameText;
    @FXML private ImageView avatarView;
    @FXML
    private Label globalStatus;
    private Timeline statusTimeline;

    // =========================================================
    //  PUBLIC SETTERS
    // =========================================================

    /**
     * Establece el nombre de usuario en la etiqueta del encabezado.
     *
     * @param nickname nickname del usuario actual
     */
    public void setNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) return;
        nicknameText.setText("");
        nicknameText.setText(nickname);
    }

    /**
     * Carga y muestra la imagen de avatar del usuario en la vista.
     *
     * @param avatarPath ruta del archivo de imagen del avatar
     */
    public void viewAvatar(String avatarPath) {
        if (avatarPath != null && !avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                Image avatarImage = new Image(avatarFile.toURI().toString(), 126, 126, true, true);
                avatarView.setImage(avatarImage);
            }
        }
    }

    // =========================================================
    //  INITIALIZE
    // =========================================================

    /**
     * Inicializa el controlador principal.
     * Configura el slider de zoom, carga el mapa por defecto,
     * inicializa el estado vacío y prepara los eventos de
     * teclado y selección de anotaciones.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoom_slider.setMin(1);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);

        zoom_slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (activityMode) {
                escala = newVal.doubleValue();
                aplicarZoom();
            } else {
                zoom(newVal.doubleValue());
            }
        });

        buildMap(new File("maps/upv.jpg"));

        User user = app.getCurrentUser();
        if (user != null) {
            viewAvatar(user.getAvatarPath());
            setNickname(user.getNickName());
            setGlobalStatus("Sesión iniciada: " + user.getNickName());
        }

        inicioVacio();

        map_scrollpane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && tipoPendiente != null) {
                limpiarAnotacionPendiente();
                mostrarAviso("Anotación cancelada.");
                setGlobalStatus("Anotación cancelada");
            }
        });

        btnEliminarAnotacion.setDisable(true);
        listaAnotaciones.getSelectionModel().selectedIndexProperty().addListener(
            (obs, old, idx) -> btnEliminarAnotacion.setDisable(idx.intValue() < 0)
        );

        listaAnotaciones.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            if (idx.intValue() < 0 || projection == null || actividadActual == null) return;
            List<Annotation> anns = actividadActual.getAnnotations();
            if (idx.intValue() >= anns.size()) return;
            List<GeoPoint> pts = anns.get(idx.intValue()).getGeoPoints();
            if (pts == null || pts.isEmpty()) return;
            Point2D p = projection.project(pts.get(0));
            zoomToPosition(p.getX(), p.getY());
        });

        Platform.runLater(() -> {
            Stage stage = (Stage) tabPane.getScene().getWindow();
            stage.setOnCloseRequest(this::salirAplicación);
        });
    }

    // =========================================================
    //  ZOOM (normal mode)
    // =========================================================

    /**
     * Aumenta el zoom del mapa en la cantidad definida.
     * Se adapta al modo normal o modo actividad.
     *
     * @param event evento de acción del botón de zoom+
     */
    @FXML
    void zoomIn(ActionEvent event) {
        if (activityMode) {
            escala = Math.min(escala + ZOOM_STEP, ZOOM_MAX);
            zoom_slider.setValue(escala);
        } else {
            zoom_slider.setValue(zoom_slider.getValue() + 0.1);
        }
    }

    /**
     * Reduce el zoom del mapa en la cantidad definida.
     * Se adapta al modo normal o modo actividad.
     *
     * @param event evento de acción del botón de zoom-
     */
    @FXML
    void zoomOut(ActionEvent event) {
        if (activityMode) {
            escala = Math.max(escala - ZOOM_STEP, ZOOM_MIN);
            zoom_slider.setValue(escala);
        } else {
            zoom_slider.setValue(zoom_slider.getValue() - 0.1);
        }
    }

    /**
     * Aplica un factor de escala al grupo de zoom en modo normal.
     * Guarda y restaura la posición de scroll para evitar saltos.
     *
     * @param scaleValue nuevo factor de escala
     */
    private void zoom(double scaleValue) {
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    /**
     * Aplica el factor de escala actual al grupo de zoom en modo actividad.
     * Guarda y restaura la posición de scroll para evitar saltos.
     */
    private void aplicarZoom() {
        double h = map_scrollpane.getHvalue();
        double v = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(escala);
        zoomGroup.setScaleY(escala);
        map_scrollpane.setHvalue(h);
        map_scrollpane.setVvalue(v);
    }

    /**
     * Maneja el scroll del ratón sobre el mapa para hacer zoom.
     * El zoom se centra en la posición del ratón usando escalado relativo.
     * Se adapta al modo normal o modo actividad.
     *
     * @param event evento de scroll del ratón
     */
    private void onMapScroll(ScrollEvent event) {
        event.consume();
        double oldScale = zoomGroup.getScaleX();
        double newScale = oldScale * Math.exp(event.getDeltaY() * 0.01);
        double min = activityMode ? ZOOM_MIN : 1;
        double max = activityMode ? ZOOM_MAX : 1.5;
        newScale = Math.max(min, Math.min(max, newScale));
        if (Math.abs(newScale - oldScale) < 0.001) return;

        double mx = event.getX(), my = event.getY();
        double vw = map_scrollpane.getViewportBounds().getWidth();
        double vh = map_scrollpane.getViewportBounds().getHeight();
        double cw = mapPane.getWidth() * oldScale;
        double ch = mapPane.getHeight() * oldScale;
        double sx = (cw - vw) > 0 ? map_scrollpane.getHvalue() * (cw - vw) : 0;
        double sy = (ch - vh) > 0 ? map_scrollpane.getVvalue() * (ch - vh) : 0;

        double mmx = sx + mx, mmy = sy + my;

        zoomGroup.setScaleX(newScale);
        zoomGroup.setScaleY(newScale);
        map_scrollpane.layout();

        double ratio = newScale / oldScale;
        double ncw = mapPane.getWidth() * newScale;
        double nch = mapPane.getHeight() * newScale;
        double nsx = mmx * ratio - mx;
        double nsy = mmy * ratio - my;

        if (ncw - vw > 0)
            map_scrollpane.setHvalue(Math.max(0, Math.min(1, nsx / (ncw - vw))));
        if (nch - vh > 0)
            map_scrollpane.setVvalue(Math.max(0, Math.min(1, nsy / (nch - vh))));

        if (activityMode) {
            escala = newScale;
            zoom_slider.setValue(newScale);
        } else {
            zoom_slider.setValue(newScale);
        }
    }

    // =========================================================
    //  MOUSE POSITION
    // =========================================================

    /**
     * Actualiza la etiqueta de posición con las coordenadas del ratón.
     * Muestra coordenadas de escena y locales del nodo.
     *
     * @param event evento de movimiento del ratón
     */
    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText(
            "sceneX: " + (int) event.getSceneX() +
            ", sceneY: " + (int) event.getSceneY() + "\n" +
            "         X: " + (int) event.getX() +
            ",          Y: " + (int) event.getY()
        );
    }

    // =========================================================
    //  BUILD DEFAULT MAP
    // =========================================================

    /**
     * Carga una imagen y construye la jerarquía de nodos del mapa por defecto.
     * Configura el ScrollPane con el Group escalable para zoom.
     *
     * @param imgFile fichero de imagen a cargar como fondo del mapa
     */
    private void buildMap(File imgFile) {
        if (!imgFile.exists()) {
            map_scrollpane.setContent(new Label("Imagen no encontrada: " + imgFile.getPath()));
            return;
        }

        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        mapPane = new Pane();
        mapPane.setPrefSize(W, H);
        mapPane.setMinSize(W, H);
        mapPane.setMaxSize(W, H);

        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        mapPane.setOnScroll(this::onMapScroll);

        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        double zoom = zoom_slider.getValue();
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);
        map_scrollpane.setContent(contentGroup);
    }

    // =========================================================
    //  BUILD ACTIVITY MAP
    // =========================================================

    /**
     * Construye el mapa de una actividad con su ruta y anotaciones.
     * Proyecta los puntos geográficos sobre la imagen del mapa.
     * Ajusta el zoom inicial para que la ruta se vea completa.
     */
    private void buildActivityMap() {
        if (actividadActual == null) return;

        MapRegion region = actividadActual.getSuggestedMap();
        if (region == null) {
            mostrarTextoEnMapa("No hay mapa disponible para esta actividad");
            setGlobalStatus("No hay mapa disponible para esta actividad");
            return;
        }

        File mapaFile = new File(region.getImagePath());
        if (!mapaFile.exists()) {
            mostrarTextoEnMapa("No se encuentra el mapa: " + region.getImagePath());
            setGlobalStatus("No se encuentra el mapa: " + region.getImagePath());
            return;
        }

        Image img = new Image(mapaFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        Pane activityMapPane = new Pane();
        activityMapPane.setPrefSize(W, H);
        activityMapPane.setMinSize(W, H);
        activityMapPane.setMaxSize(W, H);

        Rectangle clip = new Rectangle(W, H);
        activityMapPane.setClip(clip);

        ImageView iv = new ImageView(img);
        activityMapPane.getChildren().add(iv);

        projection = new MapProjection(region, W, H);

        activityMapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (tipoPendiente != null && primerPuntoPendiente != null) {
                    GeoPoint punto = projection.unproject(e.getX(), e.getY());
                    guardarAnotacion(Arrays.asList(primerPuntoPendiente, punto), e.getX(), e.getY());
                    limpiarAnotacionPendiente();
                } else {
                    onActivityMapRightClick(e.getX(), e.getY());
                }
            }
        });
        activityMapPane.setOnScroll(this::onMapScroll);

        dibujarRuta(activityMapPane);
        dibujarInicioFin(activityMapPane);
        dibujarAnotaciones(activityMapPane);

        zoomGroup.getChildren().setAll(activityMapPane);
        mapPane = activityMapPane;

        if (resetZoom) { // CÓDIGO GENERADO CON IA PARA DEBUGGEAR ZOOM MAPA
            double displayW = map_scrollpane.getViewportBounds().getWidth() > 0
                ? map_scrollpane.getViewportBounds().getWidth() : 700;
            double displayH = map_scrollpane.getViewportBounds().getHeight() > 0
                ? map_scrollpane.getViewportBounds().getHeight() : 500;
            escala = Math.min(displayW / W, displayH / H);
            escala = Math.max(ZOOM_MIN, Math.min(escala, ZOOM_MAX));
            zoom_slider.setValue(escala);
            resetZoom = false;
        } else {
            aplicarZoom();
        }
    }

    // =========================================================
    //  DRAWING HELPERS (reestructurados de las otras pantallas)
    // =========================================================

    /**
     * Dibuja la polilínea de la ruta de la actividad sobre el pane indicado.
     *
     * @param pane panel sobre el que dibujar la ruta
     */
    private void dibujarRuta(Pane pane) {
        Polyline ruta = new Polyline();
        ruta.setStroke(Color.web("#55d6b3"));
        ruta.setStrokeWidth(3);
        for (TrackPoint tp : actividadActual.getTrackPoints()) {
            Point2D p = projection.project(tp);
            ruta.getPoints().addAll(p.getX(), p.getY());
        }
        pane.getChildren().add(ruta);
    }

    /**
     * Dibuja marcadores circulares para inicio (verde) y fin (rojo) de la ruta.
     *
     * @param pane panel sobre el que dibujar los marcadores
     */
    private void dibujarInicioFin(Pane pane) {
        Point2D inicio = projection.project(actividadActual.getStartPoint());
        Point2D fin = projection.project(actividadActual.getEndPoint());

        Circle cInicio = new Circle(inicio.getX(), inicio.getY(), 6);
        cInicio.setFill(Color.LIMEGREEN);
        cInicio.setStroke(Color.WHITE);
        cInicio.setStrokeWidth(2);

        Circle cFin = new Circle(fin.getX(), fin.getY(), 6);
        cFin.setFill(Color.RED);
        cFin.setStroke(Color.WHITE);
        cFin.setStrokeWidth(2);

        pane.getChildren().addAll(cInicio, cFin);
    }

    /**
     * Dibuja todas las anotaciones de la actividad sobre el pane indicado.
     * Soporta tipos POINT, TEXT, LINE y CIRCLE.
     *
     * @param pane panel sobre el que dibujar las anotaciones
     */
    private void dibujarAnotaciones(Pane pane) {
        if (actividadActual == null) return;
        for (Annotation ann : actividadActual.getAnnotations()) {
            List<GeoPoint> puntos = ann.getGeoPoints();
            if (puntos == null || puntos.isEmpty()) continue;

            Color color = Color.web(ann.getColor());

            if (ann.getType() == AnnotationType.POINT) {
                Point2D p = projection.project(puntos.get(0));
                Circle c = new Circle(p.getX(), p.getY(), 7);
                c.setFill(color);
                Text texto = new Text(p.getX() + 10, p.getY() - 10, textoAnotacion(ann));
                texto.setFill(color);
                texto.setStyle("-fx-font-weight: bold;");
                pane.getChildren().addAll(c, texto);
            } else if (ann.getType() == AnnotationType.TEXT) {
                Point2D p = projection.project(puntos.get(0));
                Circle invisible = new Circle(p.getX(), p.getY(), 5);
                invisible.setFill(Color.TRANSPARENT); // Color invisible para que el punto no se muestre :)
                invisible.setStroke(Color.TRANSPARENT);
                Text texto = new Text(p.getX(), p.getY(), textoAnotacion(ann));
                texto.setFill(color);
                texto.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                pane.getChildren().addAll(invisible, texto);
            } else if (ann.getType() == AnnotationType.LINE && puntos.size() >= 2) {
                Point2D p1 = projection.project(puntos.get(0));
                Point2D p2 = projection.project(puntos.get(1));
                Line linea = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                linea.setStroke(color);
                linea.setStrokeWidth(ann.getStrokeWidth());
                pane.getChildren().add(linea);
            } else if (ann.getType() == AnnotationType.CIRCLE && puntos.size() >= 2) {
                Point2D centro = projection.project(puntos.get(0));
                Point2D borde = projection.project(puntos.get(1));
                Circle circulo = new Circle(centro.getX(), centro.getY(), centro.distance(borde));
                circulo.setStroke(color);
                circulo.setStrokeWidth(ann.getStrokeWidth());
                circulo.setFill(Color.TRANSPARENT);
                pane.getChildren().add(circulo);
            }
        }
    }

    // =========================================================
    //  ACTIVITY SELECTION DIALOG ( sustituto de una pantalla entera ->  Caso de uso 4.3.- Visualizar Actividad)
    // =========================================================

    /**
     * Abre un diálogo para seleccionar una actividad y cargarla en la vista.
     * Si no hay actividades, muestra un aviso al usuario.
     */
    @FXML
    private void visualizarActividad() {
        List<Activity> actividades = app.getUserActivities();
        if (actividades.isEmpty()) {
            mostrarAviso("No hay actividades. Importa una con '+ Nueva Actividad'.");
            setGlobalStatus("No hay actividades. Importa una con '+ Nueva Actividad'.");
            return;
        }

        Dialog<Activity> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar actividad");
        dialog.setHeaderText("Elige la actividad que quieres visualizar:");

        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        ComboBox<Activity> combo = new ComboBox<>();
        combo.getItems().setAll(actividades);
        combo.setConverter(new StringConverter<Activity>() {
            @Override
            public String toString(Activity a) {
                return a == null ? "" : a.getName();
            }
            @Override
            public Activity fromString(String s) { return null; }
        });
        combo.setValue(actividades.get(0));
        combo.setPrefWidth(320);

        dialog.getDialogPane().setContent(combo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn.equals(ButtonType.OK) ? combo.getValue() : null);

        Optional<Activity> result = mostrarDialogo(dialog);
        result.ifPresent(this::cargarActividad);
    }

    // =========================================================
    //  LOAD ACTIVITY
    // =========================================================

    /**
     * Carga una actividad en la vista principal.
     * Muestra estadísticas, anotaciones y cambia al modo actividad.
     * Es invocable desde otros controladores (ej. AcumuladoActividadesController)
     * para cargar directamente una actividad sin pasar por el diálogo de selección.
     *
     * @param actividad actividad a cargar y visualizar
     */
    public void cargarActividad(Activity actividad) { // public xq así puedo acceder desde la pantalla "Acumulado Actividades"
        this.actividadActual = actividad;
        activityMode = true;
        resetZoom = true;

        buildActivityMap();

        distanciaLabel.setText(String.format("%.2f km", actividad.getTotalDistance() / 1000.0));
        duracionLabel.setText(formatearDuracion(actividad.getDuration()));
        velocidadLabel.setText(String.format("%.2f km/h", actividad.getAverageSpeed()));
        ritmoLabel.setText(String.format("%.2f min/km", actividad.getAveragePace()));
        desnivelPositivoLabel.setText(String.format("%.0f m", actividad.getElevationGain()));
        desnivelNegativoLabel.setText(String.format("%.0f m", actividad.getElevationLoss()));
        altitudMinimaLabel.setText(String.format("%.0f m", actividad.getMinElevation()));
        altitudMaximaLabel.setText(String.format("%.0f m", actividad.getMaxElevation()));

        cargarListaAnotaciones();

        emptyStatePane.setVisible(false);
        emptyStatePane.setManaged(false);
        activityContent.setVisible(true);
        activityContent.setManaged(true);

        tabPane.getSelectionModel().select(1);

        zoom_slider.setMin(ZOOM_MIN);
        zoom_slider.setMax(ZOOM_MAX);

        Platform.runLater(() -> map_scrollpane.requestFocus());

        setGlobalStatus("Actividad cargada: " + actividad.getName());
    }

    /**
     * Restablece la vista al estado inicial vacío (sin actividad cargada).
     * Muestra el mapa por defecto y oculta los paneles de actividad.
     */
    private void inicioVacio() {
        activityMode = false;
        actividadActual = null;
        projection = null;
        tipoPendiente = null;
        primerPuntoPendiente = null;
        textoPendiente = null;
        colorPendiente = null;

        activityContent.setVisible(false);
        activityContent.setManaged(false);
        emptyStatePane.setVisible(true);
        emptyStatePane.setManaged(true);

        buildMap(new File("maps/upv.jpg"));

        zoom_slider.setMin(1);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);

        setGlobalStatus("Sin actividad cargada");
    }

    // =========================================================
    //  ANNOTATIONS
    // =========================================================

    /**
     * Elimina la anotación seleccionada de la lista tras confirmación.
     * Actualiza el mapa y la lista de anotaciones.
     */
    @FXML
    private void eliminarAnotacion() {
        int index = listaAnotaciones.getSelectionModel().getSelectedIndex();
        if (index < 0 || actividadActual == null) return;

        Annotation ann = actividadActual.getAnnotations().get(index);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar anotación");
        confirm.setHeaderText("¿Eliminar esta anotación?");
        confirm.setContentText(ann.getType() + " - " + textoAnotacion(ann));

        Stage dialogStage = (Stage) confirm.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));

        Optional<ButtonType> result = mostrarDialogo(confirm);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean ok = app.removeAnnotation(actividadActual, ann);
            if (ok) {
                cargarListaAnotaciones();
                resetZoom = false;
                buildActivityMap();
                mostrarAviso("Anotación eliminada.");
                setGlobalStatus("Anotación eliminada");
            } else {
                mostrarError("No se pudo eliminar la anotación.");
            }
        }
    }

    /**
     * Maneja el clic derecho sobre el mapa en modo actividad.
     * Muestra el diálogo de anotación y, para POINT/TEXT, guarda
     * directamente; para LINE/CIRCLE, espera un segundo clic.
     *
     * @param x coordenada X del clic en el mapa
     * @param y coordenada Y del clic en el mapa
     */
    private void onActivityMapRightClick(double x, double y) {
        if (actividadActual == null || projection == null) return;

        Optional<DatosAnotacion> resultado = mostrarDialogoAnotacion();
        if (resultado.isEmpty()) return;

        DatosAnotacion datos = resultado.get();
        GeoPoint punto = projection.unproject(x, y);

        if (datos.tipo == AnnotationType.POINT || datos.tipo == AnnotationType.TEXT) {
            Annotation ann = new Annotation(datos.tipo, datos.texto, datos.color, 2.0, Arrays.asList(punto));
            Annotation guardada = app.addAnnotation(actividadActual, ann);
            if (guardada != null) {
                cargarListaAnotaciones();
                resetZoom = false;
                buildActivityMap();
                zoomToPosition(x, y);
                mostrarAviso("Anotación guardada correctamente.");
                setGlobalStatus("Anotación guardada");
            } else {
                mostrarError("No se pudo guardar la anotación.");
            }
        } else {
            tipoPendiente = datos.tipo;
            textoPendiente = datos.texto;
            colorPendiente = datos.color;
            primerPuntoPendiente = punto;
            mostrarAviso("Primer punto marcado. Haz clic derecho para marcar el segundo punto.");
            setGlobalStatus("Primer punto marcado. Haz clic derecho para el segundo punto.");
        }
    }

    /**
     * Muestra un diálogo para configurar una nueva anotación.
     * Permite seleccionar tipo, texto y color.
     *
     * @return Optional con los datos de la anotación, o vacío si se cancela
     */
    private Optional<DatosAnotacion> mostrarDialogoAnotacion() {
        Dialog<DatosAnotacion> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotación");
        dialog.setHeaderText("Introduce los datos de la anotación");

        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        ComboBox<AnnotationType> comboTipo = new ComboBox<>();
        comboTipo.getItems().addAll(AnnotationType.POINT, AnnotationType.TEXT, AnnotationType.LINE, AnnotationType.CIRCLE);
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

        dialog.setResultConverter(boton -> {
            if (boton == ButtonType.OK) {
                String texto = campoTexto.getText();
                if (texto == null || texto.trim().isEmpty()) texto = comboTipo.getValue().toString();
                return new DatosAnotacion(comboTipo.getValue(), texto, convertirColorAHex(colorPicker.getValue()));
            }
            return null;
        });

        return mostrarDialogo(dialog);
    }

    /**
     * Guarda la anotación pendiente (LINE/CIRCLE) y centra el mapa en ella.
     *
     * @param puntos puntos geográficos que definen la anotación
     * @param zoomX  coordenada X para centrar el zoom
     * @param zoomY  coordenada Y para centrar el zoom
     */
    private void guardarAnotacion(List<GeoPoint> puntos, double zoomX, double zoomY) {
        if (tipoPendiente == AnnotationType.CIRCLE && puntos.size() >= 2 && projection != null) {
            Point2D centro = projection.project(puntos.get(0));
            Point2D borde = projection.project(puntos.get(1));
            double radius = centro.distance(borde);
            double W = mapPane.getWidth();
            double H = mapPane.getHeight();
            if (centro.getX() - radius < 0 || centro.getY() - radius < 0 ||
                centro.getX() + radius > W || centro.getY() + radius > H) {
                mostrarError("El círculo se sale del mapa. Elige puntos más centrados.");
                limpiarAnotacionPendiente();
                return;
            }
        }
        Annotation ann = new Annotation(tipoPendiente, textoPendiente, colorPendiente, 2.0, puntos);
        Annotation guardada = app.addAnnotation(actividadActual, ann);

        if (guardada != null) {
            cargarListaAnotaciones();
            resetZoom = false;
            buildActivityMap();
            zoomToPosition(zoomX, zoomY);
            mostrarAviso("Anotación guardada correctamente.");
            setGlobalStatus("Anotación guardada");
        } else {
            mostrarError("No se pudo guardar la anotación.");
        }
    }

    /**
     * Aplica zoom y centra el mapa en una posición específica con animación.
     *
     * @param x coordenada X del punto a centrar
     * @param y coordenada Y del punto a centrar
     */
    private void zoomToPosition(double x, double y) {
        escala = Math.min(escala + 0.3, ZOOM_MAX);
        zoom_slider.setValue(escala);

        double mapW = mapPane.getWidth() * escala;
        double mapH = mapPane.getHeight() * escala;
        double viewW = map_scrollpane.getViewportBounds().getWidth();
        double viewH = map_scrollpane.getViewportBounds().getHeight();

        if (mapW <= viewW || mapH <= viewH) return;

        double scrollH = Math.max(0, Math.min(1, (x * escala - viewW / 2) / (mapW - viewW)));
        double scrollV = Math.max(0, Math.min(1, (y * escala - viewH / 2) / (mapH - viewH)));
        
        // CÓDIGO GENERADO CON IA ( DESPLAZAMIENTO A LA POSICIÓN DE ANOTACIÓN CON FRAMES ) 
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(
            javafx.util.Duration.millis(500),
            new KeyValue(map_scrollpane.hvalueProperty(), scrollH),
            new KeyValue(map_scrollpane.vvalueProperty(), scrollV)
        ));
        timeline.play();
    }

    /**
     * Carga la lista de anotaciones de la actividad actual en el ListView.
     * Muestra el texto y las coordenadas del primer punto de cada anotación.
     */
    private void cargarListaAnotaciones() {
        listaAnotaciones.getItems().clear();
        if (actividadActual == null) {
            lblSinAnotaciones.setVisible(true);
            lblSinAnotaciones.setManaged(true);
            return;
        }
        for (Annotation ann : actividadActual.getAnnotations()) {
            String texto = textoAnotacion(ann);
            List<GeoPoint> pts = ann.getGeoPoints();
            String x = "", y = "";
            if (pts != null && !pts.isEmpty()) {
                x = String.format("%.6f", pts.get(0).getLongitude());
                y = String.format("%.6f", pts.get(0).getLatitude());
            }
            listaAnotaciones.getItems().add(texto + " - X: " + x + " - Y: " + y);
        }
        boolean empty = listaAnotaciones.getItems().isEmpty();
        lblSinAnotaciones.setVisible(empty);
        lblSinAnotaciones.setManaged(empty);
    }

    /**
     * Obtiene el texto de una anotación o su tipo si está vacío.
     *
     * @param ann anotación de la que obtener el texto
     * @return texto de la anotación o su tipo como cadena
     */
    private String textoAnotacion(Annotation ann) {
        String t = ann.getText();
        return (t == null || t.isEmpty()) ? ann.getType().toString() : t;
    }

    /**
     * Abre la ventana de perfil de desnivel para la actividad actual.
     */
    @FXML
    private void abrirPerfilDesnivel() {
        if (actividadActual == null) {
            mostrarAviso("Primero debes cargar una actividad.");
            setGlobalStatus("Primero debes cargar una actividad");
            return;
        }
        abrirVentana("/views/PerfilDesnivel.fxml", "Perfil de desnivel", actividadActual);
        setGlobalStatus("Ventana de Perfil de desnivel abierta");
    }

    /**
     * Abre la ventana de velocidad sobre trazado para la actividad actual.
     */
    @FXML
    private void abrirVelocidad() {
        if (actividadActual == null) {
            mostrarAviso("Primero debes cargar una actividad.");
            setGlobalStatus("Primero debes cargar una actividad");
            return;
        }
        abrirVentana("/views/VelocidadTrazado.fxml", "Velocidad sobre trazado", actividadActual);
        setGlobalStatus("Ventana de Velocidad sobre trazado abierta");
    }

    // =========================================================
    //  NAVIGATION
    // =========================================================

    /**
     * Muestra un diálogo informativo con datos de la asignatura.
     *
     * @param event evento de acción del menú
     */
    @FXML
    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2026");
        mostrarDialogo(mensaje);
    }

    /**
     * Cierra la sesión del usuario actual y vuelve al login.
     * Muestra un diálogo de confirmación antes de cerrar sesión.
     *
     * @param event evento de acción del botón/menú
     */
    @FXML
    private void cerrarSesion(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar sesión");
        alert.setHeaderText("Estás a punto de cerrar sesión");
        alert.setContentText("¿Seguro que quieres salir?");
        Optional<ButtonType> result = mostrarDialogo(alert);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            app.logout();
            //cargarPantalla("/views/Login.fxml", "Login"); // no funciona bien porque he cambiado el método
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) nicknameText.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Login");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Maneja el cierre de la aplicación desde la ventana principal.
     * Muestra confirmación y cierra sesión antes de salir.
     *
     * @param event evento de cierre de ventana
     */
    private void salirAplicación(WindowEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir de la aplicación");
        alert.setHeaderText("Estás a punto de salir de la aplicación");
        alert.setContentText("¿Seguro que quieres salir?");
        Optional<ButtonType> result = mostrarDialogo(alert);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            app.logout();
            Platform.exit();
        } else {
            event.consume();
        }
    }

    /**
     * Navega a la pantalla de modificación de perfil.
     */
    @FXML
    private void modificarPerfil() { cargarPantalla("/views/ModificarPerfil.fxml", "Modificar Perfil");}

    /**
     * Navega a la pantalla de historial de sesiones.
     */
    @FXML
    private void historialSesiones() { cargarPantalla("/views/HistorialSesiones.fxml", "Historial de Sesiones");}

    /**
     * Navega a la pantalla de registro de nueva actividad.
     */
    @FXML
    private void nuevaActividad() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RegistrarActividad.fxml"));
            Parent root = loader.load();
            RegistrarActividadController controller = loader.getController();
            controller.setParentController(this);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Registrar actividad");
            stage.show();
            setGlobalStatus("Abriendo: Registrar actividad");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Abre la ventana emergente de acumulado de actividades.
     * Carga el FXML manualmente para poder pasar la referencia
     * del controlador principal (this) al controlador del acumulado,
     * permitiendo la navegación directa al hacer doble clic en una actividad.
     */
    @FXML
    private void acumuladoActividades() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AcumuladoActividades.fxml"));
            Parent root = loader.load();
            AcumuladoActividadesController controller = loader.getController();
            controller.setParentController(this);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Acumulado actividades");
            stage.show();
            setGlobalStatus("Abriendo: Acumulado actividades");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navega a la pantalla para añadir un nuevo mapa personalizado.
     *
     * @param event evento de acción del menú
     */
    @FXML
    private void addMapa(ActionEvent event) { cargarPantalla("/views/AddMapa.fxml", "Añadir mapa");}

    /**
     * Abre un selector de ficheros para cambiar la imagen de fondo del mapa.
     * Reinicia el estado vacío y construye el nuevo mapa.
     *
     * @param event evento de acción del menú
     * @throws IOException si hay un problema al obtener la ruta canónica
     */
    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("maps"));
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mapas", "*.png", "*.jpg"));
        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (imgFile != null) {
            inicioVacio();
            buildMap(imgFile);
            setGlobalStatus("Mapa cambiado: " + imgFile.getName());
        }
    }

    /**
     * Abre una nueva ventana con la pantalla indicada por el fxml.
     *
     * @param fxml   ruta del archivo FXML a cargar
     * @param titulo título de la nueva ventana
     */
    private void cargarPantalla(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof ModificarPerfilController mpc) {
                mpc.setOnPerfilActualizadoCallback(() -> {
                    User user = app.getCurrentUser();
                    if (user != null) {
                        viewAvatar(user.getAvatarPath());
                        setNickname(user.getNickName());
                    }
                });
            }
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.show();
            setGlobalStatus("Abriendo: " + titulo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre una ventana secundaria pasando la actividad al controlador.
     * Soporta PerfilDesnivelController y VelocidadTrazadoController.
     *
     * @param fxml      ruta del archivo FXML a cargar
     * @param titulo    título de la ventana
     * @param actividad actividad a pasar al controlador destino
     */
    private void abrirVentana(String fxml, String titulo, Activity actividad) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof VelocidadTrazadoController velocidadTrazadoController) { // POO de los coj****
                velocidadTrazadoController.setActividad(actividad); 
            } else if (controller instanceof PerfilDesnivelController perfilDesnivelController) {
                perfilDesnivelController.setActividad(actividad); 
            }
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    //  HELPERS
    // =========================================================

    /**
     * Limpia el estado de anotación pendiente, reiniciando todos los campos.
     */
    private void limpiarAnotacionPendiente() {
        tipoPendiente = null;
        textoPendiente = null;
        colorPendiente = null;
        primerPuntoPendiente = null;
    }

    /**
     * Convierte un color JavaFX a su representación hexadecimal.
     *
     * @param color color a convertir
     * @return cadena en formato "#RRGGBB"
     */
    private String convertirColorAHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Formatea una duración a formato "HH:MM:SS".
     *
     * @param duration duración a formatear
     * @return cadena con el tiempo formateado
     */
    private String formatearDuracion(Duration duration) {
        long segundos = duration.getSeconds();
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long seg = segundos % 60;
        return String.format("%02d:%02d:%02d", horas, minutos, seg);
    }

    /**
     * Muestra un mensaje de texto directamente sobre el mapa.
     *
     * @param mensaje texto a mostrar
     */
    private void mostrarTextoEnMapa(String mensaje) {
        Label label = new Label(mensaje);
        label.setStyle("-fx-text-fill: white; "
                + "-fx-font-size: 14px;");
        label.setLayoutX(40);
        label.setLayoutY(40);
        if (mapPane != null) mapPane.getChildren().add(label);
    }

    private void setGlobalStatus(String mensaje) {
        if (statusTimeline != null) {
            statusTimeline.stop();
        }
        globalStatus.setText(mensaje);
        statusTimeline = new Timeline(
            new KeyFrame(javafx.util.Duration.seconds(5),
                evt -> globalStatus.setText("Listo"))
        );
        statusTimeline.setCycleCount(1);
        statusTimeline.play();
    }

    private <T> Optional<T> mostrarDialogo(Dialog<T> dialogo) {
        Optional<T> resultado = dialogo.showAndWait();
        reMaximizarVentana();
        return resultado;
    }

    private void reMaximizarVentana() { // MÉTODO GENERADO CON IA PARA BUG TAMAÑO PANTALLA (QUE SIGUE SIN SOLUCIONARSE)
        Stage stage = (Stage) tabPane.getScene().getWindow();
        if (stage != null) {
            stage.setMaximized(true);
        }
    }

    /**
     * Muestra un diálogo informativo con un mensaje para el usuario.
     *
     * @param mensaje texto informativo a mostrar
     */
    private void mostrarAviso(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        mostrarDialogo(alert);
    }

    /**
     * Muestra un diálogo de error con un mensaje para el usuario.
     *
     * @param mensaje texto del error a mostrar
     */
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        mostrarDialogo(alert);
    }

    // =========================================================
    //  INNER CLASS
    // =========================================================

    private static class DatosAnotacion {
        AnnotationType tipo;
        String texto;
        String color;
        /**
         * Constructor de DatosAnotacion.
         *
         * @param tipo  tipo de anotación (POINT, TEXT, LINE, CIRCLE)
         * @param texto texto descriptivo de la anotación
         * @param color color en formato hexadecimal
         */
        DatosAnotacion(AnnotationType tipo, String texto, String color) {
            this.tipo = tipo;
            this.texto = texto;
            this.color = color;
        }
    }
}
