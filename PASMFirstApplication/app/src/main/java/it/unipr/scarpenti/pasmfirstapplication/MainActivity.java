package it.unipr.scarpenti.pasmfirstapplication;

//per la compatibilità con vecchie API

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Un Tag per filtrare i messaggi nel log
    private static final String TAG = "HelloVisionWorldTag";
    // La classe CameraBridgeViewBase implementa l'interazione tra OpenCV e la tlc
    private CameraBridgeViewBase mOpenCvCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloVisionView);
        // Metti la view come visibile
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        // Registra l'attività this come quella che risponde all'oggetto callback
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chiama l'inizializzazione asincrona e passa l'oggetto callback
        // creato in precendeza, e sceglie quale versione di OpenCV caricare.
        // Serve anche a verificare che l'OpenCV manager installato supporti
        // la versione che si sta provando a caricare.
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    // Questo oggetto callback è usato quando inizializziamo la libreria OpenCV in modo asincrono
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        // Una volta che OpenCV manager è connesso viene chiamato questo metodo di
        public void onManagerConnected(int status) {
            switch (status) {
                // Una volta che OpenCV manager si è connesso con successo
                // possiamo abilitare l'interazione con la tlc
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mRgbaF.release();
        mRgbaT.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        double [] pixelValue=mRgba.get(0,0);
        double canaleR=pixelValue[0];
        double canaleG=pixelValue[1];
        double canaleB=pixelValue[2];
        Log.i(TAG,"valore canale R: "+canaleR);
        Log.i(TAG,"valore canale G: "+canaleG);
        Log.i(TAG,"valore canale B: "+canaleB);
        // Ritorniamo il frame a colori così com'è per essere visualizzato sullo schermo
        return mRgba;
    }
}
