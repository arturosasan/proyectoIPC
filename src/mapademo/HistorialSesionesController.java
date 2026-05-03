package mapademo;

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
    
    
    public void setNickname(String nick) {
        this.currentUserNick = nick;
    }
    
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
    
    @FXML
    private void handleSalir(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));
            Parent root = loader.load();
            FXMLDocumentController controller = loader.getController();
            
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