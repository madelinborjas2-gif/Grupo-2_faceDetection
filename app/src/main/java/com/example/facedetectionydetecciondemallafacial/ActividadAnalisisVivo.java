package com.example.facedetectionydetecciondemallafacial;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActividadAnalisisVivo extends AppCompatActivity {
    private static final String TAG = "ActividadAnalisisVivo";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private PreviewView previewView;
    private SuperposicionGrafica graphicOverlay;
    private TextView tvPerclos, tvEyeStatus, tvLeftEye, tvRightEye;
    private View alertCard;

    private ProcessCameraProvider cameraProvider;
    private final CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
    private ExecutorService cameraExecutor;
    private FaceMeshDetector detector; //Deteccion de la malla facial

    private final Deque<Boolean> eyeStatusWindow = new ArrayDeque<>(); // Cola para frames para calculo PERCLOS
    private static final int WINDOW_SIZE = 100;

    // Para el sonido de alerta
    private ToneGenerator toneGenerator;
    private int alertCooldown = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analisis_vivo);

        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        tvPerclos = findViewById(R.id.tvPerclos);
        tvEyeStatus = findViewById(R.id.tvEyeStatus);
        tvLeftEye = findViewById(R.id.tvLeftEye);
        tvRightEye = findViewById(R.id.tvRightEye);
        alertCard = findViewById(R.id.alertCard);
        ImageButton btnBack = findViewById(R.id.btnBack);

        cameraExecutor = Executors.newSingleThreadExecutor();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        btnBack.setOnClickListener(v -> finish());

        if (checkPermissions()) {
            startCamera();
        } else {
            requestPermissions();
        }

        setupDetector();
    }

    //Verificion de permisos
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    //Solicitud de permisos
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    //Configuracion el detector de malla facial
    private void setupDetector() {
        FaceMeshDetectorOptions options = new FaceMeshDetectorOptions.Builder()
                .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build();
        detector = FaceMeshDetection.getClient(options);
    }

    //Inicia la camara
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //Configuracion de la camara y analisis de imagenes
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
        }
    }


    //Detectar rostros con face mesh y analisis
    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(androidx.camera.core.ImageProxy image) {
        android.media.Image mediaImage = image.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
            detector.process(inputImage)
                    .addOnSuccessListener(faceMeshes -> {
                        graphicOverlay.clear();
                        
                        int rotation = image.getImageInfo().getRotationDegrees();
                        if (rotation == 90 || rotation == 270) {
                            graphicOverlay.setCameraInfo(image.getHeight(), image.getWidth(), 1);
                        } else {
                            graphicOverlay.setCameraInfo(image.getWidth(), image.getHeight(), 1);
                        }

                        if (!faceMeshes.isEmpty()) {
                            FaceMesh faceMesh = faceMeshes.get(0);
                            graphicOverlay.add(new FaceMeshGrafico(graphicOverlay, faceMesh));
                            analyzeFatigue(faceMesh);
                        } else {
                            updateUINoFace();
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error en detección", e))
                    .addOnCompleteListener(task -> image.close());
        } else {
            image.close();
        }
    }


    //Calculo del EAR(Eye Aspect Ratio) para saber si los ojos estan cerrados o abiertos en cada fotograma
    private void analyzeFatigue(FaceMesh faceMesh) {


        //Numeros fijos de los puntos correspondientes a los ojos en la malla facial de 468 puntos
        float leftEAR = calculateEAR(faceMesh, 159, 145, 33, 133);
        float rightEAR = calculateEAR(faceMesh, 386, 374, 362, 263);
        
        // Umbral EAR: <0.16 para ojos cerrados
        // Umbral EAR: 0.25 - 0.35 para ojos abiertos
        boolean eyesClosed = (leftEAR < 0.16f && rightEAR < 0.16f);

        runOnUiThread(() -> {
            tvLeftEye.setText(String.format(Locale.getDefault(), "Ojo Izq (EAR): %.2f", leftEAR));
            tvRightEye.setText(String.format(Locale.getDefault(), "Ojo Der (EAR): %.2f", rightEAR));
            updatePerclos(eyesClosed);
        });
    }


    //Obtencio de los valores de los puntos de la malla facial y calculo de EAR = distancia vertical del ojo/ distancia horizontal del ojo
    private float calculateEAR(FaceMesh faceMesh, int top, int bottom, int left, int right) {
        float verticalDist = getDistance(faceMesh.getAllPoints().get(top).getPosition(), 
                                         faceMesh.getAllPoints().get(bottom).getPosition());
        float horizontalDist = getDistance(faceMesh.getAllPoints().get(left).getPosition(), 
                                           faceMesh.getAllPoints().get(right).getPosition());
        return verticalDist / horizontalDist;
    }

    //Distancia euclidiana entre dos puntos, si es < 0.16 se considera ojos cerrados
    private float getDistance(com.google.mlkit.vision.common.PointF3D p1, com.google.mlkit.vision.common.PointF3D p2) {
        return (float) Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    private void updatePerclos(boolean closed) {
        if (eyeStatusWindow.size() >= WINDOW_SIZE) {
            eyeStatusWindow.removeFirst();
        }
        eyeStatusWindow.addLast(closed);

        int closedCount = 0;
        for (Boolean b : eyeStatusWindow) {
            if (b) closedCount++;
        }

        float perclos = (float) closedCount / WINDOW_SIZE * 100;
        tvPerclos.setText(String.format(Locale.getDefault(), "PERCLOS: %.1f%%", perclos));

        // Un PERCLOS > 12% en una ventana de aprox 3 segundos indica ojos cerrados por más de 0.6s (fatiga real).
        if (perclos > 12) { 
            alertCard.setVisibility(View.VISIBLE);
            tvEyeStatus.setText("Estado: FATIGA DETECTADA");
            
            if (alertCooldown <= 0) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 2500);
                alertCooldown = 60;
            }
        } else {
            alertCard.setVisibility(View.GONE);
            tvEyeStatus.setText("Estado: Normal");
        }
        
        if (alertCooldown > 0) alertCooldown--;
    }

    private void updateUINoFace() {
        runOnUiThread(() -> {
            tvEyeStatus.setText("Estado: No se detecta rostro");
            graphicOverlay.clear();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (detector != null) {
            detector.close();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
