package de.hofalab.hofalapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.io.IOException;

public class RemoteControlActivity extends AppCompatActivity {
    private static final String LOG_TAG = "HOFALAPP CNC REMOTE";

    private EditText gcodeView;
    private Button executeGCodeButton;
    ToggleButton toggleSpindle;
    private GCodeTask gcodeTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);

        gcodeView = findViewById(R.id.enterGcode);
        executeGCodeButton = findViewById(R.id.button_send_gcode);

        gcodeView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendGCode();
                }
                return false;
            }
        });

        executeGCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendGCode();
            }
        });

        toggleSpindle = findViewById(R.id.toggle_spindle);
        toggleSpindle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    sendGCode("M3");
                }
                else
                {
                    sendGCode("M5");
                }
            }
        });

        Spinner spinnerStepWidth = findViewById(R.id.spinner_step_width);
        spinnerStepWidth.setSelection(1);
        spinnerStepWidth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if(parent.getItemAtPosition(pos).toString().equals("0.1 mm")) CNCDevice.setStep(0.1);
                else if(parent.getItemAtPosition(pos).toString().equals("1 mm")) CNCDevice.setStep(1);
                else if(parent.getItemAtPosition(pos).toString().equals("1 cm")) CNCDevice.setStep(10);
                else if(parent.getItemAtPosition(pos).toString().equals("10 cm")) CNCDevice.setStep(100);
                else Log.e(LOG_TAG, "Step width " + parent.getItemAtPosition(pos).toString() + " not implemented");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner spinnerCoordPlane = findViewById(R.id.spinner_coord_plane);
        spinnerCoordPlane.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if(parent.getItemAtPosition(pos).toString().equals("XY")){
                    CNCDevice.setHorizontalAxis(0); CNCDevice.setVerticalAxis(1);
                } else if(parent.getItemAtPosition(pos).toString().equals("XZ")){
                    CNCDevice.setHorizontalAxis(0); CNCDevice.setVerticalAxis(2);
                } else if(parent.getItemAtPosition(pos).toString().equals("YZ")){
                    CNCDevice.setHorizontalAxis(1); CNCDevice.setVerticalAxis(2);
                } else Log.e(LOG_TAG, "Coord plane " + parent.getItemAtPosition(pos).toString() + " not implemented");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        findViewById(R.id.button_NW).setOnClickListener(new DirButton(-1, 1));
        findViewById(R.id.button_N).setOnClickListener( new DirButton( 0, 1));
        findViewById(R.id.button_NE).setOnClickListener(new DirButton( 1, 1));
        findViewById(R.id.button_W).setOnClickListener( new DirButton(-1, 0));
        findViewById(R.id.button_E).setOnClickListener( new DirButton( 1, 0));
        findViewById(R.id.button_SW).setOnClickListener(new DirButton(-1,-1));
        findViewById(R.id.button_S).setOnClickListener( new DirButton( 0,-1));
        findViewById(R.id.button_SE).setOnClickListener(new DirButton( 1,-1));

        Button drawModeButton = findViewById(R.id.button_draw_mode);
        drawModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RemoteControlActivity.this, DrawControlActivity.class);
                startActivity(intent);
            }
        });
    }

    private class DirButton implements View.OnClickListener{
        private int horizontalPart, verticalPart;

        DirButton(int horizontalPart, int verticalPart){
            this.horizontalPart = horizontalPart;
            this.verticalPart = verticalPart;
        }

        @Override
        public void onClick(View view) {
            if (gcodeTask != null) return;
            Stroke s = new Stroke(CNCDevice.getPos().copy());
            Vector3 stepV = new Vector3();
            stepV.set(CNCDevice.getHorizontalAxis(),   CNCDevice.getStep() * horizontalPart);
            stepV.set(CNCDevice.getVerticalAxis(),    CNCDevice.getStep() * verticalPart);
            CNCDevice.getPos().add(stepV);
            CNCDevice.getPos().clamp(CNCDevice.wrkspcMin, CNCDevice.wrkspcMax);
            if(toggleSpindle.isChecked() && !CNCDevice.getPos().equals(s.data.get(0))) {
                s.data.add(CNCDevice.getPos().copy());
                CNCDevice.getStrokesDone().add(s);
            }
            sendGCode(CNCDevice.getPos().toString());
        }
    }

    private void sendGCode(){
        sendGCode(gcodeView.getText().toString());
    }

    private void sendGCode(String line){
        if (gcodeTask != null ) return;
        if (line.length() == 0) return;
        try {
            gcodeTask = new RemoteControlActivity.GCodeTask(line.getBytes("UTF-8"));
            gcodeTask.execute((Void) null);
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    public class GCodeTask extends AsyncTask<Void, Void, Boolean> {
        private final byte[] data;

        GCodeTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    executeGCodeButton.setText(getString(R.string.gcode_busy));
                }
            });

            try {
                SocketHandler.getSocket().getOutputStream().write(data);
                int res = SocketHandler.getSocket().getInputStream().read();
                if(res == 1) return true;
                else throw new IOException("Unexpected answer");
            }
            catch( IOException e){
                Log.e(LOG_TAG, e.toString());
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            gcodeTask = null;
            if (success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        executeGCodeButton.setText(getString(R.string.action_send_gcode));
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        executeGCodeButton.setText(getString(R.string.failed));
                    }
                });
                try {
                    SocketHandler.getSocket().close();
                } catch( IOException e){
                    Log.e(LOG_TAG, e.toString());
                }
                SocketHandler.setSocket(null);
                RemoteControlActivity.this.finish();
            }
        }

        @Override
        protected void onCancelled() {
            gcodeTask = null;
            RemoteControlActivity.this.finish();
        }
    }

}

/*
                print('G91')
#print('F1200')
#scale = 0.2
#while True:
#   data = s.recv(1024)
#   try:
#       jdata = json.loads(data)
#       #jdata.rotationVector.value
#       xpos = -jdata['gravity']['value'][0] * scale
#       ypos = -jdata['gravity']['value'][1] * scale
#       print('G1 X' + str(xpos) + ' Y'+str(ypos))
#
#   except json.decoder.JSONDecodeError:
#       pass
*/