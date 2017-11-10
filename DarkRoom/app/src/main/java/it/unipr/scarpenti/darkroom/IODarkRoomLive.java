package it.unipr.scarpenti.darkroom;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Arrays;

public class IODarkRoomLive extends AppCompatActivity implements NumberPicker.OnValueChangeListener {

    private static String TAG = "DarkRoomTag";
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;
    Mat sampledImage = null;
    Mat originalImage = null;
    Mat greyImage = null;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    NumberPicker np;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iodark_room);

        np = (NumberPicker) findViewById(R.id.nBin);
        np.setOnValueChangedListener(this);
        np.setMinValue(1);
        np.setMaxValue(256);
        np.setValue(25);
        np.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.iodark_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (sampledImage == null && id != R.id.action_OpenGallery) {
            Context context = getApplicationContext();
            CharSequence text = "Bisogna prima caricare un'immagine!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return true;
        }

        switch (id) {
            case R.id.action_OpenGallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.msg_selectImage)), SELECT_PICTURE);
                return true;
            case R.id.action_Hist:
                return displayHist();
            case R.id.action_togs:
                greyImage = new Mat();
                Imgproc.cvtColor(sampledImage, greyImage, Imgproc.COLOR_RGB2GRAY);
                displayImage(greyImage);
                return true;
            case R.id.action_egs:
                if (greyImage == null) {
                    Context context = getApplicationContext();
                    CharSequence text = "Bisogna prima convertire a livelli di grigio!";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    return true;
                }
                Mat eqGS = new Mat();
                Imgproc.equalizeHist(greyImage, eqGS);
                displayImage(eqGS);
                return true;
            case R.id.action_average:
            case R.id.action_gaussian:
            case R.id.action_median:
                blurImage(id);
                return true;
            case R.id.action_sobel:
                return sobelEdgeDetection(id);
            case R.id.action_canny:
                return cannyEdgeDetection(id);
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    private boolean sobelEdgeDetection(int id) {
        Mat blurredImage=new Mat();
        Size size=new Size(7,7);
        Imgproc.GaussianBlur(sampledImage, blurredImage, size, 0,0);
        Mat gray = new Mat();
        Imgproc.cvtColor(blurredImage, gray, Imgproc.COLOR_RGB2GRAY);
        Mat xFirstDervative =new Mat(),yFirstDervative =new Mat();
        int ddepth= CvType.CV_16S;
        Imgproc.Sobel(gray, xFirstDervative,ddepth , 1,0);
        Imgproc.Sobel(gray, yFirstDervative,ddepth , 0,1);
        Mat absXD=new Mat(),absYD=new Mat();
        Core.convertScaleAbs(xFirstDervative, absXD);
        Core.convertScaleAbs(yFirstDervative, absYD);
        Mat edgeImage=new Mat();
        Core.addWeighted(absXD, 0.5, absYD, 0.5, 0, edgeImage);
        displayImage(edgeImage);
        return true;
    }

    private boolean cannyEdgeDetection(int id) {
        Mat gray = new Mat();
        Imgproc.cvtColor(sampledImage, gray, Imgproc.COLOR_RGB2GRAY);
        Mat edgeImage=new Mat();
        Imgproc.Canny(gray, edgeImage, 100, 200);
        displayImage(edgeImage);
        return true;
    }

    private void blurImage(int id) {

        Mat blurredImage = new Mat();
        Size size = new Size(15, 15);
        int kernelDim = 11;

        switch (id) {
            case R.id.action_average:
                Imgproc.blur(sampledImage, blurredImage, size);
                displayImage(blurredImage);
                return;
            case R.id.action_gaussian:
                Imgproc.GaussianBlur(sampledImage, blurredImage, size, 0, 0);
                displayImage(blurredImage);
                return;
            case R.id.action_median:
                Imgproc.medianBlur(sampledImage, blurredImage, kernelDim);
                displayImage(blurredImage);
                return;
        }
    }

    private boolean displayHist() {
        np.setVisibility(View.VISIBLE);
        Mat histImage = new Mat();
        sampledImage.copyTo(histImage);
        calcHist(histImage);
        displayImage(histImage);
        return true;
    }

    int mHistSizeNum = 25;

    private void calcHist(Mat img) {
        MatOfInt mHistSize = new MatOfInt(mHistSizeNum);
        Mat hist = new Mat();
        float[] mBuff = new float[mHistSizeNum];
        MatOfFloat histogramRanges = new MatOfFloat(0f, 256f);
        Scalar mColorsRGB[] = new Scalar[]{new Scalar(200, 0, 0), new Scalar(0, 200, 0), new Scalar(0, 0, 200)};

        org.opencv.core.Point mP1 = new org.opencv.core.Point();
        org.opencv.core.Point mP2 = new org.opencv.core.Point();
        int thickness = (int) (img.width() / (mHistSizeNum + 10) / 3);
        if (thickness > 3) thickness = 3;

        MatOfInt mChannels[] = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        Size sizeRgba = img.size();
        int offset = (int) (sizeRgba.width - (3 * (mHistSizeNum * thickness + 30)));

        for (int c = 0; c < 3; c++) {
            Imgproc.calcHist(Arrays.asList(img), mChannels[c], new Mat(), hist,
                    mHistSize, histogramRanges);
            Core.normalize(hist, hist, sizeRgba.height / 2, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for (int h = 0; h < mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thickness;
                mP1.y = sizeRgba.height - 1;
                mP2.y = mP1.y - (int) mBuff[h];
                Imgproc.line(img, mP1, mP2, mColorsRGB[c], thickness);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri imgUri = data.getData();
                selectedImagePath = getPath(imgUri);
                Log.i(TAG, "selectedImagePath: " + selectedImagePath);
                loadImage(selectedImagePath);
                displayImage(sampledImage);
            }

        } else {
            Toast t = Toast.makeText(this.getApplicationContext(), R.string.err_selectImage, Toast.LENGTH_LONG);
            t.show();
        }
    }

    private void displayImage(Mat image) {
        // Creiamo una Bitmap
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        // Convertiamo l'immagine di tipo Mat in una Bitmap
        Utils.matToBitmap(image, bitMap);
        // Collego la ImageView e gli assegno la BitMap
        ImageView iv = (ImageView) findViewById(R.id.IODarkRoomImageView);
        iv.setImageBitmap(bitMap);
    }

    private void loadImage(String imagePath) {
        originalImage = Imgcodecs.imread(imagePath);
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
        Display display = getWindowManager().getDefaultDisplay();
        // Qui va selezionato l'import della classe "android graphics Point" !
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        sampledImage = new Mat();
        double downSampleRatio = calculateSubSampleSize(rgbImage, width, height);
        Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio,
                downSampleRatio, Imgproc.INTER_AREA);

        flipOnExifInfo();

    }

    private void flipOnExifInfo() {
        try {
            ExifInterface exif = new ExifInterface(selectedImagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    // ottieni l'immagine specchiata
                    sampledImage = sampledImage.t();
                    // flip lungo l'asse y
                    Core.flip(sampledImage, sampledImage, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    // ottieni l'immagine "sotto-sopra"
                    sampledImage = sampledImage.t();
                    // flip lungo l'asse x
                    Core.flip(sampledImage, sampledImage, 0);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Errore lettura EXIF", e);
            Toast t = Toast.makeText(this.getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            t.show();
        }
    }

    private double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
        // Recuperiamo l'altezza e larghezza dell'immagine sorgente
        int height = srcImage.height();
        int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // Calcoliamo i rapporti tra altezza e larghezza richiesti e quelli dell 'immagine sorgente
            double heightRatio = (double) reqHeight / (double) height;
            double widthRatio = (double) reqWidth / (double) width;
            // Scegliamo tra i due rapporti il minore
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private String getPath(Uri uri) {
        if (uri == null) return null;
        // prova a recuperare l'immagine prima dal Media Store
        // questo però funziona solo per immagini selezionate dalla galleria
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        mHistSizeNum = newVal;
        displayHist();
    }
}
