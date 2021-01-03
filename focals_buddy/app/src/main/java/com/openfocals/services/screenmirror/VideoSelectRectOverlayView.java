package com.openfocals.services.screenmirror;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.openfocals.services.DeviceService;

import static android.content.Context.WINDOW_SERVICE;

public class VideoSelectRectOverlayView extends View {
    private static final String TAG = "FOCALS_VIDEO_OVERLAY";
    private Paint paint;
    Context context_;

    public WindowManager.LayoutParams params;

    public VideoSelectRectOverlayView(Context context) {
        super(context);

        context_ = context;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(10);
        paint.setARGB(255, 255, 0, 0);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.parseColor("#66CCFFFF"));
        canvas.drawRect(0, 0, params.width, params.height, paint);
    }


    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private int initialWidth;
    private int initialHeight;
    boolean in_size_mode = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;

                initialWidth = params.width;
                initialHeight = params.height;

                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                in_size_mode = ((initialTouchX > params.width - 20) && (initialTouchY > params.height - 20));
                return true;
            case MotionEvent.ACTION_UP:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (in_size_mode) {
                    params.width = Math.max(220, initialWidth + (int) (event.getRawX() - initialTouchX));
                    params.height = Math.max(220, initialHeight + (int) (event.getRawY() - initialTouchY));
                } else {
                    params.x = Math.max(0, initialX + (int) (event.getRawX() - initialTouchX));
                    params.y = Math.max(0, initialY + (int) (event.getRawY() - initialTouchY));
                }

                Log.i(TAG, "Updated params: " + params.x + " : " + params.y + " : " + params.width + " : " + params.height);

                Rect f = new Rect(params.x, params.y, params.x + params.width, params.y + params.height);
                DeviceService.getInstance().screen_cap_rect = f;
                ((WindowManager) context_.getSystemService(WINDOW_SERVICE)).updateViewLayout(this, params);
                return true;

        }
        return false;
    }

}
