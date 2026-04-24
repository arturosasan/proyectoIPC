package mapademo;
import javafx.geometry.Point2D;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jose
 */
public class Poi {
    
    private String code;
    
    private Point2D position;

    /**
     * Get the value of position
     *
     * @return the value of position
     */
    public Point2D getPosition() {
        return position;
    }

    /**
     * Set the value of position
     *
     * @param position new value of position
     */
    public void setPosition(Point2D position) {
        this.position = position;
    }





    /**
     * Get the value of code
     *
     * @return the value of code
     */
    public String getCode() {
        return code;
    }

    /**
     * Set the value of code
     *
     * @param code new value of code
     */
    public void setCode(String code) {
        this.code = code;
    }
    
    
    Poi(String cod, double x, double y){
        code=cod;
 
        position= new Point2D(x,y);
        
    }

    Poi() {
         
    }

    @Override
    public String toString() {
        return  code + " [x:" +position.getX()+" y: "+position.getY()+"]" ;
    }
    
}

    

