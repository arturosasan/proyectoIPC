/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;

public class VisualizarActividadController implements Initializable {

  @FXML
  private ComboBox<Activity> actividadComboBox;
  @FXML
  private AnchorPane mapPane;

  @FXML
  private Label distanciaLabel;
  @FXML
  private Label duracionLabel;
  @FXML
  private Label velocidadLabel;
  @FXML
  private Label ritmoLabel;
  @FXML
  private Label desnivelPositivoLabel;
  @FXML
  private Label desnivelNegativoLabel;
  @FXML
  private Label altitudMinimaLabel;
  @FXML
  private Label altitudMaximaLabel;

  private final SportActivityApp app = SportActivityApp.getInstance();

  private MapProjection projection;

  /**
   * Inicializa el controlador de visualización de actividad.
   * Carga la lista de actividades del usuario en el ComboBox.
   *
   * @param url  URL del documento FXML (no usado)
   * @param rb   paquete de recursos (no usado)
   */
  @Override
  public void initialize(URL url, ResourceBundle rb) {
    actividadComboBox.setItems(
        FXCollections.observableArrayList(app.getUserActivities()));
  }

  /**
   * Maneja la selección de una actividad en el ComboBox.
   * Muestra las estadísticas y dibuja el mapa con la ruta.
   */
  @FXML
  private void handleSeleccionarActividad() {
    Activity actividad = actividadComboBox.getValue();

    if (actividad == null) {
      return;
    }

    mostrarEstadisticas(actividad);
    dibujarMapaYRuta(actividad);
  }

  /**
   * Muestra las estadísticas de la actividad en las etiquetas correspondientes.
   * Incluye distancia, duración, velocidad, ritmo, desniveles y altitudes.
   *
   * @param actividad actividad de la que mostrar estadísticas
   */
  private void mostrarEstadisticas(Activity actividad) {
    distanciaLabel.setText(String.format("%.2f km", actividad.getTotalDistance() / 1000.0));
    duracionLabel.setText(actividad.getDuration().toString());
    velocidadLabel.setText(String.format("%.2f km/h", actividad.getAverageSpeed()));
    ritmoLabel.setText(String.format("%.2f min/km", actividad.getAveragePace()));
    desnivelPositivoLabel.setText(String.format("%.0f m", actividad.getElevationGain()));
    desnivelNegativoLabel.setText(String.format("%.0f m", actividad.getElevationLoss()));
    altitudMinimaLabel.setText(String.format("%.0f m", actividad.getMinElevation()));
    altitudMaximaLabel.setText(String.format("%.0f m", actividad.getMaxElevation()));
  }

  /**
   * Dibuja el mapa de fondo y la polilínea de la ruta de la actividad.
   * Proyecta los puntos geográficos sobre la imagen del mapa.
   *
   * @param actividad actividad cuya ruta se va a dibujar
   */
  private void dibujarMapaYRuta(Activity actividad) {
    mapPane.getChildren().clear();

    MapRegion region = actividad.getSuggestedMap();

    if (region == null) {
      Label error = new Label("No hay mapa disponible para esta actividad");
      error.setStyle("-fx-text-fill: white;");
      error.setLayoutX(50);
      error.setLayoutY(50);
      mapPane.getChildren().add(error);
      return;
    }

    File mapaFile = new File(region.getImagePath());

    if (!mapaFile.exists()) {
      Label error = new Label("No se encuentra la imagen del mapa: " + region.getImagePath());
      error.setStyle("-fx-text-fill: white;");
      error.setLayoutX(50);
      error.setLayoutY(50);
      mapPane.getChildren().add(error);
      return;
    }

    Image mapa = new Image(mapaFile.toURI().toString());
    ImageView mapaView = new ImageView(mapa);

    mapaView.setFitWidth(mapPane.getPrefWidth());
    mapaView.setFitHeight(mapPane.getPrefHeight());
    mapaView.setPreserveRatio(false);

    projection = new MapProjection(
        region,
        mapPane.getPrefWidth(),
        mapPane.getPrefHeight());

    Polyline ruta = new Polyline();
    ruta.setStroke(Color.BLUE);
    ruta.setStrokeWidth(3);

    for (TrackPoint tp : actividad.getTrackPoints()) {
      Point2D punto = projection.project(tp);
      ruta.getPoints().addAll(punto.getX(), punto.getY());
    }

    mapPane.getChildren().add(mapaView);
    mapPane.getChildren().add(ruta);

    dibujarInicioFin(actividad);
  }

  /**
   * Dibuja marcadores circulares para el punto de inicio (verde) y fin (rojo).
   *
   * @param actividad actividad con los puntos de inicio y fin
   */
  private void dibujarInicioFin(Activity actividad) {
    if (projection == null) {
      return;
    }

    TrackPoint inicio = actividad.getStartPoint();
    TrackPoint fin = actividad.getEndPoint();

    Point2D pInicio = projection.project(inicio);
    Point2D pFin = projection.project(fin);

    Circle circuloInicio = new Circle(pInicio.getX(), pInicio.getY(), 6);
    circuloInicio.setFill(Color.GREEN);

    Circle circuloFin = new Circle(pFin.getX(), pFin.getY(), 6);
    circuloFin.setFill(Color.RED);

    mapPane.getChildren().add(circuloInicio);
    mapPane.getChildren().add(circuloFin);
  }

  /**
   * Navega de vuelta a la pantalla principal del mapa.
   */
  @FXML
  private void handleVolver() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MapaPrincipal.fxml"));
      Parent root = loader.load();
      Stage stage = (Stage) mapPane.getScene().getWindow();
      stage.setScene(new Scene(root));
      stage.setTitle("Pantalla principal");
      stage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
