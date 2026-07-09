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


        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navAnalysis = findViewById(R.id.navAnalysis);

        configureLaunchers();

        btnCamera.setOnClickListener(v -> checkPermissionsAndShowOptions());
        btnGallery.setOnClickListener(v -> openGallery());
        btnAnalyze.setOnClickListener(v -> analyzeSelectedMedia());


        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        );

        navAnalysis.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ActividadAnalisisVivo.class);
            startActivity(intent);
        });
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
            analyzeVideoFrames(selectedMediaUri);
        } else {
            analyzeImage(selectedMediaUri);
        }
    }

    private void analyzeImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            detectFaces(image);

        } catch (IOException e) {
            resultTitle.setText("ERROR");
            resultText.setText("No se pudo preparar la imagen para el análisis.");
        }
    }

    private void analyzeVideoFrames(Uri uri) {
        try {
            resultTitle.setText("ANALIZANDO VIDEO...");
            resultText.setText("Extrayendo fotogramas del video para un análisis más completo...");

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);

            String durationText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long durationMs = 0;

            if (durationText != null) {
                durationMs = Long.parseLong(durationText);
            }

            if (durationMs <= 0) {
                retriever.release();
                resultTitle.setText("ERROR");
                resultText.setText("No se pudo leer la duración del video.");
                return;
            }

            List<Bitmap> frames = new ArrayList<>();

            int framesPerSecond = 5;
            int maxFrames = 60;
            long intervalMs = 1000 / framesPerSecond;

            for (long timeMs = 0; timeMs < durationMs && frames.size() < maxFrames; timeMs += intervalMs) {
                Bitmap frame = retriever.getFrameAtTime(
                        timeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                );

                if (frame != null) {
                    frames.add(frame);
                }
            }

            retriever.release();

            if (frames.isEmpty()) {
                resultTitle.setText("ERROR");
                resultText.setText("No se pudieron extraer fotogramas del video.");
                return;
            }

            analyzeVideoFramesSequentially(frames);

        } catch (Exception e) {
            resultTitle.setText("ERROR");
            resultText.setText("No se pudo analizar el video seleccionado.");
        }
    }

    private void analyzeVideoFramesSequentially(List<Bitmap> frames) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        VideoAnalysisSummary summary = new VideoAnalysisSummary();
        summary.totalFrames = frames.size();

        processNextVideoFrame(frames, 0, detector, summary);
    }

    private void processNextVideoFrame(
            List<Bitmap> frames,
            int index,
            FaceDetector detector,
            VideoAnalysisSummary summary
    ) {
        if (index >= frames.size()) {
            detector.close();

            resultTitle.setText("RESULTADO DEL ANÁLISIS");
            resultText.setText(buildVideoResultText(summary));
            return;
        }

        resultText.setText("Analizando fotograma " + (index + 1) + " de " + frames.size() + "...");

        Bitmap frame = frames.get(index);
        InputImage image = InputImage.fromBitmap(frame, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    summary.framesAnalyzed++;

                    if (faces != null && !faces.isEmpty()) {
                        summary.framesWithFace++;
                        summary.totalFacesDetected += faces.size();

                        Face face = faces.get(0);

                        Float smilingProb = face.getSmilingProbability();
                        Float leftEyeProb = face.getLeftEyeOpenProbability();
                        Float rightEyeProb = face.getRightEyeOpenProbability();

                        if (leftEyeProb != null && rightEyeProb != null) {
                            float averageEyes = (leftEyeProb + rightEyeProb) / 2f;

                            summary.sumLeftEye += leftEyeProb;
                            summary.sumRightEye += rightEyeProb;
                            summary.eyeDataCount++;

                            if (averageEyes < 0.35f) {
                                summary.closedEyeFrames++;
                            }

                            if (averageEyes >= 0.35f && averageEyes < 0.60f) {
                                summary.lowEyeFrames++;
                            }
                        }

                        if (smilingProb != null) {
                            summary.sumSmile += smilingProb;
                            summary.smileDataCount++;
                        }

                        summary.sumHeadY += face.getHeadEulerAngleY();
                        summary.sumHeadZ += face.getHeadEulerAngleZ();
                        summary.angleDataCount++;
                    }
                })
                .addOnFailureListener(e -> {
                    summary.framesAnalyzed++;
                })
                .addOnCompleteListener(task -> {
                    if (!frame.isRecycled()) {
                        frame.recycle();
                    }

                    processNextVideoFrame(frames, index + 1, detector, summary);
                });
    }

    private void detectFaces(InputImage image) {
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
                    resultText.setText(buildImageResultText(faces));
                })
                .addOnFailureListener(e -> {
                    resultTitle.setText("ERROR");
                    resultText.setText("No se pudo completar el análisis facial.");
                })
                .addOnCompleteListener(task -> detector.close());
    }

    private String buildImageResultText(List<Face> faces) {
        if (faces == null || faces.isEmpty()) {
            return "No se detectó ningún rostro.\n\n" +
                    "Recomendación: usa buena iluminación y coloca el rostro centrado en la cámara.";
        }

        StringBuilder result = new StringBuilder();

        result.append("Archivo: Imagen\n\n");
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

            if (leftEyeProb != null && rightEyeProb != null) {
                float leftPercent = leftEyeProb * 100;
                float rightPercent = rightEyeProb * 100;

                result.append("• Evaluación visual: ")
                        .append(calcularEvaluacionVisual(leftPercent, rightPercent))
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

        result.append("Nota: esta es una estimación visual, no un diagnóstico médico.");

        return result.toString();
    }

    private String buildVideoResultText(VideoAnalysisSummary summary) {
        if (summary.framesWithFace == 0) {
            return "Archivo: Video\n\n" +
                    "Fotogramas analizados: " + summary.framesAnalyzed + "\n" +
                    "No se detectó ningún rostro en los fotogramas analizados.\n\n" +
                    "Recomendación: graba el video con buena iluminación y con el rostro centrado.";
        }

        int denominator = summary.eyeDataCount > 0 ? summary.eyeDataCount : summary.framesWithFace;
        int perclos = Math.round((summary.closedEyeFrames * 100f) / denominator);

        String estadoVisual;

        if (perclos >= 50) {
            estadoVisual = "ALERTA: posible somnolencia / ojos cerrados frecuentes";
        } else if (perclos >= 30) {
            estadoVisual = "PRECAUCIÓN: posible cansancio visual";
        } else {
            estadoVisual = "Normal: ojos mayormente abiertos";
        }

        String leftEyeAvg = "No disponible";
        String rightEyeAvg = "No disponible";
        String smileAvg = "No disponible";

        if (summary.eyeDataCount > 0) {
            leftEyeAvg = Math.round((summary.sumLeftEye / summary.eyeDataCount) * 100) + "%";
            rightEyeAvg = Math.round((summary.sumRightEye / summary.eyeDataCount) * 100) + "%";
        }

        if (summary.smileDataCount > 0) {
            smileAvg = Math.round((summary.sumSmile / summary.smileDataCount) * 100) + "%";
        }

        int avgHeadY = 0;
        int avgHeadZ = 0;

        if (summary.angleDataCount > 0) {
            avgHeadY = Math.round(summary.sumHeadY / summary.angleDataCount);
            avgHeadZ = Math.round(summary.sumHeadZ / summary.angleDataCount);
        }

        return "Archivo: Video\n" +
                "Fotogramas extraídos: " + summary.totalFrames + "\n" +
                "Fotogramas analizados: " + summary.framesAnalyzed + "\n" +
                "Fotogramas con rostro: " + summary.framesWithFace + "\n\n" +

                "Estimación PERCLOS: " + perclos + "%\n" +
                "Estado visual: " + estadoVisual + "\n\n" +

                "Promedios del video:\n" +
                "• Ojo izquierdo abierto: " + leftEyeAvg + "\n" +
                "• Ojo derecho abierto: " + rightEyeAvg + "\n" +
                "• Sonrisa: " + smileAvg + "\n" +
                "• Giro horizontal promedio: " + avgHeadY + "°\n" +
                "• Inclinación promedio: " + avgHeadZ + "°\n\n" +

                "Fotogramas con ojos cerrados: " + summary.closedEyeFrames + "\n" +
                "Fotogramas con ojos parcialmente bajos: " + summary.lowEyeFrames + "\n\n" +

                "Nota: esta es una estimación visual, no un diagnóstico médico.";
    }

    private String calcularEvaluacionVisual(float opizq, float opder) {
        if (opizq <= 20 && opder <= 20) {
            return "ALERTA - Ojos cerrados o posible microsueño";
        } else if (opizq <= 20 || opder <= 20 || (opizq <= 40 && opder <= 40)) {
            return "ADVERTENCIA - Posible somnolencia o fatiga temprana";
        } else {
            return "NORMAL - Ojos mayormente abiertos";
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

    private static class VideoAnalysisSummary {
        int totalFrames = 0;
        int framesAnalyzed = 0;
        int framesWithFace = 0;
        int totalFacesDetected = 0;

        int closedEyeFrames = 0;
        int lowEyeFrames = 0;

        float sumLeftEye = 0f;
        float sumRightEye = 0f;
        int eyeDataCount = 0;

        float sumSmile = 0f;
        int smileDataCount = 0;

        float sumHeadY = 0f;
        float sumHeadZ = 0f;
        int angleDataCount = 0;
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