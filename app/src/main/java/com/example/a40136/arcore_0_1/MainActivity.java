package com.example.a40136.arcore_0_1;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private Button button;
    private TextView tv;

    private Scene scene;
    private boolean isTaking;
    private int count_pic;
    private int gap = 2;


    private Timer timer;
    private TimerTask timerTask;

    private String str;

    private LinkedList<AnchorNode> anchornodes;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        /*ModelRenderable.builder()
                .setSource(this, Uri.parse("model.sfb"))
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });*/
        anchornodes = new LinkedList<>();

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    /*if (andyRenderable == null) {
                        return;
                    }*/
                    if (anchornodes.size() >= 2) {
                        anchornodes.get(0).setEnabled(false);
                        anchornodes.get(0).getAnchor().detach();
                        anchornodes.pop();
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    anchornodes.add(anchorNode);
                        /*// Create the transformable andy and add it to the anchor.
                        TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                        andy.setParent(anchorNode);
                        andy.setRenderable(andyRenderable);
                        andy.select();*/
                    drawPoint(anchorNode, new Color(0xff0000));
                    // showTV();

 /*                   float[] translation = {0,0,0};
                    float[] rotation = {0,0,0,0};
                    Pose pose = new Pose(translation,rotation);
                    AnchorNode origin = new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(pose));
                    drawPoint(origin, new Color(0x000000));*/

                });


        scene = arFragment.getArSceneView().getScene();
        scene.addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                showTV();
            }
        });

        isTaking = false;
        count_pic = 0;

        tv = findViewById(R.id.tv);

        str = "Basic Information: " + "\r\n" +
                "focalLengthX: 495.4435" + "\r\n" +
                "focalLengthY: 495.42935" + "\r\n" +
                "cameraPricipalPoint_cx: 318.31845" + "\r\n" +
                "cameraPricipalPoint_cy: 236.57025" + "\r\n" +
                "cameraImageDimensions_width: 640.0" + "\r\n" +
                "cameraImageDimensions_height: 480.0" + "\r\n"+ "\r\n"+ "\r\n";

        button = findViewById(R.id.btn_getImage);
        button.setOnClickListener(view -> {
            // takePhoto();
            isTaking = !isTaking;
            if (isTaking) {
                startTimer();
            } else {
                stopTimer();
                writeTxt(str);
            }

        });
    }

    private void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                count_pic++;
                if (anchornodes.size() == 1) {
                    takePhoto();
                }
            }
        };
        timer.schedule(timerTask, 0, 1000 / gap);
    }

    private void stopTimer() {
        timer.cancel();
    }

    private float calculateDistance(List<AnchorNode> anchorNodes) {
        AnchorNode anchorNode_0 = anchorNodes.get(0);
        AnchorNode anchorNode_1 = anchorNodes.get(1);
        float dx = anchorNode_0.getWorldPosition().x - anchorNode_1.getWorldPosition().x;
        float dy = anchorNode_0.getWorldPosition().y - anchorNode_1.getWorldPosition().y;
        float dz = anchorNode_0.getWorldPosition().z - anchorNode_1.getWorldPosition().z;
        float result = Float.valueOf(String.format("%.3f", Math.sqrt(dx * dx + dy * dy + dz * dz)));
        return result;
    }

    private void showTV() {
        ArSceneView arSceneView = arFragment.getArSceneView();
        String txt = "";
        int anchorsSize = arSceneView.getSession().getAllAnchors().size();
        txt += "Anchors size: " + anchorsSize + "\n";
        if (anchorsSize == 2) {
            float distance = calculateDistance(anchornodes);
            txt += "Distance: " + distance + "\n";
            txt += "Point_0: " + anchornodes.get(0).getAnchor().getPose().toString() + "\n";
            txt += "Point_1: " + anchornodes.get(1).getAnchor().getPose().toString() + "\n";
        }

        Camera camera = arFragment.getArSceneView().getArFrame().getCamera();
        String cameraPose = camera.getPose().toString();
        int imageWidth = camera.getImageIntrinsics().getImageDimensions()[0];
        int imageHeight = camera.getImageIntrinsics().getImageDimensions()[1];
        txt += "CameraPose: " + cameraPose + "\n";
        // txt += "imageWidth: "+imageWidth+"\t"+"imageHeight: "+imageHeight+"\n";
        txt += "count: " + count_pic + "\n";


        tv.setText(txt);
    }

    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private String generateFilenameWithInfo(String info) {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + info + ".jpg";
    }


    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(out)));
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    private void takePhoto() {
        // final String filename = generateFilename();

        ArSceneView surfaceView = arFragment.getArSceneView();
        // Create a bitmap the size of the scene view.
        Frame frame = surfaceView.getArFrame();
        Camera camera = frame.getCamera();

        AnchorNode anchorNode0 = anchornodes.get(0);
        //AnchorNode anchorNode1 = anchornodes.get(1);


/*        String cameraPose = camera.getPose().toString();
        int imageWidth = camera.getImageIntrinsics().getImageDimensions()[0];
        int imageHeight = camera.getImageIntrinsics().getImageDimensions()[1];
        float focalLengthX = camera.getImageIntrinsics().getFocalLength()[0];
        float focalLengthY = camera.getImageIntrinsics().getFocalLength()[1];
        float cameraPricipalPoint_cx = camera.getImageIntrinsics().getPrincipalPoint()[0];
        float cameraPricipalPoint_cy = camera.getImageIntrinsics().getPrincipalPoint()[1];
        float cameraImageDimensions_width = camera.getImageIntrinsics().getImageDimensions()[0];
        float cameraImageDimensions_height = camera.getImageIntrinsics().getImageDimensions()[1];
        long cameraTimestamp = frame.getTimestamp();*/

        final String filename = generateFilenameWithInfo(String.valueOf(count_pic)
                /*+ "_CP_" + cameraPose
                + "_FLx_" + focalLengthX
                + "_FLy_" + focalLengthY
                + "_PPcx_" + cameraPricipalPoint_cx
                + "_PPcy_" + cameraPricipalPoint_cy
                + "_ImageWidth_" + cameraImageDimensions_width
                + "_ImageHeight_" + cameraImageDimensions_height
                + "_M1P_" + M1P*/);
        Log.d(TAG, "takePhoto: " + filename);
        str += ""
                + "Num: " + count_pic + "\r\n"
                + getPoseInfo("Camera", camera.getPose())
                + "\r\n"
                + getCameraInfo("Camera",camera)
                + "\r\n"
                + getPoseInfo("Virtual_Camera",camera.getDisplayOrientedPose())
                + "\r\n"
                + getPoseInfo("AndroidSensor", frame.getAndroidSensorPose())
                + "\r\n"
                + getPoseInfo("Node_0", anchorNode0.getAnchor().getPose())
                /*+ "\r\n"
                + getPoseInfo("Node_1", anchorNode1.getAnchor().getPose())*/
                + "-----------------------------------" + "\r\n"
                + "-----------------------------------" + "\r\n"
                + "-----------------------------------" + "\r\n"
                + "\r\n"
                + "\r\n"
                + "\r\n";

        final Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(surfaceView, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("DrawAR", "Failed to copyPixels: " + copyResult);
                Toast toast = Toast.makeText(this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    private void drawPoint(AnchorNode anchorNode, Color color) {
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), color)
                .thenAccept(
                        material -> {
                            ModelRenderable model = ShapeFactory.makeCube(
                                    new Vector3(.01f, .01f, .01f), Vector3.zero(), material
                            );
                            anchorNode.setRenderable(model);
                        }
                );
    }
    private String getCameraInfo(String name, Camera camera){
        String str = "";
        float[] projectionMatrix = new float[16];
        camera.getProjectionMatrix(projectionMatrix,0,0.1f,100.0f);
        str += getMatrixInfo("projectionMatrix",projectionMatrix);
        float[] viewMatrix = new float[16];
        camera.getViewMatrix(viewMatrix, 0);
        str += getMatrixInfo("\r\nviewMatrix",viewMatrix);
        return str;
    }

    private String getPoseInfo(String name, Pose pose) {
        String str = name+"-----------------------------------------------------------------\r\n";
        float[] rotationQuaternion = pose.getRotationQuaternion();
        str += getVectorInfo("rotationQuaternion", rotationQuaternion);
        float[] translation = pose.getTranslation();
        str += getVectorInfo("\r\ntranslation",translation);
        float[] XAxis = pose.getXAxis();
        str += getVectorInfo("\r\nXAxis",XAxis);
        float[] YAxis = pose.getYAxis();
        str += getVectorInfo("\r\nYAxis",YAxis);
        float[] ZAxis = pose.getZAxis();
        str += getVectorInfo("\r\nZAxis",ZAxis);
        float[] matrix = new float[16];
        pose.toMatrix(matrix, 0);
        str += getMatrixInfo("\r\nmatrix",matrix);
        return str;
    }
    private String getMatrixInfo(String name, float[] matrix){
        String str = name+"\r\n";
        str += matrix[0]+"  "
                +matrix[4] + "  "
                +matrix[8] + "  "
                +matrix[12] + "  " +"\r\n"
                +matrix[1] + "  "
                +matrix[5] + "  "
                +matrix[9] + "  "
                +matrix[13] + "  "+"\r\n"
                +matrix[2] + "  "
                +matrix[6] + "  "
                +matrix[10] + "  "
                +matrix[14] + "  "+"\r\n"
                +matrix[3] + "  "
                +matrix[7] + "  "
                +matrix[11] + "  "
                +matrix[15] + "  "+"\r\n";
        return str;
    }

    private String getVectorInfo(String name, float[] vector) {
        String str = "";
        str += name + "[";
        for (int i = 0; i < vector.length-1; i++) {
            str += vector[i] + ", ";
        }
        str += vector[vector.length-1];
        str += "]\r\n";
        return str;
    }

    private void writeTxt(String str) {
        //新建文件夹
        // String folderName = "User";
        // File sdCardDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folderName);
        File sdCardDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/");
        //Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
        if (!sdCardDir.exists()) {
            if (!sdCardDir.mkdirs()) {

                try {
                    sdCardDir.createNewFile();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        try {
            //新建文件
            File saveFile = new File(sdCardDir, "user.txt");

            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            // FileOutputStream outStream =null;
            //outStream = new FileOutputStream(saveFile);

            final FileOutputStream outStream = new FileOutputStream(saveFile);

            try {
                outStream.write(str.getBytes());
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "onClick: --------------" + e.toString());
            }

            Toast.makeText(MainActivity.this, "写入成功", Toast.LENGTH_SHORT).show();
            //outStream.write("测试写入文件".getBytes());
            //outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
