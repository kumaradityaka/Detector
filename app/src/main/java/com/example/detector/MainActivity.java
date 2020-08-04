package com.example.detector;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG ="DETECTOR" ;
    MaterialButton DetectObj;
    private GraphicOverlay mGraphicOverlay;
    private CameraView cameraView;
    AlertDialog alertDialog;
    TextView SmileProb;
    AlertDialog startMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DetectObj =findViewById(R.id.detect);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);
        cameraView = findViewById(R.id.cameraView);
        SmileProb = findViewById(R.id.smile_prob);

        alertDialog = new SpotsDialog.Builder()
                            .setMessage("Detecting....")
                            .setContext(this)
                            .setCancelable(false)
                            .build();



        DetectObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.start();
                cameraView.captureImage();
                mGraphicOverlay.clear();
            }
        });


        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                alertDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.getWidth(),cameraView.getHeight(),false);
                cameraView.stop();

                doDetectObject(bitmap);

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        AlertDialog.Builder builder =new AlertDialog.Builder(this)
                                                    .setMessage(" To Detect faces from another picture click on the detect button once again")
                                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    });

        builder.create();


    }

    private void doDetectObject(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                                    .build();
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                        .getVisionFaceDetector(options);
        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        processFaceResult(firebaseVisionFaces);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }

    private void processFaceResult(List<FirebaseVisionFace> firebaseVisionFaces) {
        float smileProb=0.0f;

        int count = 0;
        for(FirebaseVisionFace face : firebaseVisionFaces){
            Rect bounds = face.getBoundingBox();

            //Classification
            if(face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY){
                smileProb = face.getSmilingProbability();
            }
            float check = smileProb*100;
            if(check >= 0 && check <=20){
                SmileProb.setText("Why so Serious?");
            }
            else if(check >=20.1 && check <= 90){
                SmileProb.setText("Put on a Happy Face!");
            }
            else{
                SmileProb.setText("Keep Smiling!");
            }

            RectOverlay rect = new RectOverlay(mGraphicOverlay, bounds);
            mGraphicOverlay.add(rect);
            count++;
        }
        alertDialog.dismiss();
        Toast.makeText(this, String.format("Detected %d faces in image", count), Toast.LENGTH_SHORT).show();

    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        SmileProb.setText(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraView.start();
    }

}
