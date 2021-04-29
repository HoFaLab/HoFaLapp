package de.hofalab.hofalapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.util.Base64;
import java.util.Arrays;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

/**
 * A login screen that offers login to HoFaLab CNC device via one-time QR code
 */
public class CNCLoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = "HOFALAPP CNC LOGIN";
    private static final String STATE_SCAN = "STATE_SCAN";
    private static final String STATE_BITMAP = "STATE_BITMAP";
    private static final String STATE_SCAN_BUTTON = "STATE_SCAN_BUTTON";
    private static final String STATE_CONNECT_BUTTON = "STATE_CONNECT_BUTTON";
    private static final int PHOTO_REQUEST = 10;

    /* UI elements  */
    private EditText usernameView;
    private Button takeImageButton;
    private Button scanButton;
    private Button connectButton;
    private ImageView imageView;

    private Bitmap bitmap;
    private BarcodeDetector detector;
    byte[] scanResult = null;
    private ConnectTask connectTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cnclogin);
        // Set up the login form.
        usernameView = findViewById(R.id.username);

        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        takeImageButton = findViewById(R.id.take_image_button);
        scanButton = findViewById(R.id.scan_button);
        connectButton = findViewById(R.id.conncect_button);
        imageView = findViewById(R.id.image);

        takeImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        scanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                scanQR();
            }
        });

        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_SCAN_BUTTON, scanButton.getText().toString());
        savedInstanceState.putString(STATE_CONNECT_BUTTON, connectButton.getText().toString());
        savedInstanceState.putByteArray(STATE_SCAN, scanResult);
        savedInstanceState.putParcelable(STATE_BITMAP,bitmap);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        scanButton.setText( savedInstanceState.getString(STATE_SCAN_BUTTON) );
        connectButton.setText( savedInstanceState.getString(STATE_CONNECT_BUTTON) );
        scanResult = savedInstanceState.getByteArray(STATE_SCAN);
        bitmap = savedInstanceState.getParcelable(STATE_BITMAP);
        imageView.setImageBitmap(bitmap);
    }

    private void takePicture() {
        takeImageButton.setError(null);

        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, PHOTO_REQUEST);
            } else {
                takeImageButton.setError(getString(R.string.error_no_camera_app));
            }
        } else {
            takeImageButton.setError(getString(R.string.error_no_camera));
        }
    }

    private void scanQR() {
        try {
            if (detector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Barcode> barcodes = detector.detect(frame);
                for (int index = 0; index < barcodes.size(); index++) {
                    Barcode code = barcodes.valueAt(index);
                    try {
                        byte[] decodedBytes = Base64.decode(code.rawValue, Base64.DEFAULT);
                        if(decodedBytes.length != 14) continue;
                        scanResult = decodedBytes;
                        scanButton.setText(getString(R.string.action_scan_qr) + getString(R.string.success));
                        connectButton.setText(getString(R.string.action_connect_to_CNC));
                        return;
                    } catch (Exception e) {
                        Log.d(LOG_TAG, e.toString());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        scanButton.setText(getString(R.string.action_scan_qr) + getString(R.string.failed));
    }

    private void connect(){
        if (SocketHandler.getSocket() != null && SocketHandler.getSocket().isConnected()){
            Intent intent = new Intent(CNCLoginActivity.this, RemoteControlActivity.class);
            startActivity(intent);
            return;
        }

        if (connectTask != null || scanResult == null) {
            return;
        }

        // Reset errors.
        usernameView.setError(null);
        // Store values at the time of the login attempt.
        String username = usernameView.getText().toString();
        if(TextUtils.isEmpty(username)) {
            usernameView.setError(getString(R.string.error_field_required));
            usernameView.requestFocus();
            return;
        }

        connectTask = new ConnectTask(username, scanResult);
        connectTask.execute((Void) null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            scanButton.setText(getString(R.string.action_scan_qr));
        }
    }

    public class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private final String username;
        private final byte[] data;
        private Socket sock;

        ConnectTask(String username, byte[] data) {
            this.username = username;
            this.data = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if(sock!=null) sock.close();
                byte[] ip = {data[0], data[1], data[2], data[3]};
                InetAddress server = InetAddress.getByAddress(ip);
                int port = data[4] + data[5]*256;
                byte[] token = {data[6], data[7], data[8], data[9],
                        data[10], data[11], data[12], data[13]};
                sock = new Socket();
                sock.connect(new InetSocketAddress(server, port), 5000);
                sock.setSoTimeout(30000);
                sock.getOutputStream().write(token);
                byte[] buf = new byte[3];
                int number_bytes_read = sock.getInputStream().read(buf);
                if(number_bytes_read == 3 && Arrays.equals(buf,"ACK".getBytes("UTF-8"))){
                    sock.getOutputStream().write(username.getBytes("UTF-8"),0, Math.min(64, username.length()));
                    SocketHandler.setSocket(sock);
                    return true;
                }
                if(sock!=null) sock.close();
            }
            catch( IOException e){
                Log.e(LOG_TAG, e.toString());
            }
            SocketHandler.setSocket(null);
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            connectTask = null;

            if (success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setText(getString(R.string.action_connect_to_CNC) + getString(R.string.success));
                    }
                });
                Intent intent = new Intent(CNCLoginActivity.this, RemoteControlActivity.class);
                startActivity(intent);
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setText(getString(R.string.action_connect_to_CNC) + getString(R.string.failed));
                    }
                });
            }
        }

        @Override
        protected void onCancelled() {
            connectTask = null;
            try {
                if (sock != null) sock.close();
            } catch( IOException e){
                Log.d(LOG_TAG, e.toString());
            }
        }
    }
}



