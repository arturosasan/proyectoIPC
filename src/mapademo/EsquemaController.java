package mapademo;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import upv.ipc.sportlib.SportActivityApp;

public class EsquemaController implements Initializable {

    @FXML
    private ListView<Poi> map_listview;

    @FXML
    private ScrollPane map_scrollpane;

    @FXML
    private Slider zoom_slider;

    @FXML
    private Label mousePosition;

    @FXML
    private SplitPane splitPane;

    private SportActivityApp app = SportActivityApp.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (zoom_slider != null) {
            zoom_slider.setMin(0.5);
            zoom_slider.setMax(1.5);
            zoom_slider.setValue(1.0);
        }
    }

    @FXML
    void zoomIn(ActionEvent event) {
        if (zoom_slider != null) {
            zoom_slider.setValue(zoom_slider.getValue() + 0.1);
        }
    }

    @FXML
    void zoomOut(ActionEvent event) {
        if (zoom_slider != null) {
            zoom_slider.setValue(zoom_slider.getValue() - 0.1);
        }
    }

    @FXML
    void listClicked(MouseEvent event) {
    }

    @FXML
    void showPosition(MouseEvent event) {
        if (mousePosition != null && event != null) {
            mousePosition.setText("X: " + (int) event.getX() + " | Y: " + (int) event.getY());
        }
    }

    @FXML
    void cambiarMapa(ActionEvent event) {
    }

    @FXML
    void about(ActionEvent event) {
    }

    @FXML
    void cerrarSesion(ActionEvent event) {
        app.logout();
    }
}