package com.joyhonest.joyBlePrinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

//import androidx.annotation.RequiresApi;
//import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class joyBlePrinterManager {

    private final String TAG = "joyBlePrinterManager";
    private Context context;
    private BluetoothManager blManager;
    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> printerList;

    private Handler handlerDelay;
    private BluetoothLeScanner mScanner;
    volatile static joyBlePrinterManager singleton;

    UUID advServiceUUID = UUID.fromString("0000af30-0000-1000-8000-00805f9b34fb");
    String sServiceUUID = "0000af30-0000-1000-8000-00805f9b34fb";

    private joyBlePrinterManager() {
        if (singleton != null) {
            throw new IllegalStateException("Singleton instance already created!");
        }
    }

    public void setContext(Context context) {
        this.context = context.getApplicationContext();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("","no ble");

        }
        blManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (blManager != null) {
            mBluetoothAdapter = blManager.getAdapter();
            if(mBluetoothAdapter.isEnabled()) {
                if (mBluetoothAdapter != null) {
                    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
            }
        }
        printerList = new ArrayList<>();
        handlerDelay = new Handler(context.getMainLooper());
    }

    public boolean isBleSupported() {
        return this.context != null && context.getApplicationContext().
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }


    private boolean findDevice(BluetoothDevice printer) {
        for (BluetoothDevice printer1 : printerList) {
            if (printer1.getAddress().equalsIgnoreCase(printer.getAddress())) {
                return true;
            }
        }
        return false;
    }

    joyBlePrinterClient.joyBlePrinter_ScanningCallback scanningCallback = null;

    private Handler mainHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            //
            if(scanningCallback!=null)
            {
                joyBlePrinter printer = (joyBlePrinter)msg.obj;
                scanningCallback.onFindPrinter(printer);
            }
        }
    };
    ScanCallback mScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);

            if (scanningCallback != null) {
                BluetoothDevice device = result.getDevice();// 获取BLE设备信息
                ScanRecord scanRecordA = result.getScanRecord();
                if(scanRecordA!=null) {
                    byte[] scanRecord = scanRecordA.getBytes();
                    //21-26

                }
                if (!findDevice(device)) {
                    printerList.add(device);
                    joyBlePrinter printer = new joyBlePrinter(context, device);
                    //scanningCallback.onFindPrinter(printer);
                    Message msg = Message.obtain();
                    msg.obj = printer;
                    mainHandler.sendMessage(msg);
                }
            }
        }
    };

    public static joyBlePrinterManager getInstance(Context context) {
        if (singleton == null) {
            synchronized (joyBlePrinterManager.class) { // 加锁
                if (singleton == null) {
                    singleton = new joyBlePrinterManager();
                    if (singleton != null) {
                        singleton.setContext(context);
                    }
                }
            }
        }
        return singleton;
    }


    boolean bScanning = false;

    public int joyBlePrinterStartScan(joyBlePrinterClient.joyBlePrinter_ScanningCallback callback, int nSec) {
        if (bScanning)
            return 0;
        int re = ScanBluePrinter();
        if (re == 0) {
            this.scanningCallback = callback;
            if (nSec > 0) {
                handlerDelay.removeCallbacksAndMessages(null);
                if (bScanning) {
                    handlerDelay.postDelayed(this::joyBlePrinterStopScaning, 1000L * nSec);
                }
            }
        }
        return re;
    }



    public int joyBlePrinterStopScaning() {
        if (!bScanning)
            return -3;
        if(context == null) {
            bScanning = false;
            return -1;
        }
        try {


            int  daa = 0;
            //if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                daa = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN);
            }
            if(daa == 0)
            {
                bScanning = false;
                handlerDelay.removeCallbacksAndMessages(null);
                if (mScanner != null) {
                    mScanner.stopScan(mScanCallback);
                    SystemClock.sleep(150);
                    scanningCallback = null;
                }
                Log.e(TAG, "Stop Scanning");
                return 0;
            }
        }
        catch (Exception ignored)
        {

        }
        return -2;



    }


    private int ScanBluePrinter() {
        if(bScanning)
            return 0;
        printerList.clear();
        UUID[] da = new UUID[1];
        int nResult = -1;
        da[0] = advServiceUUID;
        int daa = PackageManager.PERMISSION_GRANTED;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            daa = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN);
        }
        if (daa == PackageManager.PERMISSION_GRANTED) {
            bScanning = true;
            if(mScanner!=null)
            {
                List<ScanFilter> filters = new ArrayList<>();
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(sServiceUUID)).build();
                filters.add(filter);

                ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                mScanner.startScan(filters, scanSettings, mScanCallback);
                nResult = 0;
            }
            else
            {

                nResult = -1;
            }


        }
        else
        {
            nResult = -2;
            Log.e("joyBlePrinterManager","Permission error");
        }
        return nResult;

    }




}
