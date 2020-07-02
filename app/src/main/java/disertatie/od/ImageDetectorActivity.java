package disertatie.od;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import disertatie.od.env.ImageUtils;
import disertatie.od.tflite.Classifier;
import disertatie.od.tflite.Classifier.Recognition;
import disertatie.od.tflite.TFLiteObjectDetectionAPIModel;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class ImageDetectorActivity extends AppCompatActivity {

    private static final String TAG = ImageDetectorActivity.class.getSimpleName();

    private int PICK_IMAGE_REQUEST = 1;
    private static Bitmap bitmap = null;

    private static final int MODEL_INPUT_SIZE = 300;
    private static final boolean IS_MODEL_QUANTIZED = true;
    private static final String MODEL_FILE = "detect.tflite";
    private static final String LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final Size IMAGE_SIZE = new Size(640, 480);

    private Classifier detector;
    private Bitmap croppedBitmap;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Paint myRectPaint = new Paint();
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);
        myRectPaint.setStrokeWidth(2.0f);

        Paint myTextPaint = new Paint();
        myTextPaint.setColor(Color.GREEN);
        myTextPaint.setTextSize(20.0f);
        myTextPaint.setStyle(Paint.Style.FILL);
        myTextPaint.setStrokeWidth(2.0f);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setTextSize(20.0f);

        RectF bbox = new RectF();
        ImageView myImageView = findViewById(R.id.imgView);

        ArrayList<RectF> rectFArrayList = new ArrayList<>();
        ArrayList<String> labelsArrayList = new ArrayList<>();
        ArrayList<Float> confidenceArrayList = new ArrayList<>();

        Button selectImage = findViewById(R.id.detectButton);


        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    detector =
                            TFLiteObjectDetectionAPIModel.create(
                                    getAssets(),
                                    MODEL_FILE,
                                    LABELS_FILE,
                                    MODEL_INPUT_SIZE,
                                    IS_MODEL_QUANTIZED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int cropSize = MODEL_INPUT_SIZE;
                int previewWidth = IMAGE_SIZE.getWidth();
                int previewHeight = IMAGE_SIZE.getHeight();
                int sensorOrientation = 0;
                croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

                frameToCropTransform =
                        ImageUtils.getTransformationMatrix(
                                previewWidth, previewHeight,
                                cropSize, cropSize,
                                sensorOrientation, false);
                cropToFrameTransform = new Matrix();
                frameToCropTransform.invert(cropToFrameTransform);

                Canvas canvas = new Canvas(croppedBitmap);
                try {
                    canvas.drawBitmap(loadImage("test.png"), frameToCropTransform, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final List<Recognition> results = detector.recognizeImage(croppedBitmap);

                float maxConfidence = 0;

                for (Recognition item : results) {
                    if(item.getConfidence().floatValue() > maxConfidence){
                        maxConfidence = item.getConfidence().floatValue();
                    }
                }

                Log.i(TAG + ": MaxConfidence ", String.valueOf(maxConfidence));

                rectFArrayList.clear();
                labelsArrayList.clear();
                confidenceArrayList.clear();

                for (Recognition item : results) {
                    if(item.getConfidence().floatValue() >= 0.5 && !Objects.equals(item.getTitle(), "???")) {

                        cropToFrameTransform.mapRect(bbox, item.getLocation());

                        textView.append(item.getTitle());
                        textView.append(": ");
                        textView.append(item.getConfidence().toString());
                        textView.append("\n");
                        //textView.append(item.getLocation().toString());

                        Log.i(TAG + ": Title ", item.getTitle());
                        Log.i(TAG + ": Confidence ", item.getConfidence().toString());
                        Log.i(TAG + ": Location ", item.getLocation().toString());


                        int x1 = (int) item.getLocation().left;
                        int y1 = (int) item.getLocation().top;
                        int x2 = (int) item.getLocation().right;
                        int y2 = (int) item.getLocation().bottom;

                        rectFArrayList.add(new RectF(x1, y1, x2, y2));
                        labelsArrayList.add(item.getTitle());
                        confidenceArrayList.add(item.getConfidence());

                        //Create a new image bitmap and attach a brand new canvas to it
                        //Bitmap tempBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
                        Canvas tempCanvas = new Canvas(croppedBitmap);

                        //Draw the image bitmap into the cavas
                        tempCanvas.drawBitmap(bitmap, frameToCropTransform, null);


                        //Draw everything else you want into the canvas, in this example a rectangle with rounded edges
                        for(RectF rectf : rectFArrayList) {
                            //tempCanvas.drawRoundRect(rectf, 2, 2, myRectPaint);
                            float cornerSize = Math.min(rectf.width(), rectf.height()) / 8.0f;
                            tempCanvas.drawRoundRect(rectf, cornerSize, cornerSize, myRectPaint);
                            int i = rectFArrayList.indexOf(rectf);
                            final String labelString =
                                    !TextUtils.isEmpty(item.getTitle())
                                            ? String.format("%s %.2f", labelsArrayList.get(i), (100 * confidenceArrayList.get(i)))
                                            : String.format("%.2f", (100 * confidenceArrayList.get(i)));
                            //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
                            // labelString);
                            tempCanvas.drawText( labelString + "%", rectf.left + cornerSize, rectf.top, myTextPaint);
                        }

                        //Attach the canvas to the ImageView
                        myImageView.setImageDrawable(new BitmapDrawable(getResources(), croppedBitmap));
                    }
                }

            }
        });
    }


    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Log.i(TAG, String.valueOf(bitmap));

                ImageView imageView = findViewById(R.id.imgView);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.open_image:
          chooseImage();
        Toast.makeText(this, "Select an image from the gallery", Toast.LENGTH_SHORT).show();
        TextView textView = findViewById(R.id.textView);
        textView.setText(" ");
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }



    private Bitmap loadImage(String fileName) throws Exception {
        if(bitmap == null){
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open(fileName);
            return BitmapFactory.decodeStream(inputStream);
        }
        return bitmap;
    }

    private String translateLabel(int labelPosition, String language){
        ArrayList<String> labelMapEnglish = new ArrayList<>();
        ArrayList<String> labelMapFrench = new ArrayList<>();
        ArrayList<String> labelMapGerman = new ArrayList<>();
        ArrayList<String> labelMapRomanian = new ArrayList<>();

        switch (language){
            case "french":
                return labelMapFrench.get(labelPosition);
            case "german":
                return labelMapGerman.get(labelPosition);
            case "romanian":
                return labelMapRomanian.get(labelPosition);
            default:
                return labelMapEnglish.get(labelPosition);
        }
    }
}
