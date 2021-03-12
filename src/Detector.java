
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class Detector {
    // FXML camera button
    @FXML
    private Button cameraButton;
    @FXML
    private ImageView originalFrame;
    @FXML
    private ImageView maskGImage;
    @FXML
    private ImageView maskRImage;
    @FXML
    private ImageView morphImage;

    /*FXML label to show the current values set with the sliders

    @FXML
    private Label hsvValues;*/

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that performs the video capture
    private VideoCapture capture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive;

    // property for object binding
    private ObjectProperty<String> hsvValuesProp;


    @FXML
    private void startCamera() {
      /*  // HSV values for object detection
        hsvValuesProp = new SimpleObjectProperty<>();
        this.hsvValues.textProperty().bind(hsvValuesProp);
        + rajouter private Vmax,Vmin... et slider dans FXML
       */

        // set a fixed width for all the image to show and preserve image ratio
        this.imageViewProperties(this.originalFrame, 400);
        this.imageViewProperties(this.maskGImage, 200);
        this.imageViewProperties(this.maskRImage, 200);

        if (!this.cameraActive) {
            // start the video capture
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run() {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();
                        // convert and show the frame
                        Image imageToShow = Conversion.mat2Image(frame);
                        updateImageView(originalFrame, imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.cameraButton.setText("Stop Camera");
            } else {
                // log the error
                System.err.println("Failed to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");

            // stop the timer
            this.stopAcquisition();
        }
    }

    private Mat grabFrame() {
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    // init
                    Mat blurredImage = new Mat();
                    Mat hsvImage = new Mat();
                    Mat mask = new Mat();
                    Mat morphOutput = new Mat();

                    // remove some noise
                    Imgproc.blur(frame, blurredImage, new Size(7, 7));

                    // convert the frame to HSV
                    Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

                    // get thresholding values from the UI
                    // remember: H ranges 0-180, S and V range 0-255
                    // threshold HSV image to select green and red tumbler
                    Scalar VminValues = new Scalar(40,120,0);
                    Scalar VmaxValues = new Scalar(100,255,255);
                    Core.inRange(hsvImage, VminValues, VmaxValues, mask);
                    this.updateImageView(this.maskGImage, Conversion.mat2Image(mask));
                    TumblerTracking(mask,frame);
                    this.updateImageView(this.originalFrame, Conversion.mat2Image(frame));


                    Scalar RminValues = new Scalar(0,125,100);
                    Scalar RmaxValues = new Scalar(0,255,255);
                    Core.inRange(hsvImage, RminValues, RmaxValues, mask);
                    this.updateImageView(this.maskRImage, Conversion.mat2Image(mask));
                    TumblerTracking(mask,frame);
                    this.updateImageView(this.originalFrame, Conversion.mat2Image(frame));

                    // morphological operators
                    // dilate with large element, erode with small ones
                    Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
                    Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

                    Imgproc.erode(mask, morphOutput, erodeElement);
                    Imgproc.erode(morphOutput, morphOutput, erodeElement);

                    Imgproc.dilate(morphOutput, morphOutput, dilateElement);
                    Imgproc.dilate(morphOutput, morphOutput, dilateElement);





                }

            } catch (Exception e) {
                // log the (full) error
                System.err.print("Exception during the image elaboration...");
                e.printStackTrace();
            }
        }

        return frame;
    }


    private Mat TumblerTracking(Mat maskedImage, Mat frame) {
        // init
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        /*List<Point> centroid = new ArrayList<>();
        List<Moments> moments=new ArrayList<>();


        // find contours
        Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0)  {
            for (int i = 0; i < contours.size() ; i++) {
                moments.set(i, Imgproc.moments(contours.get(i)));
                centroid.set(i , moments.get(i).get_m10() / moments.get(i).get_m00();
                centroid.set(i).y = moments.get(i).get_m01() / moments.get(i).get_m00();
           }
           */

            // for each contour, display it in blue
        ArrayList<Point> centroid=new ArrayList<>();
        ArrayList<Moments> moments= new ArrayList<>(); //hyper shlag

        // find contours
        Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0)  {
            for (int i = 0; i < contours.size() ; i++) {
                moments.add(Imgproc.moments(contours.get(i)));
                centroid.add(new Point(moments.get(i).get_m10() / moments.get(i).get_m00(),(moments.get(i).get_m01()/moments.get(i).get_m00())));
               // String centroids = "";
               // centroid.forEach(c -> centroids.concat( c.x + " " + c.y));
               // System.out.println("Centroid is situated here : " + centroid);

            }


                for (int i = 0; i >= 0; i = (int) hierarchy.get(0, i)[0]) {
                    Imgproc.drawContours(frame, contours, i, new Scalar(250, 0, 0), 2, Imgproc.LINE_8, hierarchy);
                }


        }
         //Positions + retour de matrice
        //String position;
         //if (position == "b") {
            double dist1;
            double dist2;
            double dist3;
            if (centroid.size() == 6) {
                if (Math.abs(centroid.get(2).y - centroid.get(0).y) == Math.abs(centroid.get(2).y - centroid.get(0).y)) {
                    dist1 = Math.abs(centroid.get(2).y - centroid.get(0).y);
                    if (dist1 <= 75 && dist1 >= 65) {
                        System.out.print("RVRVV");
                    }
                }
                if (Math.abs(centroid.get(4).y - centroid.get(2).y) == Math.abs(centroid.get(4).y - centroid.get(2).y)) {
                    dist2 = Math.abs(centroid.get(4).y - centroid.get(2).y);
                    if (dist2 <= 72 && dist2 >= 62) {
                        System.out.print("RVVRV");
                    }
                }
                if (Math.abs(centroid.get(5).y - centroid.get(0).y) == Math.abs(centroid.get(5).y - centroid.get(0).y)) {
                    dist3 = Math.abs(centroid.get(5).y - centroid.get(0).y);
                    if (dist3 <= 70 && dist3 >= 60) {
                        System.out.print("RRVVV");
                    }
                }
                if (Math.abs(centroid.get(2).y - centroid.get(0).y) == Math.abs(centroid.get(2).y - centroid.get(0).y)) {
                    dist1 = Math.abs(centroid.get(2).y - centroid.get(0).y);
                    if (dist1 <= 115 && dist1 >= 105) {
                        System.out.print("VRRVR");

                    } else if (dist1 <= 80 && dist1 >= 70) {
                        System.out.print("VRVRR");

                    } else if (dist1 <= 40 && dist1 >= 30) {
                        System.out.print("VVRRR");

                    }
                }
            }

         /* Confirmer avec ancien code Yasmine
           if (position == "j") {
                    double dist1;
                    if (mc.size() != 4) {
                        int results;
                        std::tie (results, mc) =cameraTraitement();
                    } else {
                    */



        return frame;
        
    }



    private void imageViewProperties(ImageView image, int dimension)
    {
        // set a fixed width for the given ImageView
        image.setFitWidth(dimension);
        // preserve the image ratio
        image.setPreserveRatio(true);
    }


    private void stopAcquisition() {

        if (this.timer!=null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // log any exception
                System.err.println("Problème de capture caméra " + e);
            }
        }

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }


    private void updateImageView(ImageView view, Image image)
    {
        Conversion.onFXThread(view.imageProperty(), image);
    }


    protected void setClosed()
    {
        this.stopAcquisition();
    }

}