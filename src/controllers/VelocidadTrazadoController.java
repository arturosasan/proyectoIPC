/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

public class VelocidadTrazadoController implements Initializable {

    @FXML
    private AnchorPane mapPane;

    @FXML
    private Label infoLabel;

    private Activity actividad;
    private MapProjection projection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void setActividad(Activity actividad) {
        this.actividad = actividad;

        if (actividad != null) {
            dibujarVelocidadSobreMapa();
        }
    }

    private void dibujarVelocidadSobreMapa() {
        mapPane.getChildren().clear();

        MapRegion region = actividad.getSuggestedMap();

        if (region == null) {
            infoLabel.setText("No hay mapa disponible para esta actividad.");
            return;
        }

        File mapaFile = new File(region.getImagePath());

        if (!mapaFile.exists()) {
            infoLabel.setText("No se encuentra el mapa: " + region.getImagePath());
            return;
        }

        Image mapa = new Image(mapaFile.toURI().toString());

        ImageView mapaView = new ImageView(mapa);
        mapaView.setFitWidth(mapPane.getPrefWidth());
        mapaView.setFitHeight(mapPane.getPrefHeight());
        mapaView.setPreserveRatio(false);

        mapPane.getChildren().add(mapaView);

        projection = new MapProjection(
                region,
                mapPane.getPrefWidth(),
                mapPane.getPrefHeight()
        );

        List<TrackPoint> puntos = actividad.getTrackPoints();

        if (puntos == null || puntos.size() < 2) {
            infoLabel.setText("La actividad no tiene suficientes puntos.");
            return;
        }

        double velocidadMaxima = calcularVelocidadMaxima(puntos);

        for (int i = 0; i < puntos.size() - 1; i++) {
            TrackPoint p1 = puntos.get(i);
            TrackPoint p2 = puntos.get(i + 1);

            Point2D punto1 = projection.project(p1);
            Point2D punto2 = projection.project(p2);

            double velocidad = calcularVelocidad(p1, p2);

            Line tramo = new Line(
                    punto1.getX(),
                    punto1.getY(),
                    punto2.getX(),
                    punto2.getY()
            );

            tramo.setStroke(colorPorVelocidad(velocidad, velocidadMaxima));
            tramo.setStrokeWidth(4);

            final double velocidadFinal = velocidad;

            tramo.setOnMouseEntered(event -> {
                infoLabel.setText(
                        String.format("Velocidad del tramo: %.2f km/h", velocidadFinal)
                );
                tramo.setStrokeWidth(7);
            });

            tramo.setOnMouseExited(event -> {
                infoLabel.setText("Pasa el ratón sobre un tramo para ver su velocidad.");
                tramo.setStrokeWidth(4);
            });

            mapPane.getChildren().add(tramo);
        }

        dibujarInicioFin();

        infoLabel.setText("Pasa el ratón sobre un tramo para ver su velocidad.");
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

            double horas = tiempo.toSeconds() / 3600.0;
            double kilometros = distanciaMetros / 1000.0;

            return kilometros / horas;

        } catch (Exception e) {
            return 0;
        }
    }

    private Color colorPorVelocidad(double velocidad, double velocidadMaxima) {
        if (velocidadMaxima <= 0) {
            return Color.web("#55d6b3");
        }

        double porcentaje = velocidad / velocidadMaxima;

        if (porcentaje < 0.33) {
            return Color.web("#16c7e8");
        } else if (porcentaje < 0.66) {
            return Color.web("#55d6b3");
        } else {
            return Color.web("#ef4264");
        }
    }

    private void dibujarInicioFin() {
        TrackPoint inicio = actividad.getStartPoint();
        TrackPoint fin = actividad.getEndPoint();

        Point2D pInicio = projection.project(inicio);
        Point2D pFin = projection.project(fin);

        Circle circuloInicio = new Circle(pInicio.getX(), pInicio.getY(), 7);
        circuloInicio.setFill(Color.GREEN);

        Circle circuloFin = new Circle(pFin.getX(), pFin.getY(), 7);
        circuloFin.setFill(Color.RED);

        mapPane.getChildren().addAll(circuloInicio, circuloFin);
    }

    @FXML
    private void volver() {
        mapPane.getScene().getWindow().hide();
    }
}
