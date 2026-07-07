package com.example.facedetectionydetecciondemallafacial;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import java.io.InputStream;

public class FatigueLensActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private ImageView imagePreview;
    private TextView previewText;
    private Uri selectedMediaUri;
    private Uri cameraMediaUri;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<Uri> recordVideoLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fatigue_lens);

        imagePreview = findViewById(R.id.imagePreview);
        previewText = findViewById(R.id.previewText);

        final MaterialButton btnCamera = findViewById(R.id.btnCamera);
        final MaterialButton btnGallery = findViewById(R.id.btnGallery);
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navAnalysis = findViewById(R.id.navAnalysis);

        configureLaunchers();

        btnCamera.setOnClickListener(v -> checkPermissionsAndShowOptions());
        btnGallery.setOnClickListener(v -> openGallery());

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Configuración pendiente", Toast.LENGTH_SHORT).show()
        );

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        );

        navAnalysis.setOnClickListener(v -> {
            if (selectedMediaUri == null) {
                Toast.makeText(this, "Primero captura o selecciona un archivo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Archivo listo para análisis", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraMediaUri != null) {
                        selectedMediaUri = cameraMediaUri;
                        showSelectedImage(selectedMediaUri);
                        Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        recordVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.CaptureVideo(),
                success -> {
                    if (success && cameraMediaUri != null) {
                        selectedMediaUri = cameraMediaUri;
                        imagePreview.setImageResource(android.R.drawable.presence_video_online);
                        previewText.setText("Video capturado");
                        previewText.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Video guardado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    findViewById(R.id.btnGallery).setSelected(false);

                    if (uri != null) {
                        selectedMediaUri = uri;
                        String type = getContentResolver().getType(uri);
                        if (type != null && type.contains("video")) {
                            imagePreview.setImageResource(android.R.drawable.presence_video_online);
                            previewText.setText("Video seleccionado");
                            previewText.setVisibility(View.VISIBLE);
                        } else {
                            showSelectedImage(selectedMediaUri);
                        }
                    }
                }
        );
    }

    private void checkPermissionsAndShowOptions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            showCameraOptions();
        }
    }

    private void showCameraOptions() {
        final MaterialButton btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setSelected(true);

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
        dialog.setOnDismissListener(dialogInterface -> btnCamera.setSelected(false));

        dialog.show();
    }

    private void openCameraForPhoto() {
        try {
            File photoFile = createMediaFile(".jpg");
            cameraMediaUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(cameraMediaUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCameraForVideo() {
        try {
            File videoFile = createMediaFile(".mp4");
            cameraMediaUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", videoFile);
            recordVideoLauncher.launch(cameraMediaUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private File createMediaFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "FL_" + timeStamp + "_";
        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FatigueLens");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(fileName, extension, storageDir);
    }

    private void openGallery() {
        MaterialButton btnGallery = findViewById(R.id.btnGallery);
        // Activa inversión de colores mientras la galería está abierta
        btnGallery.setSelected(true);
        galleryLauncher.launch("*/*");
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
            }
            if (inputStream != null) inputStream.close();
        } catch (Exception e) {
            imagePreview.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraOptions();
            } else {
                Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
