package ru.amcp.socialdistance;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adapter;
    private BluetoothGatt mBluetoothGatt;
    private LocationManager locationManager;
    private TextView latitude;
    private TextView longitude;
    private Looper looper = null;
    private String coll = null;
    Location mlocation;
    private Criteria criteria;
    private Connection mConnect = null;
    private Socket mSocket = null;
    private String HOST = "192.168.88.237";
    private int PORT = 12345;
    private boolean prevCollision = false;
    private boolean curCollision = false;
    private String LOG_TAG = "SOCKET";
    private String TAG = "SOCKET";
    private Date curDate;
    private long time;

    UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean result = gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e(TAG, "STATE_OTHER");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            try {
                byte[] charValue = characteristic.getValue();
                byte flag = charValue[0];
                Log.i(TAG, "Characteristic: " + flag);
                Toast toast = Toast.makeText(getApplicationContext(), "Characteristic: " + flag, Toast.LENGTH_LONG);
                toast.show();
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
        }
    };
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                mlocation = location;
                Log.d("Location Changes", location.toString());
                latitude.setText(String.valueOf(location.getLatitude()));
                longitude.setText(String.valueOf(location.getLongitude()));
            } catch (Exception e) {
                latitude.setText(e.getMessage());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("Status Changed", String.valueOf(status));
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d("Provider Enabled", provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d("Provider Disabled", provider);
        }
    };

    public String readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (adapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        return String.valueOf(characteristic.getStringValue(0));
    }

    public void connectToDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
        try {
            gattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS,
                    BluetoothGatt.STATE_CONNECTED);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void checkLoc() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }
        BluetoothGattService serv;
        try {
            locationManager.requestSingleUpdate(new Criteria(), locationListener, looper);
            serv = mBluetoothGatt.getService(SERVICE_UUID);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Services: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            coll = readCharacteristic(serv.getCharacteristics().get(0));
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Services: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (coll != null) {
            time = 0;
            curCollision = coll.equals("Collision");
            if (prevCollision != curCollision) {
                if (curCollision) {
                    curDate = new Date();
                } else {
                    time = ((new Date()).getTime() - curDate.getTime()) / 1000;
                }
                // Создание подключения
                if(!curCollision){
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                String currentDateandTime = sdf.format(curDate);
                                Socket socket = new Socket(HOST, PORT);
                                if (mlocation != null) {
                                    String value = currentDateandTime + "," + mlocation.getLongitude() + "," + mlocation.getLatitude() + "," + time;
                                    socket.getOutputStream().write(value.getBytes());
                                    socket.getOutputStream().flush();
                                }
                                socket.close();
                            } catch (Exception e) {
                                Toast toast = Toast.makeText(getApplicationContext(), "Services: " + e.toString(), Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                    });
                    thread.start();
                }
            }
            prevCollision = curCollision;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.btnGetLocation);
        latitude = (TextView) findViewById(R.id.textView4);
        longitude = (TextView) findViewById(R.id.textView5);

        adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (mBluetoothGatt == null) {
                    BluetoothDevice device = result.getDevice();
                    connectToDevice(device);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
            }

            @Override
            public void onScanFailed(int errorCode) {
            }
        };

        List<ScanFilter> filters = new ArrayList<>();
        String[] peripheralAddresses = new String[]{"D8:A0:1D:5E:18:12"};
        if (peripheralAddresses != null) {
            filters = new ArrayList<>();
            for (String address : peripheralAddresses) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setDeviceAddress(address)
                        .build();
                filters.add(filter);
            }
        }
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();
        if (scanner != null) {
            scanner.startScan(filters, scanSettings, scanCallback);
            Log.d(TAG, "scan started");
        } else {
            Log.e(TAG, "could not get scanner object");
        }

        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.scheduleAtFixedRate(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        checkLoc();
                    }
                });
            }
        }, 10, 1, TimeUnit.SECONDS);
    }

}
