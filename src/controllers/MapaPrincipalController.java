/*
 * ============================================================
 *  PROYECTO EJEMPLO – IPC 2026
 *  Asignatura: Interfaces Persona-Computador
 *  Universitat Politècnica de València
 * ============================================================
 *
 *  DESCRIPCIÓN GENERAL
 *  -------------------
 *  Este controlador gestiona la vista principal de la aplicación
 *  de puntos de interés (POI) sobre un mapa.
 *
 *  Funcionalidades implementadas:
 *   1. Carga y visualización de una imagen de mapa.
 *   2. Zoom interactivo mediante un Slider.
 *   3. Añadir POIs (texto) y anotaciones (círculos) con clic derecho.
 *   4. Listado de POIs en un ListView con CellFactory personalizada.
 *   5. Centrado animado del mapa al seleccionar un POI de la lista.
 *   6. Modo inserción: activar con botón y colocar POI con siguiente clic.
 *
 *  PATRÓN UTILIZADO: MVC (Model-View-Controller)
 *   - Modelo : clase Poi  (datos del punto de interés)
 *   - Vista  : MapaPrincipal.fxml  (layout declarativo)
 *   - Control: esta clase (lógica de interacción)
 *
 * ============================================================
 */
package controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * Controlador principal de la aplicación de mapa con POIs.
 *
 * La anotación @FXML conecta automáticamente los campos de esta clase
 * con los elementos declarados en el fichero FXML mediante su atributo fx:id.
 *
 * Implementa {@link Initializable} para poder ejecutar código de
 * inicialización una vez que el FXML ha sido cargado completamente.
 */
public class MapaPrincipalController implements Initializable {

    private SportActivityApp app = SportActivityApp.getInstance();
    

    // =========================================================
    //  ESTRUCTURA DE NODOS PARA ZOOM
    // =========================================================
    //
    //  El zoom se consigue escalando un Group (zoomGroup).
    //  Escalar un Group NO desplaza los nodos que contiene,
    //  lo que evita el "salto" visual al hacer zoom.
    //
    //  Jerarquía de nodos:
    //
    //  ScrollPane (map_scrollpane)
    //   └─ contentGroup          ← Group raíz dentro del ScrollPane
    //       └─ zoomGroup         ← se escala para el zoom
    //           └─ mapPane       ← Pane con la imagen y los POIs
    //               ├─ ImageView ← imagen del mapa
    //               ├─ Text      ← etiquetas de POIs
    //               └─ Circle    ← anotaciones circulares
    //
    // =========================================================

    /** Group que se escala para aplicar el zoom. */
    private Group zoomGroup;

    /**
     * Pane que actúa como lienzo del mapa.
     * Contiene la imagen de fondo y todos los elementos superpuestos
     * (textos, círculos, etc.). Sus dimensiones coinciden con las de
     * la imagen cargada.
     */
    private Pane mapPane;

    
    /** Menú contextual reutilizable para el clic derecho sobre el mapa. */
    private ContextMenu mapContextMenu;


    /**
     * Indica si el controlador está en modo inserción de POI.
     * {@code true} → el próximo clic izquierdo sobre el mapa abre el diálogo.
     */
    private boolean insertionMode = false;

    // =========================================================
    //  ELEMENTOS FXML  (inyectados automáticamente por el cargador)
    // =========================================================

    /** Lista lateral que muestra todos los POIs añadidos al mapa. */
    @FXML
    private ListView<Poi> map_listview;

    /** ScrollPane que envuelve el mapa y permite desplazarlo. */
    @FXML
    private ScrollPane map_scrollpane;

    /**
     * Slider de zoom.
     * Rango: [0.5 – 1.5]. Valor inicial: 1.0 (sin zoom).
     * Cada cambio de valor llama al método zoom().
     */
    @FXML
    private Slider zoom_slider;
    
    /**
     * Botón de pin visible sobre el mapa.
     * Se desplaza hasta la posición del POI seleccionado en la lista.
     */
    private MenuButton map_pin;

    // FIX 5 — Eliminadas las variables sin uso:
    //   · 'mousePosistion' (errata + duplicado de mousePosition)
    //   · 'pin_info'       (inyectada pero nunca actualizada)

    /** Etiqueta en la barra de estado que muestra las coordenadas del ratón. */
    @FXML
    private Label mousePosition;
    @FXML
    private Text nicknameText;

    @FXML
    private ImageView avatarView;
    

