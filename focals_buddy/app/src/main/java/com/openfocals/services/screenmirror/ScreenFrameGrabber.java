package com.openfocals.services.screenmirror;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.openfocals.services.DeviceService;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;




import java.io.IOException;



public class ScreenFrameGrabber {
    private static final String TAG = ScreenFrameGrabber.class.getSimpleName();
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;


    ////////////////////////////////////
    //
    private static final int RECHECK_MS = 50;
    private static final int MIN_FRAME_GAP_MS = 0; //250;
     
    private MediaProjection projection_;
    private VirtualDisplay display_;
    ImageReader imgreader_ = null;
    protected final int width_;
    protected final int height_;
    private final int density_;
    private final Handler handler_;

    public boolean started = false;
    private Socket socket_;

    OutputStream output_ = null; // = s.getOutputStream();
    boolean did_socket_ = false;
    
    long last_frame_ms_ = 0;
    String pend_frame_;


    ScreenFrameListener listener_;

    public void setListener(ScreenFrameListener listener)
    {
        listener_ = listener;
    }

    public ScreenFrameGrabber(final MediaProjection projection,
                              final int width,
                              final int height,
                              final int density
    ) {
        projection_ = projection;

        width_ = width;
        height_ = height;
        density_ = density;
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        handler_ = new Handler(thread.getLooper());
        Rect f = DeviceService.getInstance().screen_cap_rect;

        if (f == null)
        {
            DeviceService.getInstance().screen_cap_rect = new Rect(0, 0, 220, 220);
        }
    }


    public void start()
    {
        started = true;
        imgreader_ = ImageReader.newInstance(width_, height_, PixelFormat.RGBA_8888, 5);
        projection_.createVirtualDisplay("screencapture",
                width_, height_, density_,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imgreader_.getSurface(), null, handler_);
        imgreader_.setOnImageAvailableListener(new ImageAvailableListener(), handler_); //null);


        // Frame delay in push model (currently disabled)
        //if (MIN_FRAME_GAP_MS != 0) {
        //    handler_.postDelayed(
        //            new Runnable() {
        //                public void run() {
        //                    checkSendFrame();
        //                }
        //            }, RECHECK_MS);
        //}

    }

    public void stop()
    {
        started = false;
    }

    void checkSendFrame()
    {
        if (!started) return;

        if (pend_frame_ != null)
        {
            long curms = System.currentTimeMillis();
            if ((curms - last_frame_ms_) >= MIN_FRAME_GAP_MS)
            {
                if (listener_ != null) {
                    Log.i(TAG, "Sending pended frame");
                    listener_.onFrameData(pend_frame_);

                }
                pend_frame_ = null;
                last_frame_ms_ = curms;
            }
            else {
                Log.i(TAG, "Pended frame is still too early - waiting");
            }

        }
        
        handler_.postDelayed(
                    new Runnable() {
                        public void run() {
                            checkSendFrame();
                        }
                    }, RECHECK_MS);
    }



    public static class AndroidBmpUtil {

        private static final int BMP_WIDTH_OF_TIMES = 4;
        private static final int BYTE_PER_PIXEL = 3;

        /**
         * Android Bitmap Object to Window's v3 24bit Bmp Format File
         * @param orgBitmap
         * @param filePath
         * @return file saved result
         */
        public static ByteBuffer getbmp(Bitmap orgBitmap) throws IOException {
            long start = System.currentTimeMillis();
            if(orgBitmap == null){
                return null;
            }

            //image size
            int width = orgBitmap.getWidth();
            int height = orgBitmap.getHeight();

            //image dummy data size
            //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
            byte[] dummyBytesPerRow = null;
            boolean hasDummy = false;
            int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
            if(rowWidthInBytes%BMP_WIDTH_OF_TIMES>0){
                hasDummy=true;
                //the number of dummy bytes we need to add on each row
                dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES-(rowWidthInBytes%BMP_WIDTH_OF_TIMES))];
                //just fill an array with the dummy bytes we need to append at the end of each row
                for(int i = 0; i < dummyBytesPerRow.length; i++){
                    dummyBytesPerRow[i] = (byte)0xFF;
                }
            }

            //an array to receive the pixels from the source image
            int[] pixels = new int[width * height];

            //the number of bytes used in the file to store raw image data (excluding file headers)
            int imageSize = (rowWidthInBytes+(hasDummy?dummyBytesPerRow.length:0)) * height;
            //file headers size
            int imageDataOffset = 0x36;

            //final size of the file
            int fileSize = imageSize + imageDataOffset;

            //Android Bitmap Image Data
            orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
            ByteBuffer buffer = ByteBuffer.allocate(fileSize);

            /**
             * BITMAP FILE HEADER Write Start
             **/
            buffer.put((byte)0x42);
            buffer.put((byte)0x4D);

            //size
            buffer.put(writeInt(fileSize));

            //reserved
            buffer.put(writeShort((short)0));
            buffer.put(writeShort((short)0));

            //image data start offset
            buffer.put(writeInt(imageDataOffset));

            /** BITMAP FILE HEADER Write End */

            //*******************************************

