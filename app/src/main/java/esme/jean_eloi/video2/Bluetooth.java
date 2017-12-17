package esme.jean_eloi.video2;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity {
    public static final UUID UUID_POTAR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//client configuration characteristique

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mScanning;

    private static final int RQS_ENABLE_BLUETOOTH = 1;

    private Button mFragmentButton;
    private Button mScanButton;
    private ListView listViewLE;
    private List<BluetoothDevice> listBluetoothDevice;
    private List<String> listStringBluetoothDevice;
    private ListAdapter adapterLeScanResult;
    private BroadcastReceiver act2InitReceiver;

    private BluetoothGatt mGatt;
    private ProgressDialog mProgressDialog;
    private static final long SCAN_PERIOD = 5000;

    //    private List<BluetoothGattService> mServices;
    private BluetoothGattCharacteristic mPotarCharacteristic;

    private TextView mPotarValueTextView;
    private static int value_potar = 0;
    private Handler mHandler = new Handler();
    private Handler mHandler2 = new Handler();

    private Intent myIntent;
    public static final String SOME_KEY = "some_key";

    Runnable mUIrunnable = new Runnable() {
        @Override
        public void run() {
            mPotarValueTextView.setText(String.valueOf(value_potar));
//            value_potar += 1;
//            mPotarValueTextView.setText(String.valueOf(value_potar));

            mHandler2.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        //        myIntent = new Intent(getApplicationContext(), MapActivity.class);

        // Create the progress dialog and set a custom message
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Scanning in progress...");

        // Permission required to launch a BLE scan (popup that ask for permission to have localisation)
        int MY_PERMISSION_REQUEST_CONSTANT = 2;
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_CONSTANT);

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH LOW ENERGY not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish();
        } /*else {
            Toast.makeText(this,
                    "BLUETOOTH_LE is supported in this device!",
                    Toast.LENGTH_SHORT).show();
        } */

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Scan button used to detect nearby bluetooth devices
        mScanButton = (Button) findViewById(R.id.scan_button);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        // Button that enables bluetooth when clicked on
        Button mEnableButton = (Button) findViewById(R.id.activate_button);
        mEnableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
                } else {
                    Toast.makeText(Bluetooth.this, "Bluetooth is already enabled", Toast.LENGTH_LONG).show();
                }
            }
        });


        // Button that stops the scan
        Button mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
                Toast.makeText(Bluetooth.this, "Scan stopped", Toast.LENGTH_LONG).show();
                if (mUIrunnable != null && mHandler != null) {
                    mHandler2.removeCallbacks(mUIrunnable);
                }
            }
        });

        //Button that disconnects the connection with the device
        Button mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGatt != null) {
                    mGatt.disconnect();
                }
            }
        });

        mPotarValueTextView = (TextView) findViewById(R.id.value_potar);

        // ListView and adapter used to display the list of detected devices
        listViewLE = (ListView) findViewById(R.id.discovery_list);
        listBluetoothDevice = new ArrayList<>();
        listStringBluetoothDevice = new ArrayList<>();
        adapterLeScanResult = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, listStringBluetoothDevice);
        listViewLE.setAdapter(adapterLeScanResult);
        listViewLE.setOnItemClickListener(scanResultOnItemClickListener);

    }

    // Implementation of onItemClick listener for the adapter (list of devices)
    AdapterView.OnItemClickListener scanResultOnItemClickListener =
            new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Get the bluetooth device according to the position in the list
//                    final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                    Log.e("onItemClick", "Position = " + position);
                    final BluetoothDevice device = listBluetoothDevice.get(position);
                    if (mGatt == null) {
                        connectToDevice(device);
                    }

