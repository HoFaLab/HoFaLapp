package de.hofalab.hofalapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.LinkedList;

public class DrawControlActivity extends AppCompatActivity {
    private static final String LOG_TAG = "HOFALAPP CNC DRAW";

    private ExecuteTask task = null;
    private Button executeButton;
    private ImageView imageView;
    private LinkedList<Stroke> strokeStackPending;
    private Stroke currentStroke = null;

    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_control);

        Button undo = findViewById(R.id.button_undo);
        executeButton = findViewById(R.id.button_send_gcode2);

        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                execute();
            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undo();
            }
        });

        strokeStackPending = new LinkedList<Stroke>();
        imageView = findViewById(R.id.draw_area);

        imageView.post(new Runnable() {
            @Override
            public void run() {
                Vector3 wrkspcSize = CNCDevice.getWrkspcSize();
                double wrkspWidth = wrkspcSize.get(CNCDevice.getHorizontalAxis());
                double wrkspHeight = wrkspcSize.get(CNCDevice.getVerticalAxis());

                int w = imageView.getWidth();
                int h = imageView.getHeight();
                int h_adapted = (int) Math.round(wrkspHeight/wrkspWidth * w);
                int w_adapted = (int) Math.round(wrkspWidth/wrkspHeight * h);
                if(h_adapted <= h)
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(w, h_adapted));

                else
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(w_adapted, h));
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        computeImage();
                    }
                });
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View view, MotionEvent ev) {
                MotionEvent.PointerCoords outPointerCoords = new MotionEvent.PointerCoords();
                ev.getPointerCoords(0, outPointerCoords);

                if(ev.getAction() == MotionEvent.ACTION_DOWN){
                    if(currentStroke != null) return false;
                    currentStroke = new Stroke(convert(outPointerCoords));
                    return true;
                } else if(ev.getAction() == MotionEvent.ACTION_MOVE) {
                    if(currentStroke == null) return false;
                    Vector3 newPos = convert(outPointerCoords);
                    Vector3 step = newPos.copy();
                    step.sub(currentStroke.data.lastElement());
                    if(step.lengthSqr() > 0.1) {
                        draw(currentStroke.data.lastElement(), newPos);
                        currentStroke.data.add(newPos);
                        return true;
                    } else return false;
                } else if(ev.getAction() == MotionEvent.ACTION_UP){
                    if(currentStroke == null) return false;
                    Vector3 newPos = convert(outPointerCoords);
                    draw(currentStroke.data.lastElement(), newPos);
                    currentStroke.data.add(newPos);
                    strokeStackPending.push(currentStroke);
                    currentStroke = null;
                    return true;
                }
                return false;
            }
        });
    }

    // To (re-)compute the image shown in the imageView based on CNCDevice.getStrokesDone
    private void computeImage(){
        int axis = CNCDevice.getHorizontalAxis();
        double strokewidth = Stroke.width / CNCDevice.getWrkspcSize().get(axis) * imageView.getWidth();

        bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setAlpha(25);
        paint.setStrokeWidth((int)Math.ceil(strokewidth));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        LinkedList<Stroke> strokes = CNCDevice.getStrokesDone();
        for(Stroke s : strokes){
            for(int i=0; i < s.data.size()-1; i++){
                Pair<Integer,Integer> start = convert(s.data.get(i));
                Pair<Integer,Integer> end = convert(s.data.get(i+1));
                canvas.drawLine(start.first, start.second, end.first, end.second, paint);
            }
        }
        imageView.setImageBitmap(bitmap);

        paint.setAlpha(255);
    }

    private void draw(Vector3 from, Vector3 to){
        Pair<Integer,Integer> start = convert(from);
        Pair<Integer,Integer> end = convert(to);
        canvas.drawLine(start.first, start.second, end.first, end.second, paint);
        imageView.setImageBitmap(bitmap);
    }

    private Vector3 convert(MotionEvent.PointerCoords in){
        float relX = in.x / imageView.getWidth();
        float relY = 1 - in.y / imageView.getHeight();
        Vector3 res = CNCDevice.getPos().copy();
        int h = CNCDevice.getHorizontalAxis();
        int v = CNCDevice.getVerticalAxis();
        res.set(h, CNCDevice.wrkspcMin.get(h) + relX * CNCDevice.getWrkspcSize().get(h));
        res.set(v, CNCDevice.wrkspcMin.get(v) + relY * CNCDevice.getWrkspcSize().get(v));
        return res;
    }

    private Pair<Integer,Integer> convert(Vector3 in){
        int h = CNCDevice.getHorizontalAxis();
        int v = CNCDevice.getVerticalAxis();
        double relX = (in.get(h) - CNCDevice.wrkspcMin.get(h)) / CNCDevice.getWrkspcSize().get(h);
        double relY = (in.get(v) - CNCDevice.wrkspcMin.get(v)) / CNCDevice.getWrkspcSize().get(v);
        int x = (int) Math.round(relX * imageView.getWidth());
        int y = (int) Math.round((1-relY) * imageView.getHeight());
        return new Pair<Integer,Integer>(x,y);
    }

    private void undo(){
        currentStroke = null;
        if(strokeStackPending.size() > 0) strokeStackPending.pop();
        repaint();
    }

    private void repaint(){
        computeImage();
        for(Stroke s : strokeStackPending){
            for(int i=0; i < s.data.size()-1; i++){
                Pair<Integer,Integer> start = convert(s.data.get(i));
                Pair<Integer,Integer> end = convert(s.data.get(i+1));
                canvas.drawLine(start.first, start.second, end.first, end.second, paint);
            }
        }
        imageView.setImageBitmap(bitmap);
    }

    private void execute(){
        if (task != null) {
            return;
        }

        task = new ExecuteTask();
        task.execute((Void) null);
    }

    public class ExecuteTask extends AsyncTask<Void, Void, Boolean> {
        ExecuteTask() {
            super();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    executeButton.setText(getString(R.string.execute_busy));
                }
            });

            try {
                for(Stroke s : strokeStackPending) {
                    execute("M5");
                    execute("G0Z2");
                    Vector3 target = s.data.firstElement().copy();
                    target.set(2,2);
                    execute(target.toString());

                    execute("M3");
                    execute("G1F800");
                    for(int i=0; i < s.data.size(); i++){
                        execute(s.data.get(i).toString());
                    }
                    execute("M5");
                    execute("G0Z2");
                    CNCDevice.getStrokesDone().add(s);
                }
                Vector3 pos = strokeStackPending.getLast().data.lastElement().copy();
                pos.set(2,2);
                CNCDevice.setPos(pos);
                strokeStackPending.clear();
                return true;
            }
            catch( IOException e){
                Log.e(LOG_TAG, e.toString());
            }
            return false;
        }

        private void execute(String msg) throws IOException{
            byte[] data = msg.getBytes("UTF-8");
            SocketHandler.getSocket().getOutputStream().write(data);
            int res = SocketHandler.getSocket().getInputStream().read();
            if(res != 1) throw new IOException("Unexpected answer");
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            task = null;
            if (success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        executeButton.setText(getString(R.string.action_execute));
                        repaint();
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        executeButton.setText(getString(R.string.failed));
                    }
                });
                try {
                    SocketHandler.getSocket().close();
                } catch( IOException e){
                    Log.e(LOG_TAG, e.toString());
                }
                SocketHandler.setSocket(null);
                DrawControlActivity.this.finish();
            }
        }

        @Override
        protected void onCancelled() {
            task = null;
            DrawControlActivity.this.finish();
        }
    }

}
