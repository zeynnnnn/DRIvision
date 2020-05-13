package org.tensorflow.lite.examples.detection.tracking;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import android.os.Build;
import android.os.IBinder;
import android.os.Trace;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;

import jp.co.recruit_lifestyle.sample.service.FloatingViewService;

import static com.example.simon.cameraapp.CameraService.getPreviewHeight;
import static com.example.simon.cameraapp.CameraService.getPreviewWidth;
import static jp.co.recruit_lifestyle.sample.service.FloatingViewService.changeSpeedSign;


public class DetectorService extends Service {

    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "retrained_graph.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    @SuppressLint("NewApi")
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private static Runnable postInferenceCallback;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;
    protected static int previewWidth = 0;
    protected static int previewHeight = 0;
    private Bitmap rgbFrameBitmap = null;
    private static Bitmap croppedBitmap = null;
    private static boolean isProcessingFrame = false;
    private static byte[][] yuvBytes = new byte[3][];
    private static int[] rgbBytes = null;
    private static int yRowStride;
    private static Runnable imageConverter;

    private static Classifier detector;

    private static boolean computingDetection = false;

    boolean mBounded;
    static FloatingViewService mServer;
    static HashSet<String> speedLabels;
    static String previousLabel;
    public static  boolean started = false;

    public DetectorService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        previousLabel = "";
        speedLabels = new HashSet<String>();
        speedLabels.add("speed_limit_20");
        speedLabels.add("speed_limit_30");
        speedLabels.add("speed_limit_50");
        speedLabels.add("speed_limit_60");
        speedLabels.add("speed_limit_70");
        speedLabels.add("speed_limit_80");
        speedLabels.add("speed_limit_100");
        speedLabels.add("speed_limit_120");

        /*
        Bundle bundle = intent.getExtras();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Size size =  bundle.getSize("size");
        }

         */

        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
        }

        previewWidth = getPreviewWidth();
        previewHeight = getPreviewHeight();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        started = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void detectTrafficSign(Bitmap croppedBitmap){
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            /*
            final Image image = ImageIO.read(new File(imagePath));

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

             */

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            //image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage(croppedBitmap);
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    public static void processImage(Bitmap croppedBitmap) {

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        readyForNextImage();

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        //System.out.println("MBOUNDED: " + mBounded);
    /*
    if(!mBounded) {
      Intent mIntent = new Intent(this, ChatHeadService.class);
      bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

     */
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {

                // Update Service HERE
                //System.out.println("RESULT ID: " + result.getTitle());
                if(speedLabels.contains(result.getTitle()) && !result.getTitle().equals(previousLabel)) {
                    //mServer.changeSpeedSign(result.getTitle());
                    changeSpeedSign(result.getTitle());
                    previousLabel = result.getTitle();
                }
            }
        }

        computingDetection = false;
    }

    private enum DetectorMode {
        TF_OD_API;
    }

    public static void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }
}