            /** BITMAP INFO HEADER Write Start */
            //size
            buffer.put(writeInt(0x28));

            //width, height
            //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
            buffer.put(writeInt(width+(hasDummy?(dummyBytesPerRow.length==3?1:0):0)));
            buffer.put(writeInt(height));

            //planes
            buffer.put(writeShort((short)1));

            //bit count
            buffer.put(writeShort((short)24));

            //bit compression
            buffer.put(writeInt(0));

            //image data size
            buffer.put(writeInt(imageSize));

            //horizontal resolution in pixels per meter
            buffer.put(writeInt(0));

            //vertical resolution in pixels per meter (unreliable)
            buffer.put(writeInt(0));

            buffer.put(writeInt(0));

            buffer.put(writeInt(0));

            /** BITMAP INFO HEADER Write End */

            int row = height;
            int col = width;
            int startPosition = (row - 1) * col;
            int endPosition = row * col;
            while( row > 0 ){
                for(int i = startPosition; i < endPosition; i++ ){
                    buffer.put((byte)(pixels[i] & 0x000000FF));
                    buffer.put((byte)((pixels[i] & 0x0000FF00) >> 8));
                    buffer.put((byte)((pixels[i] & 0x00FF0000) >> 16));
                }
                if(hasDummy){
                    buffer.put(dummyBytesPerRow);
                }
                row--;
                endPosition = startPosition;
                startPosition = startPosition - col;
            }


            return buffer;
        }

        /**
         * Write integer to little-endian
         * @param value
         * @return
         * @throws IOException
         */
        private static byte[] writeInt(int value) throws IOException {
            byte[] b = new byte[4];

            b[0] = (byte)(value & 0x000000FF);
            b[1] = (byte)((value & 0x0000FF00) >> 8);
            b[2] = (byte)((value & 0x00FF0000) >> 16);
            b[3] = (byte)((value & 0xFF000000) >> 24);

            return b;
        }

        /**
         * Write short to little-endian byte array
         * @param value
         * @return
         * @throws IOException
         */
        private static byte[] writeShort(short value) throws IOException {
            byte[] b = new byte[2];

            b[0] = (byte)(value & 0x00FF);
            b[1] = (byte)((value & 0xFF00) >> 8);

            return b;
        }
    }


    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            Bitmap bitmap = null;

            ByteArrayOutputStream stream = null;

            try {
                image = imgreader_.acquireLatestImage();

                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Log.i(TAG, "Got frame: " + image.getWidth() + " / " + image.getHeight());
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width_;

                    //// create bitmap
                    bitmap = Bitmap.createBitmap(width_ + rowPadding / pixelStride,
                            height_, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    stream = new ByteArrayOutputStream();

                    Rect f = DeviceService.getInstance().screen_cap_rect;
                    if (f == null) {
                        image.close();
                        return;
                    }
                    int usewidth = Math.min(f.width(), bitmap.getWidth() - f.left);
                    int useheight = Math.min(f.height(), bitmap.getHeight() - f.top);

                    if ((useheight <= 0) || (usewidth <= 0))
                    {
                        image.close();
                        return;
                    }

                    Bitmap b2 = Bitmap.createBitmap(bitmap, f.left, f.top, usewidth, useheight);
                    Bitmap b3 = Bitmap.createScaledBitmap(b2, 220, 220, true);

                    b3.compress(Bitmap.CompressFormat.JPEG, 80, stream);

                    byte[] data = stream.toByteArray();

                    // FOR BMP version
                    //byte[] data = AndroidBmpUtil.getbmp(bitmap).array();

                    image.close();


                    // pushing frames doesn't work - the bluetooth sometimes gets too backed up
                    // so i switched to a pull model (we set the last frame, then when receiving
                    // a message from the glasses, we publish back the latest frame.

                    // I'm leaving in the code to pause between frames in case I ever want to switch
                    // back to a push method
                    long curms = System.currentTimeMillis();
                    //if ((MIN_FRAME_GAP_MS == 0) || ((curms - last_frame_ms_) >= MIN_FRAME_GAP_MS))
                    //{
                    if (listener_ != null)
                    {
                        Log.i(TAG, "SENDING FRAME");
                        listener_.onFrameData("data:image/jpeg;base64," + Base64.encodeToString(data, Base64.DEFAULT));
                    }
                    last_frame_ms_ = curms;
                    //}
                    //else
                    //{
                    //    Log.i(TAG, "PENDING FRAME FOR LATER");
                    //    pend_frame_ = "data:image/jpeg;base64," + Base64.encodeToString(data, Base64.DEFAULT);
                    //}

                    //if (listener_ != null)
                    //{
                    //    //listner_.onFrameData(data);
                    //    //listener_.onFrameData("data:image/x-ms-bmp;base64," + Base64.encodeToString(data, Base64.DEFAULT));
                    //    listener_.onFrameData("data:image/jpeg;base64," + Base64.encodeToString(data, Base64.DEFAULT));
                    //    //stream.toByteArray(), Base64.DEFAULT));
                    //}


                }


            } catch (Exception e) {
                Log.e(TAG, "ERROR: " + e.toString());
                e.printStackTrace();
            }

        }
    }


}
