package com.example.facedetectionydetecciondemallafacial;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import java.util.List;

public class FaceMeshGrafico extends SuperposicionGrafica.Grafico {
    private final FaceMesh faceMesh;
    private final Paint dotPaint;
    private final Paint connectionPaint;

    public FaceMeshGrafico(SuperposicionGrafica overlay, FaceMesh faceMesh) {
        super(overlay);
        this.faceMesh = faceMesh;

        dotPaint = new Paint();
        dotPaint.setColor(Color.CYAN);
        dotPaint.setAlpha(200);
        dotPaint.setStrokeWidth(2.0f);
        dotPaint.setStyle(Paint.Style.FILL);

        connectionPaint = new Paint();
        connectionPaint.setColor(Color.parseColor("#D6A014")); // fl_gold
        connectionPaint.setAlpha(80);
        connectionPaint.setStrokeWidth(0.5f);
    }

    @Override
    public void draw(Canvas canvas) {
        if (faceMesh == null) return;

        List<FaceMeshPoint> points = faceMesh.getAllPoints();
        
        // Dibujamos los puntos con un pequeño brillo cian
        for (FaceMeshPoint point : points) {
            PointF3D position = point.getPosition();
            float x = translateX(position.getX());
            float y = translateY(position.getY());
            canvas.drawCircle(x, y, 1.2f, dotPaint);
        }
        
        // Para dar el efecto de "malla" de la documentación, conectamos puntos cercanos
        // Nota: En la versión beta de ML Kit, esto simula la red sin necesidad de los índices de triángulos
        for (int i = 0; i < points.size(); i += 8) { // Saltamos para no saturar el dibujo
            PointF3D p1 = points.get(i).getPosition();
            for (int j = i + 1; j < Math.min(i + 4, points.size()); j++) {
                PointF3D p2 = points.get(j).getPosition();
                canvas.drawLine(translateX(p1.getX()), translateY(p1.getY()), 
                                translateX(p2.getX()), translateY(p2.getY()), connectionPaint);
            }
        }
    }
}