/*                    String msg = device.getAddress() + "\n"
                            + getBTDeviceType(device);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(device.getName())
                            .setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();*/

                }
            };


    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
        }

    }

    private String getBTDeviceType(BluetoothDevice d) {
        String type = "";

        switch (d.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }

        return type;
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            super.onDestroy();
        } else {
            mGatt.close();
            mGatt = null;
            mHandler.removeCallbacks(mUIrunnable);
            super.onDestroy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Method invoked when keyUp is clicked. Used to warn the user that he is about to close the application
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to leave the application ? ");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        return true;
    }

    // Initialize the Bluetooth adapter and LEScanner used for basic LE functions like scan
    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    //
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Show the progress dialog while scan is in progress
            mProgressDialog.show();
            // Clear the list of devices if enable is true
            listBluetoothDevice.clear();
            listStringBluetoothDevice.clear();
            listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                    listViewLE.invalidateViews();

                    Toast.makeText(Bluetooth.this,
                            "Scan stopped",
                            Toast.LENGTH_LONG).show();

                    mScanning = false;
                    mScanButton.setEnabled(true);
                    mProgressDialog.dismiss();
                }
            }, SCAN_PERIOD);

            mBluetoothLeScanner.startScan(scanCallback);
            mScanning = true;
            mScanButton.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;
            mScanButton.setEnabled(true);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                addBluetoothDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(Bluetooth.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
        }
    };

    private void addBluetoothDevice(BluetoothDevice device) {
//            Log.e("MainActivity", "name = " + device.getName() + "\naddress = " + device.getAddress());
        if (!listBluetoothDevice.contains(device)) {
            listBluetoothDevice.add(device);
            listStringBluetoothDevice.add(device.getName() + "\n"
                    + device.getAddress());
            listViewLE.invalidateViews();
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallBack);
            mHandler2.post(mUIrunnable);

//            startActivity(myIntent);

        }/* else {
            mGatt.disconnect();
        }*/
    }

    private final BluetoothGattCallback gattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e("gattCallBack", "STATE CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallBack", "STATE DISCONNECTED");
                    mGatt.close();
                    mGatt = null;
                    break;
                default:
                    Log.e("getCallBack", "STATE OTHER");

            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> mServices = gatt.getServices();
            Log.e("MainActivity", "onServicesDiscovered");

            for (int i = 0; i < mServices.size(); i++) {
                Log.e("Service", i + " : " + mServices.get(i).getUuid().toString());
            }

            for (BluetoothGattCharacteristic gattCharacteristic : mServices.get(2).getCharacteristics()) {
                Log.e("Characteristics", gattCharacteristic.getUuid().toString());
            }

//            gatt.readCharacteristic(mServices.get(2).getCharacteristics().get(0));
            mPotarCharacteristic = mServices.get(2).getCharacteristics().get(0);

            setNotifications();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.e("onCharacteristicRead", "");


            // Integer Valuracteristic.getValue()[0] & 0xffff;
            // Log.e("Value = ", String.valueOf(Value + 5000));


            byte[] Value = characteristic.getValue();
            byte flag = Value[0];
            Log.i("Value =", "Characteristic: " + flag);


//            gatt.disconnect();
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            Integer Value = characteristic.getValue()[0] & 0xff;
            value_potar = characteristic.getValue()[0] /*& 0xffff*/;
            Log.e("Value = ", String.valueOf(value_potar));

//            mPotarValueTextView.setText(String.valueOf(Value));
        }

    };


    private void setNotifications() {
        mGatt.setCharacteristicNotification(mPotarCharacteristic, true);

        BluetoothGattDescriptor descriptor = mPotarCharacteristic.getDescriptor(UUID_POTAR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
    }

   /* private void readCharacteristic() {
        if (mGatt != null && mPotarCharacteristic != null) {
            mGatt.readCharacteristic(mPotarCharacteristic);
        }
    }*/

    public static int getValuePotar() {
        return value_potar;
    }

    // public static int ceudruc(){
    //return 20;
    //}



    public void Go(View v) {
        //on creer une nouvelle intent on definit la class de depart ici this et la class d'arrivé ici SecondActivite
        Intent intent = new Intent(this, Video.class);
        //on lance l'intent, cela a pour effet de stoper l'activité courante et lancer une autre activite ici SecondActivite
        startActivity(intent);


    }

}