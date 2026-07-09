package com.example.facedetectionydetecciondemallafacial;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FatigueLensActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private ImageView imagePreview;
    private TextView previewText;

    private LinearLayout resultCard;
    private TextView resultTitle;
    private TextView resultText;

    private Uri selectedMediaUri;
    private Uri cameraMediaUri;

    private boolean selectedIsVideo = false;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<Uri> recordVideoLauncher;
    private ActivityResultLauncher<String[]> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fatigue_lens);

        imagePreview = findViewById(R.id.imagePreview);
        previewText = findViewById(R.id.previewText);

        resultCard = findViewById(R.id.resultCard);
        resultTitle = findViewById(R.id.resultTitle);
        resultText = findViewById(R.id.resultText);

        MaterialButton btnCamera = findViewById(R.id.btnCamera);
        MaterialButton btnGallery = findViewById(R.id.btnGallery);
        MaterialButton btnAnalyze = findViewById(R.id.btnAnalyze);

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navAnalysis = findViewById(R.id.navAnalysis);

        configureLaunchers();

        btnCamera.setOnClickListener(v -> checkPermissionsAndShowOptions());
        btnGallery.setOnClickListener(v -> openGallery());
        btnAnalyze.setOnClickListener(v -> analyzeSelectedMedia());

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Configuración pendiente", Toast.LENGTH_SHORT).show()
        );

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        );

        navAnalysis.setOnClickListener(v -> analyzeSelectedMedia());
    }

    private void configureLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraMediaUri != null) {
                        selectedMediaUri = cameraMediaUri;
                        selectedIsVideo = false;

                        hideResultCard();
                        showSelectedImage(selectedMediaUri);

                        Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se capturó ninguna foto", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        recordVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.CaptureVideo(),
                success -> {
                    if (success && cameraMediaUri != null) {
                        selectedMediaUri = cameraMediaUri;
                        selectedIsVideo = true;

                        hideResultCard();
                        showVideoPreview("Video capturado");

                        Toast.makeText(this, "Video guardado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se grabó ningún video", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        selectedMediaUri = uri;

                        String type = getContentResolver().getType(uri);
                        selectedIsVideo = type != null && type.contains("video");

                        hideResultCard();

                        if (selectedIsVideo) {
                            showVideoPreview("Video seleccionado");
                        } else {
                            showSelectedImage(selectedMediaUri);
                        }
                    } else {
                        Toast.makeText(this, "No se seleccionó ningún archivo", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkPermissionsAndShowOptions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            showCameraOptions();
        }
    }

    private void showCameraOptions() {
        String[] options = {"Tomar Foto", "Grabar Video"};

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Seleccionar acción")
                .setItems(options, (dialogInterface, which) -> {
                    if (which == 0) {
                        openCameraForPhoto();
                    } else {
                        openCameraForVideo();
                    }
                })
                .create();

        dialog.show();
    }

    private void openCameraForPhoto() {
        try {
            File photoFile = createMediaFile(".jpg");

            cameraMediaUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureLauncher.launch(cameraMediaUri);

        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCameraForVideo() {
        try {
            File videoFile = createMediaFile(".mp4");

            cameraMediaUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    videoFile
            );

            recordVideoLauncher.launch(cameraMediaUri);

        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo de video", Toast.LENGTH_SHORT).show();
        }
    }

    private File createMediaFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "FL_" + timeStamp + "_";

        File storageDir = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "FatigueLens"
        );

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return File.createTempFile(fileName, extension, storageDir);
    }

    private void openGallery() {
        galleryLauncher.launch(new String[]{"image/*", "video/*"});
    }

    private void showSelectedImage(Uri uri) {
        try {
            imagePreview.setImageTintList(null);
            imagePreview.clearColorFilter();

            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                imagePreview.setImageBitmap(bitmap);
                imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                previewText.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "No se pudo mostrar la imagen", Toast.LENGTH_SHORT).show();
            }

            if (inputStream != null) {
                inputStream.close();
            }

        } catch (Exception e) {
            imagePreview.setImageResource(android.R.drawable.ic_menu_report_image);
            previewText.setText("Error al cargar imagen");
            previewText.setVisibility(View.VISIBLE);
        }
    }

    private void showVideoPreview(String message) {
        imagePreview.setImageTintList(null);
        imagePreview.clearColorFilter();

        imagePreview.setImageResource(android.R.drawable.presence_video_online);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        previewText.setText(message);
        previewText.setVisibility(View.VISIBLE);
    }

    private void analyzeSelectedMedia() {
        if (selectedMediaUri == null) {
            Toast.makeText(this, "Primero captura o selecciona una foto o video", Toast.LENGTH_SHORT).show();
            return;
        }

        resultCard.setVisibility(View.VISIBLE);
        resultTitle.setText("ANALIZANDO...");
        resultText.setText("Procesando archivo con ML Kit Face Detection...");

        if (selectedIsVideo) {
            analyzeVideoFrame(selectedMediaUri);
        } else {
            analyzeImage(selectedMediaUri);
        }
    }

    private void analyzeImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            detectFaces(image, false);

        } catch (IOException e) {
            resultTitle.setText("ERROR");
            resultText.setText("No se pudo preparar la imagen para el análisis.");
        }
    }

    private void analyzeVideoFrame(Uri uri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);

            Bitmap frame = retriever.getFrameAtTime(
                    1000000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            );

            retriever.release();

            if (frame != null) {
                InputImage image = InputImage.fromBitmap(frame, 0);
                detectFaces(image, true);
            } else {
                resultTitle.setText("ERROR");
                resultText.setText("No se pudo extraer un fotograma del video.");
            }

        } catch (Exception e) {
            resultTitle.setText("ERROR");
            resultText.setText("No se pudo analizar el video seleccionado.");
        }
    }

    private void detectFaces(InputImage image, boolean fromVideo) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    resultTitle.setText("RESULTADO DEL ANÁLISIS");
                    resultText.setText(buildFaceResultText(faces, fromVideo));
                })
                .addOnFailureListener(e -> {
                    resultTitle.setText("ERROR");
                    resultText.setText("No se pudo completar el análisis facial.");
                });
    }

    private String buildFaceResultText(List<Face> faces, boolean fromVideo) {
        if (faces == null || faces.isEmpty()) {
            return "No se detectó ningún rostro.\n\n" +
                    "Recomendación: usa buena iluminación y coloca el rostro centrado en la cámara.";
        }

        StringBuilder result = new StringBuilder();

        if (fromVideo) {
            result.append("Archivo: Video\n");
            result.append("Nota: se analizó un fotograma del video.\n\n");
        } else {
            result.append("Archivo: Imagen\n\n");
        }

        result.append("Rostros detectados: ").append(faces.size()).append("\n\n");

        int counter = 1;

        for (Face face : faces) {
            result.append("Rostro ").append(counter).append(":\n");

            Float smilingProb = face.getSmilingProbability();
            Float leftEyeProb = face.getLeftEyeOpenProbability();
            Float rightEyeProb = face.getRightEyeOpenProbability();

            result.append("• Sonrisa: ")
                    .append(probabilityToPercent(smilingProb))
                    .append("\n");

            result.append("• Ojo izquierdo abierto: ")
                    .append(probabilityToPercent(leftEyeProb))
                    .append("\n");

            result.append("• Ojo derecho abierto: ")
                    .append(probabilityToPercent(rightEyeProb))
                    .append("\n");

            //PERCLOS
            if (leftEyeProb != null && rightEyeProb != null) {
                float leftPercent = leftEyeProb * 100;
                float rightPercent = rightEyeProb * 100;
                result.append("• Diagnóstico PERCLOS: ")
                        .append(calcularDiagnosticoPerclos(leftPercent, rightPercent))
                        .append("\n");
            }

            result.append("• Giro horizontal: ")
                    .append(Math.round(face.getHeadEulerAngleY()))
                    .append("°\n");

            result.append("• Inclinación: ")
                    .append(Math.round(face.getHeadEulerAngleZ()))
                    .append("°\n\n");

            counter++;
        }

        return result.toString();
    }
    private String calcularDiagnosticoPerclos(float opizq, float opder) {
        // ALERTA MÁXIMA (Fatiga Crítica / Microsueño): Ambos ojos apertura <= 20%
        if (opizq <= 20 && opder <= 20) {
            return "ALERTA - Microsueño / Ojos Cerrados";
        }
        // ADVERTENCIA (Somnolencia): Uno <= 20% o Ambos entre 21% y 40%
        else if (opizq <= 20 || opder <= 20 || (opizq <= 40 && opder <= 40)) {
            return "ADVERTENCIA - Somnolencia / Fatiga Temprana";
        }
        // ESTADO NORMAL: Ambos > 40%
        else {
            return "NORMAL - Alerta / Saludable";
        }
    }

    private String probabilityToPercent(Float value) {
        if (value == null) {
            return "No disponible";
        }

        int percent = Math.round(value * 100);
        return percent + "%";
    }

    private void hideResultCard() {
        resultCard.setVisibility(View.GONE);
        resultTitle.setText("");
        resultText.setText("");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;

            if (grantResults.length == 0) {
                allPermissionsGranted = false;
            }

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                showCameraOptions();
            } else {
                Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
