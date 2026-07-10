package com.example.facedetectionydetecciondemallafacial;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SuperposicionGrafica extends View {

    //Variables
    private final Object lock = new Object();
    private int imageWidth;
    private int imageHeight;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;
    private int facing = 1;
    private final List<Grafico> graficos = new ArrayList<>();

    public abstract static class Grafico {
        private final SuperposicionGrafica overlay;

        public Grafico(SuperposicionGrafica overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public float translateX(float x) {
            if (overlay.facing == 1) {
                return overlay.getWidth() - (x * overlay.widthScaleFactor);
            }
            return x * overlay.widthScaleFactor;
        }

        public float translateY(float y) {
            return y * overlay.heightScaleFactor;
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public SuperposicionGrafica(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void clear() {
        synchronized (lock) {
            graficos.clear();
        }
        postInvalidate();
    }

    public void add(Grafico grafico) {
        synchronized (lock) {
            graficos.add(grafico);
        }
        postInvalidate();
    }

    public void setCameraInfo(int width, int height, int facing) {
        synchronized (lock) {
            this.imageWidth = width;
            this.imageHeight = height;
            this.facing = facing;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            if (imageWidth != 0 && imageHeight != 0) {
                widthScaleFactor = (float) getWidth() / imageWidth;
                heightScaleFactor = (float) getHeight() / imageHeight;
            }

            for (Grafico grafico : graficos) {
                grafico.draw(canvas);
            }
        }
    }
}
