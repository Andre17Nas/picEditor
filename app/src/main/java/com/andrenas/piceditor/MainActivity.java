package com.andrenas.piceditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //CONSTANTS
    private int REQUEST_CODE_GALLERY = 002;

    private boolean HAS_IMAGE_SETED = false;

    private ViewHolder mViewHolder = new ViewHolder();

    /* IMAGE */
    Bitmap imageBitmap, noiseBitmap, originalBitmap;
    Uri imageURI;

    /* MAT */
    Mat src, dst, src_gray;

    Size ksize;

    Thread threadGaussianBlur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();
        Toast.makeText(this, "OpenCv Version: "+OpenCVLoader.OPENCV_VERSION.toString(), Toast.LENGTH_SHORT).show();

        //verify Permissions
        verifyPermissions();

        //viewholder
        this.mViewHolder.img_gallery = this.findViewById(R.id.img_container);
        this.mViewHolder.seekBar_gaussian = this.findViewById(R.id.seek_gaussian);
        this.mViewHolder.seekBar_threshold = this.findViewById(R.id.seek_threshold);
        this.mViewHolder.btn_cancel = this.findViewById(R.id.btn_cancel);


        this.mViewHolder.seekBar_gaussian.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                threadGaussianBlur = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        seek_gaussian(progress);

                    }
                });

                threadGaussianBlur.start();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.mViewHolder.seekBar_threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                Thread sobel = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        seek_threshold(progress);
                    }
                });

                sobel.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_container:
                if (this.mViewHolder.img_gallery != null) {
                    get_fromGallery();
                } else{ Toast.makeText(this, "Selecione uma imagem!", Toast.LENGTH_SHORT).show(); }
                break;
            case R.id.btn_cancel:
                buttonCancel();
                break;
        }
    }

    /*  SET IMAGE */
    public void get_fromGallery(){

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==REQUEST_CODE_GALLERY && data!=null){
            imageURI = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
                originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
            HAS_IMAGE_SETED = true;
            this.mViewHolder.img_gallery.setImageBitmap(imageBitmap);
        }

    }

    /* NOISE */
    public void seek_gaussian(int sigmax){
        src = new Mat();
        dst = new Mat();



        try {
            BitmapFactory.Options bitmapFactory = new BitmapFactory.Options();
            bitmapFactory.inDither = false;
            bitmapFactory.inSampleSize = 4;

            int width = imageBitmap.getWidth();
            int height = imageBitmap.getHeight();

            noiseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);



            Utils.bitmapToMat(imageBitmap, src);
        /*
       *
       *
       * GaussianBlur
        public static void GaussianBlur​(Mat src, Mat dst, Size ksize, double sigmaX)
        Blurs an image using a Gaussian filter. The function convolves the source image with the specified Gaussian kernel. In-place filtering is supported.
        Parameters:
        src - input image; the image can have any number of channels, which are processed independently, but the depth should be CV_8U, CV_16U, CV_16S, CV_32F or CV_64F.
        dst - output image of the same size and type as src.
        ksize - Gaussian kernel size. ksize.width and ksize.height can differ but they both must be positive and odd. Or, they can be zero's and then they are computed from sigma.
        sigmaX - Gaussian kernel standard deviation in X direction. equal to sigmaX, if both sigmas are zeros, they are computed from ksize.width and ksize.height, respectively (see #getGaussianKernel for details); to fully control the result regardless of possible future modifications of all this semantics, it is recommended to specify all of ksize, sigmaX, and sigmaY. SEE: sepFilter2D, filter2D, blur, boxFilter, bilateralFilter, medianBlur
       *
       * */

            ksize = new Size(0, 0);


            Imgproc.GaussianBlur(src, dst, ksize,sigmax);


            Utils.matToBitmap(dst, noiseBitmap);
        } catch (Exception ex){
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
        }

        mViewHolder.img_gallery.setImageBitmap(noiseBitmap);

    }

    public void seek_threshold(int progress){
        src = new Mat();
        src_gray = new Mat();
        dst = new Mat();

        try {
            BitmapFactory.Options bitmapFactory = new BitmapFactory.Options();
            bitmapFactory.inDither = false;
            bitmapFactory.inSampleSize = 4;

            int width = imageBitmap.getWidth();
            int height = imageBitmap.getHeight();

            noiseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            // Remove noise by blurring with a Gaussian filter ( kernel size = 3 )
            // Imgproc.GaussianBlur( src, src, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT );
            // Convert the image to grayscale

            Utils.bitmapToMat(imageBitmap, src);

            Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);
            Imgproc.threshold(src_gray, dst, progress, 255, Imgproc.THRESH_BINARY);

            Utils.matToBitmap(dst, noiseBitmap);

        } catch (Exception ex){
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
        }

        mViewHolder.img_gallery.setImageBitmap(noiseBitmap);

    }

    /* CONFIRM */

    /* CANCEL */
    public void buttonCancel(){

        this.mViewHolder.img_gallery.setImageBitmap(originalBitmap);


    }


    /* verify Permissions */
    private void verifyPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) +
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // ......................................................................... //
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle("PIC EDITOR");
                    alert.setMessage("READ and WRITE");
                    alert.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_GALLERY);
                        }
                    });
                    alert.setNegativeButton("CANCEL", null);
                    AlertDialog alertDialog = alert.create();
                    alertDialog.show();

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_CODE_GALLERY);

                }//checkSelf
            }//checkself
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if((grantResults.length > 0) &&(grantResults[1]) + grantResults[1] == PackageManager.PERMISSION_GRANTED){

        }
    }

    private static class ViewHolder{
        ImageView img_gallery;

        SeekBar seekBar_gaussian;
        SeekBar seekBar_threshold;

        Button btn_cancel;

    }

}
