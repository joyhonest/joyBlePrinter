package com.joyhonest.joyBlePrinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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

    //private final String TAG = "joyBlePrinterManager";
    private Context context;
    private BluetoothManager blManager;
    private BluetoothAdapter mBluetoothAdapter;
    private List<joyBlePrinter> printerList;

    private Handler handlerDelay;
    private Handler handlerDelayScanning;
    private BluetoothLeScanner mScanner;
    @SuppressLint("StaticFieldLeak")
    volatile static joyBlePrinterManager singleton;


    UUID advServiceUUID = UUID.fromString("0000af30-0000-1000-8000-00805f9b34fb");
    String sAdvServiceUUID = "0000af30-0000-1000-8000-00805f9b34fb";

    private joyBlePrinterManager() {
        if (singleton != null) {
            throw new IllegalStateException("Singleton instance already created!");
        }
    }

    public void setContext(Context context) {
        this.context = context.getApplicationContext();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if(joyBlePrinter.bLog) {
                Log.d("", "no ble");
            }

        }
        printerList = new ArrayList<>();
        handlerDelay = new Handler(context.getMainLooper());
        handlerDelayScanning = new Handler(context.getMainLooper());
        blManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (blManager != null) {
            mBluetoothAdapter = blManager.getAdapter();
            if(mBluetoothAdapter.isEnabled()) {
                if (mBluetoothAdapter != null) {
                    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
            }
            @SuppressLint("MissingPermission")
            List<BluetoothDevice> connectedDevices = blManager.getConnectedDevices(BluetoothProfile.GATT);
            if(connectedDevices!=null)
            {
                for(BluetoothDevice device :connectedDevices)
                {

                }
            }
        }



    }

    public boolean isBleSupported() {
        return this.context != null && context.getApplicationContext().
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }


    private boolean findDevice(BluetoothDevice printer) {
        for (joyBlePrinter printer1 : printerList) {
            if (printer1.sMacAddress.equalsIgnoreCase(printer.getAddress())) {
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

    int dssss = 0;
    ScanCallback mScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);

            if (scanningCallback != null) {
                BluetoothDevice device = result.getDevice();// 获取BLE设备信息
                ScanRecord scanRecordA = result.getScanRecord();
                if(scanRecordA!=null) {
                    String str = device.getName();
                    byte[] scanRecord = scanRecordA.getBytes();
                    //21-26
                    dssss = 0;


                }
                if (!findDevice(device)) {
                    joyBlePrinter printer = new joyBlePrinter(context, device);
                    printerList.add(printer);
                    //scanningCallback.onFindPrinter(printer);
                    Message msg = Message.obtain();
                    msg.obj = printer;
                    mainHandler.sendMessage(msg);
                }
            }
        }
    };


    public static synchronized joyBlePrinterManager getInstance(Context context) {
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

    private void Scan(joyBlePrinterClient.joyBlePrinter_ScanningCallback callback, int nSec)
    {
        int re = ScanBluePrinter();
        if (re == 0)
        {
            handlerDelay.removeCallbacksAndMessages(null);
            this.scanningCallback = callback;
            if (nSec > 0)
            {
                if (bScanning)
                {
                    handlerDelay.postDelayed(this::joyBlePrinterStopScaning, 1000L * nSec);
                }
            }
        }

    }
    public void joyBlePrinterStartScan(joyBlePrinterClient.joyBlePrinter_ScanningCallback callback, int nSec) {
        if (bScanning)
        {
            handlerDelayScanning.removeCallbacksAndMessages(null);

            joyBlePrinterStopScaning();
            handlerDelayScanning.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Scan(callback,nSec);
                    if(joyBlePrinter.bLog) {
                        Log.d(joyBlePrinter.TAG, "start scanning");
                    }
                }
            },1200);
        }
        else
        {
            if(joyBlePrinter.bLog)
                Log.d(joyBlePrinter.TAG,"start ");
            Scan(callback,nSec);
        }

    }




    public int joyBlePrinterStopScaning() {

        handlerDelay.removeCallbacksAndMessages(null);
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
                if (mScanner != null) {
                    mScanner.stopScan(mScanCallback);
//                    for(joyBlePrinter printer : printerList)
//                    {
//                        if(printer.isConnected())
//                        {
//                            printer.Disconnect();
//                        }
//                    }
                    printerList.clear();
                    SystemClock.sleep(150);
                    scanningCallback = null;
                }
                if(joyBlePrinter.bLog) {
                    Log.d(joyBlePrinter.TAG, "Stop Scanning");
                }

            }
        }
        catch (Exception ignored)
        {

        }
        finally {
            handlerDelay.removeCallbacksAndMessages(null);
            bScanning = false;
            scanningCallback = null;
        }
        return 0;

    }


    private int ScanBluePrinter() {
        if(bScanning)
            return 0;
        int nResult = -1;
        printerList.clear();
        UUID[] da = new UUID[1];
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
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(sAdvServiceUUID)).build();
                filters.add(filter);

                ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
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
            if(joyBlePrinter.bLog) {
                Log.d(joyBlePrinter.TAG, "Permission error");
            }
        }
        return nResult;

    }




}