    public void setNickname(String nickname) {
        nicknameText.setText(""); // para solucinar bug de que a veces aparece el texto default ([nickname]) en lugar del usuario
        nicknameText.setText(nickname);
    }
    
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
    //  MANEJADORES DE ZOOM
    // =========================================================

    /**
     * Aumenta el zoom en 0.1 unidades al pulsar el botón "+".
     *
     * @param event evento de acción del botón
     */
    @FXML
    void zoomIn(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + 0.1);
    }

    /**
     * Reduce el zoom en 0.1 unidades al pulsar el botón "–".
     *
     * @param event evento de acción del botón
     */
    @FXML
    void zoomOut(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal - 0.1);
    }

    /**
     * Aplica el factor de escala al {@code zoomGroup}.
     *
     * Este método es invocado automáticamente cada vez que cambia el
     * valor del slider, gracias al listener registrado en {@link #initialize}.
     *
     * Truco: guardamos y restauramos los valores de scroll para que el
     * contenido visible no salte al cambiar la escala.
     *
     * @param scaleValue nuevo factor de escala (p. ej. 1.2 → 120 %)
     */
    private void zoom(double scaleValue) {
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }
    
    
    // STACKOVERFLOW NO HA MUERTO --> https://stackoverflow.com/questions/39827911/javafx-8-scaling-zooming-scrollpane-relative-to-mouse-position
    private void onMapScroll(ScrollEvent event) {
        event.consume();
        double oldScale = zoomGroup.getScaleX();
        double newScale = oldScale * Math.exp(event.getDeltaY() * 0.01); // DELTA ES PARA LA POS DEL RATÓN
        newScale = Math.max(zoom_slider.getMin(),
                   Math.min(zoom_slider.getMax(), newScale));
        if (Math.abs(newScale - oldScale) < 0.001) return; // PARA EVITAR TIRONES
        
        
        // ANTES DEL ZOOM
        
        double mx = event.getX(), my = event.getY(); // Ratón
        double vw = map_scrollpane.getViewportBounds().getWidth(), 
                vh = map_scrollpane.getViewportBounds().getHeight(); // ViewPorts mapa
        double cw = mapPane.getWidth() * oldScale, ch = mapPane.getHeight() * oldScale; // Altura / Anchura
        double sx = (cw - vw) > 0 ? map_scrollpane.getHvalue() * (cw - vw) : 0, // Scroll
                sy = (ch - vh) > 0 ? map_scrollpane.getVvalue() * (ch - vh) : 0;

        double mmx = sx + mx, mmy = sy + my; // MapMouse X / Y
        
        // APLICO CAMBIOS
        
        zoomGroup.setScaleX(newScale);
        zoomGroup.setScaleY(newScale);
        map_scrollpane.layout();
        
        // ACTUALIZO CAMBIOS VISUALES
        
        double ratio = newScale / oldScale;
        double ncw = mapPane.getWidth() * newScale;
        double nch = mapPane.getHeight() * newScale;
        double nsx = mmx * ratio - mx;
        double nsy = mmy * ratio - my;
        
        // PARA QUE SE HAGA ZOOM AL ÁREA DEL RATÓN (CÓDIGO GENERADO POR IA)
        if (ncw - vw > 0)
            map_scrollpane.setHvalue(Math.max(0, Math.min(1, nsx / (ncw - vw))));
        if (nch - vh > 0)
            map_scrollpane.setVvalue(Math.max(0, Math.min(1, nsy / (nch - vh))));
        
        //SLIDER ACTUALIZADO
        zoom_slider.setValue(newScale);
    }

    
    // =========================================================
    //  SELECCIÓN EN EL LISTVIEW → CENTRADO EN EL MAPA
    // =========================================================

    /**
     * Se ejecuta cuando el usuario hace clic en un elemento del ListView.
     *
     * Objetivo: centrar el ScrollPane sobre la posición del POI seleccionado
     * con una animación suave de 500 ms, y mover el pin al punto.
     *
     * Cálculo del scroll
     * ------------------
     * El ScrollPane expresa su posición como valores normalizados [0, 1]:
     *   · hValue = 0 → extremo izquierdo
     *   · hValue = 1 → extremo derecho
     *
     * Para centrar el POI necesitamos:
     *
     *   scrollH = (poiX_escalado - viewportAncho / 2)
     *             ─────────────────────────────────────
     *             (mapaAncho_escalado - viewportAncho)
     *
     * Aplicamos clamp para no salir del rango [0, 1].
     *
     * @param event evento de ratón sobre el ListView
     */
    @FXML
    void listClicked(MouseEvent event) {
        // Obtenemos el POI seleccionado; si no hay ninguno, salimos
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();
        if (itemSelected == null) return;

        // ── Dimensiones del mapa con el zoom actual aplicado ──────────
        double mapWidth  = mapPane.getWidth()  * zoomGroup.getScaleX();
        double mapHeight = mapPane.getHeight() * zoomGroup.getScaleY();

        // ── Posición del POI escalada ──────────────────────────────────
        // getPosition() devuelve las coordenadas en el sistema local del
        // mapPane (sin zoom). Las multiplicamos por el factor de escala
        // para obtener la posición real en pantalla.
        double poiX = itemSelected.getPosition().getX() * zoomGroup.getScaleX();
        double poiY = itemSelected.getPosition().getY() * zoomGroup.getScaleY();

        // ── Tamaño visible del ScrollPane (viewport) ───────────────────
        double viewW = map_scrollpane.getViewportBounds().getWidth();
        double viewH = map_scrollpane.getViewportBounds().getHeight();

        // ── Cálculo del scroll normalizado [0, 1] ─────────────────────
        // Restamos la mitad del viewport para que el POI quede centrado
        // y no en la esquina superior-izquierda del área visible.
        double scrollH = (poiX - viewW / 2) / (mapWidth  - viewW);
        double scrollV = (poiY - viewH / 2) / (mapHeight - viewH);

        // Garantizamos que el valor esté dentro del rango válido [0, 1]
        scrollH = Math.max(0, Math.min(1, scrollH));
        scrollV = Math.max(0, Math.min(1, scrollV));

        // ── Animación suave con Timeline ──────────────────────────────
        // Timeline interpola los valores de las propiedades a lo largo
        // del tiempo. KeyValue define qué propiedad animar y hasta qué
        // valor; KeyFrame define en qué instante se alcanza ese valor.
        final Timeline timeline = new Timeline();
        final KeyValue kv1 = new KeyValue(map_scrollpane.hvalueProperty(), scrollH);
        final KeyValue kv2 = new KeyValue(map_scrollpane.vvalueProperty(), scrollV);
        final KeyFrame kf  = new KeyFrame(Duration.millis(500), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.play(); // Inicia la animación (no bloquea el hilo de la UI)

    }

    // =========================================================
    //  CONSTRUCCIÓN DEL MAPA
    // =========================================================

    /**
     * Carga una imagen y construye la jerarquía de nodos del mapa.
     *
     * Este método puede llamarse varias veces (p. ej. al cambiar el mapa),
     * ya que sustituye completamente el contenido del ScrollPane.
     *
     * @param imgFile fichero de imagen a cargar como fondo del mapa
     */
    private void buildMap(File imgFile) {
        if (!imgFile.exists()) {
            map_scrollpane.setContent(
                new Label("Imagen no encontrada: " + imgFile.getPath()));
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

        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onMapRightClick(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.PRIMARY && insertionMode) {
                insertionMode = false;
                mapPane.setStyle("");
                addPoi(e.getX(), e.getY());
            }
        });

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
    //  MENÚ CONTEXTUAL (clic derecho sobre el mapa)
    // =========================================================

    /**
     * Muestra el menú contextual reutilizable en la posición del clic.
     *
     * Las acciones de los MenuItem se actualizan con las coordenadas
     * del clic actual antes de mostrar el menú.
     *
     * @param x coordenada X del clic en el sistema local del mapPane
     * @param y coordenada Y del clic en el sistema local del mapPane
     */
    private void onMapRightClick(double x, double y) {
        // FIX 6: cerramos el menú si ya estaba visible (evita instancias flotantes)
        mapContextMenu.hide();

        // Actualizamos las acciones de los items con las coordenadas actuales.
        // Usamos variables final para que el lambda pueda capturarlas.
        final double clickX = x;
        final double clickY = y;
        mapContextMenu.getItems().get(0).setOnAction(e -> addPoi(clickX, clickY));
        mapContextMenu.getItems().get(1).setOnAction(e -> addCircle(clickX, clickY));

        // Mostramos el menú en coordenadas de pantalla
        mapContextMenu.show(
            mapPane.getScene().getWindow(),
            mapPane.localToScreen(x, y).getX(),
            mapPane.localToScreen(x, y).getY()
        );
    }

    // =========================================================
    //  INICIALIZACIÓN DEL CONTROLADOR
    // =========================================================

    /**
     * Método llamado automáticamente por el FXMLLoader tras inyectar
     * todos los elementos {@code @FXML}.
     *
     * Aquí configuramos:
     *  - El slider de zoom y su listener.
     *  - El ContextMenu reutilizable (FIX 6).
     *  - La CellFactory del ListView (FIX 4).
     *  - La carga del mapa inicial.
     *
     * @param url  URL del documento FXML (no usado aquí)
     * @param rb   paquete de recursos de internacionalización (no usado aquí)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ── Configuración del slider de zoom ──────────────────────────
        zoom_slider.setMin(0.75);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);

        // Listener que invoca zoom() cada vez que el slider cambia de valor.
        // Usamos una expresión lambda en lugar de una clase anónima por brevedad.
        zoom_slider.valueProperty().addListener(
            (observable, oldVal, newVal) -> zoom((Double) newVal)
        );

        // Los items se crean aquí sin acción; las acciones se asignan
        // en onMapRightClick() con las coordenadas correctas de cada clic.
        MenuItem miText   = new MenuItem("📝 Añadir texto");
        MenuItem miCircle = new MenuItem("⭕ Añadir círculo");
        mapContextMenu = new ContextMenu(miText, miCircle);

               //  setCellFactory() define cómo se renderiza cada celda
        //  de forma independiente al modelo Poi.
        //  Aquí mostramos "CÓDIGO – Nombre" en cada fila.
        map_listview.setCellFactory(listView -> new ListCell<Poi>() {
            @Override
            protected void updateItem(Poi poi, boolean empty) {
                // Siempre llamar a super primero (requerido por JavaFX)
                super.updateItem(poi, empty);

                if (empty || poi == null) {
                    // Celda vacía: limpiamos texto y gráfico
                    setText(null);
                    setGraphic(null);
                } else {
                    // Mostramos código y nombre separados por un guión largo
                    setText(poi.getCode() + " – " + poi.getPosition());
                }
            }
        });

        // ── Carga del mapa inicial ─────────────────────────────────────
        // El fichero se busca relativo al directorio de trabajo del proyecto.
        buildMap(new File("maps/upv.jpg"));
        
        User user = app.getCurrentUser();
        if (user != null) {
            viewAvatar(user.getAvatarPath());
            setNickname(user.getNickName()); // POR ESTA LÍNEA ERA EL BUG DEL NICKNAME QUE NO SALÍA, PASABA AL CAMBIAR PANTALLAS
    }

    }

    // =========================================================
    //  INDICADOR DE POSICIÓN DEL RATÓN
    // =========================================================

    /**
     * Actualiza la etiqueta {@code mousePosition} con las coordenadas
     * actuales del ratón, tanto en el sistema de la escena como en el
     * sistema local del nodo sobre el que se mueve.
     *
     * Útil para depuración y para que los alumnos comprendan la diferencia
     * entre coordenadas de escena y coordenadas locales.
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
    //  DIÁLOGO "ACERCA DE"
    // =========================================================

    /**
     * Muestra un diálogo informativo con datos de la asignatura.
     *
     * Nota: accedemos al Stage del diálogo para poder personalizar
     * su icono, ya que Alert no expone directamente esa propiedad.
     *
     * @param event evento de acción del menú
     */
    @FXML
    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);
        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2026");
        mensaje.showAndWait(); // Bloquea hasta que el usuario cierra el diálogo
    }

    // =========================================================
    //  AÑADIR UN POI (texto) AL MAPA
    // =========================================================

    /**
     * Muestra un diálogo para introducir el nombre del nuevo POI,
     * lo añade al ListView y dibuja su etiqueta sobre el mapa.
     *
     * @param x coordenada X del clic en el sistema local del mapPane
     * @param y coordenada Y del clic en el sistema local del mapPane
     */
    private void addPoi(double x, double y) {

        // ── Construcción del diálogo personalizado ────────────────────
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce un nuevo POI");

        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        // Botones del diálogo: Aceptar y Cancelar
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Campo de texto para el nombre del POI
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre del POI");

        // Layout del contenido del diálogo (VBox con espaciado de 10 px)
        VBox vbox = new VBox(10, new Label("Nombre:"), nameField);
        poiDialog.getDialogPane().setContent(vbox);

        // ResultConverter: transforma la selección del botón en un objeto Poi.
        // FIX 1: ya no usamos coordenadas provisionales (0,0); pasamos (x,y)
        // directamente al constructor para que el modelo sea coherente desde el inicio.
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new Poi(nameField.getText().trim(), x, y);
            }
            return null;
        });

        // Mostramos el diálogo y esperamos la respuesta del usuario
        Optional<Poi> result = poiDialog.showAndWait();

        if (result.isPresent()) {
            Poi poi = result.get();

            // FIX 1: confirmamos la posición como Point2D para compatibilidad
            // con getPosition(), usando las mismas coordenadas (x, y).
            poi.setPosition(new Point2D(x, y));

            // Añadimos el POI al ListView (la CellFactory mostrará nombre y código)
            map_listview.getItems().add(poi);

            // FIX 1: usamos (x, y) tanto para el modelo como para el Text,
            // garantizando que la etiqueta aparezca exactamente donde se hizo clic.
            Text text = new Text(poi.getCode());
            text.setX(x);
            text.setY(y);
            mapPane.getChildren().add(text);
        }
    }

    // =========================================================
    //  CAMBIAR EL MAPA (selector de fichero)
    // =========================================================

    /**
     * Abre un selector de fichero para que el usuario elija una imagen
     * diferente como mapa y reconstruye toda la vista.
     *
     * FIX 3: se comprueba que imgFile no sea null antes de usarlo,
     * evitando NullPointerException cuando el usuario cierra el FileChooser
     * sin seleccionar ningún fichero.
     *
     * @param event evento de acción del menú
     * @throws IOException si hay un problema al obtener la ruta canónica
     */
    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("./maps ")); // Carpeta donde están los mapas en la raíz del proyecto
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Mapas", "*.png", "*.jpg"));
        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());

        // FIX 3: showOpenDialog() devuelve null si el usuario cancela la selección
        if (imgFile != null) {
            System.out.println("Mapa seleccionado: " + imgFile.getCanonicalPath());
            buildMap(imgFile); // Reconstruimos la vista con la nueva imagen
            map_listview.getItems().clear(); // Borramos los datos del mapa anterior
        }
    }

    // =========================================================
    //  AÑADIR UN CÍRCULO AL MAPA
    // =========================================================

    /**
     * Dibuja un círculo rojo de radio 10 px en la posición indicada.
     *
     * Ejemplo sencillo de cómo añadir formas vectoriales (Shape) sobre el mapa.
     * Los alumnos pueden extenderlo para:
     *  - Elegir color dinámicamente.
     *  - Asociar información al círculo (tooltip, popup, etc.).
     *  - Permitir moverlo con arrastrar y soltar (drag and drop).
     *
     * @param x coordenada X en el sistema local del mapPane
     * @param y coordenada Y en el sistema local del mapPane
     */
    private void addCircle(double x, double y) {
        Circle circle = new Circle(10, Color.RED); // radio = 10 px, color = rojo
        circle.setCenterX(x);
        circle.setCenterY(y);
        mapPane.getChildren().add(circle); // Se añade sobre el mapa como cualquier nodo
    }
    
    @FXML
    private void cerrarSesion(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar sesión");
        alert.setHeaderText("Estás a punto de cerrar sesión");
        alert.setContentText("¿Seguro que quieres salir?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) { // Si se muestra la ventana && el usuario le de a "Aceptar" / "Ok"
            app.logout();
            cargarPantalla("/views/Login.fxml");
        }
    }
    
    private void cargarPantalla(String fxmlDestino) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlDestino));
            Parent root = loader.load();
            
            
            Stage stage = (Stage) nicknameText.getScene().getWindow();
            stage.setScene(new Scene(root));
            if (fxmlDestino.equals("/views/Login.fxml")) {
                stage.setTitle("Login");
            } else {
                stage.setTitle("Pantalla principal");
            }
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML 
    private void modificarPerfil() {
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ModificarPerfil.fxml"));
        Parent root = loader.load();
        ModificarPerfilController controller = loader.getController();
        controller.setNickname(nicknameText.getText()); // o como tengas guardado el nick actual
        
        Stage stage = (Stage) nicknameText.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
    }
    }
    
    @FXML 
    private void historialSesiones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/HistorialSesiones.fxml"));
            Parent root = loader.load();
            HistorialSesionesController controller = loader.getController();
            controller.setNickname(nicknameText.getText());
            
            Stage stage = (Stage) nicknameText.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Historial de sesiones");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarPantalla(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) nicknameText.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void nuevaActividad() {
        cargarPantalla("/views/RegistrarActividad.fxml", "Registrar actividad");
    }

    @FXML
    private void visualizarActividad() {
        cargarPantalla("/views/VisualizarActividad.fxml", "Visualizar actividad");
    }

    @FXML
    private void anotacionesActividad() {
        cargarPantalla("/views/AnotacionesActividad.fxml", "Anotaciones");
    }

    @FXML
    private void acumuladoActividades() {
        cargarPantalla("/controllers/AcumuladoActividades.fxml", "Acumulado actividades");
    }
}
