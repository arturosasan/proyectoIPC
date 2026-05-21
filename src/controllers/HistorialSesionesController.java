package controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class HistorialSesionesController implements Initializable {

    @FXML
    private TableView<Sesion> tablaSesiones;

    @FXML
    private TableColumn<Sesion, String> inicioColumn;

    @FXML
    private TableColumn<Sesion, String> finColumn;

    @FXML
    private TableColumn<Sesion, String> duracionColumn;

    @FXML
    private TableColumn<Sesion, Integer> importadasColumn;

    @FXML
    private TableColumn<Sesion, Integer> vistasColumn;

    @FXML
    private TableColumn<Sesion, Integer> anotacionesColumn;

    @FXML
    private Label totalImportadasLabel;

    @FXML
    private Label totalVistasLabel;

    @FXML
    private Label totalAnotacionesLabel;
    
    private String currentUserNick;
    
    
    /**
     * Almacena el nickname del usuario actual.
     *
     * @param nick nickname del usuario
     */
    public void setNickname(String nick) {
        this.currentUserNick = nick;
    }

    /**
     * Inicializa el controlador de historial de sesiones.
     * Configura las columnas de la tabla con los datos de ejemplo
     * y calcula los totales mostrados al final.
     *
     * @param url  URL del documento FXML (no usado)
     * @param rb   paquete de recursos (no usado)
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        inicioColumn.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        finColumn.setCellValueFactory(new PropertyValueFactory<>("fin"));
        duracionColumn.setCellValueFactory(new PropertyValueFactory<>("duracion"));
        importadasColumn.setCellValueFactory(new PropertyValueFactory<>("importadas"));
        vistasColumn.setCellValueFactory(new PropertyValueFactory<>("vistas"));
        anotacionesColumn.setCellValueFactory(new PropertyValueFactory<>("anotaciones"));

        ObservableList<Sesion> sesiones = FXCollections.observableArrayList(
                new Sesion("10:00", "10:45", "45 min", 3, 5, 2),
                new Sesion("11:00", "12:15", "1 h 15 min", 4, 6, 1),
                new Sesion("16:30", "17:00", "30 min", 2, 3, 0)
        );

        tablaSesiones.setItems(sesiones);

        int totalImportadas = 0;
        int totalVistas = 0;
        int totalAnotaciones = 0;

        for (Sesion s : sesiones) {
            totalImportadas += s.getImportadas();
            totalVistas += s.getVistas();
            totalAnotaciones += s.getAnotaciones();
        }

        totalImportadasLabel.setText("Total importadas: " + totalImportadas);
        totalVistasLabel.setText("Total vistas: " + totalVistas);
        totalAnotacionesLabel.setText("Total anotaciones: " + totalAnotaciones);
    }

    public static class Sesion {

        private String inicio;
        private String fin;
        private String duracion;
        private int importadas;
        private int vistas;
        private int anotaciones;

        /**
         * Constructor de una sesión de usuario.
         *
         * @param inicio      hora de inicio de la sesión
         * @param fin         hora de fin de la sesión
         * @param duracion    duración de la sesión
         * @param importadas  número de actividades importadas
         * @param vistas      número de actividades visualizadas
         * @param anotaciones número de anotaciones realizadas
         */
        public Sesion(String inicio, String fin, String duracion,
                      int importadas, int vistas, int anotaciones) {
            this.inicio = inicio;
            this.fin = fin;
            this.duracion = duracion;
            this.importadas = importadas;
            this.vistas = vistas;
            this.anotaciones = anotaciones;
        }

        /**
         * @return hora de inicio de la sesión
         */
        public String getInicio() {
            return inicio;
        }

        /**
         * @return hora de fin de la sesión
         */
        public String getFin() {
            return fin;
        }

        /**
         * @return duración de la sesión
         */
        public String getDuracion() {
            return duracion;
        }

        /**
         * @return número de actividades importadas
         */
        public int getImportadas() {
            return importadas;
        }

        /**
         * @return número de actividades visualizadas
         */
        public int getVistas() {
            return vistas;
        }

        /**
         * @return número de anotaciones realizadas
         */
        public int getAnotaciones() {
            return anotaciones;
        }
    }
    
    /**
     * Navega de vuelta a la pantalla principal del mapa.
     *
     * @param event evento de acción del botón de salir
     */
    @FXML
    private void handleSalir(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
            Parent root = loader.load();
            MapaPrincipalController controller = loader.getController();
            
            controller.setNickname(currentUserNick);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Pantalla principal");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}