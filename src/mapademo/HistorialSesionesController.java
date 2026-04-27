package mapademo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class HistorialSesionesController implements Initializable {

    @FXML
    private TableView<Sesion> tablaSesiones;

    @FXML
    private TableColumn<Sesion, String> inicioColum;

    @FXML
    private TableColumn<Sesion, String> finColum;

    @FXML
    private TableColumn<Sesion, String> duracionColum;

    @FXML
    private TableColumn<Sesion, Integer> importadasColum;

    @FXML
    private TableColumn<Sesion, Integer> vistasColum;

    @FXML
    private TableColumn<Sesion, Integer> anotacionesColum;

    @FXML
    private Label totalImportadasLabel;

    @FXML
    private Label totalVistasLabel;

    @FXML
    private Label totalAnotacionesLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        inicioColum.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        finColum.setCellValueFactory(new PropertyValueFactory<>("fin"));
        duracionColum.setCellValueFactory(new PropertyValueFactory<>("duracion"));
        importadasColum.setCellValueFactory(new PropertyValueFactory<>("importadas"));
        vistasColum.setCellValueFactory(new PropertyValueFactory<>("vistas"));
        anotacionesColum.setCellValueFactory(new PropertyValueFactory<>("anotaciones"));

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

        public Sesion(String inicio, String fin, String duracion,
                      int importadas, int vistas, int anotaciones) {
            this.inicio = inicio;
            this.fin = fin;
            this.duracion = duracion;
            this.importadas = importadas;
            this.vistas = vistas;
            this.anotaciones = anotaciones;
        }

        public String getInicio() {
            return inicio;
        }

        public String getFin() {
            return fin;
        }

        public String getDuracion() {
            return duracion;
        }

        public int getImportadas() {
            return importadas;
        }

        public int getVistas() {
            return vistas;
        }

        public int getAnotaciones() {
            return anotaciones;
        }
    }
}