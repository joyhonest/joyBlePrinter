package com.joyhonest.joyBlePrinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class joyBlePrinter {
    private final String TAG = "joyBlePrinter";
    private int nPacklen = 200;
    private  volatile boolean bBuffFull = false;

    private final Handler mainHandler;


    public int nPrinterValue = 4;


    public boolean bLattice = true;

    private int  nDelayMs = 5;

    private boolean bNeedSent = false;
    Context context;
    public String sName = "aaac";
    public String sMacAddress;

    BluetoothDevice bleDevice;

    int nLineCount;// = GrayDataList.size();
    byte[] m_data = null;
    int nLine = 0;
    int nStep = -1;

    private byte[] mSentBuffer;
    private int mSentInx = 0;
    private int mSentCount = 0;


    BluetoothGatt mGatt = null;
    boolean isOk = false;
    BluetoothGattCharacteristic Write_characteristic = null;

    UUID ServiceUUID = UUID.fromString("0000ae00-0000-1000-8000-00805f9b34fb");
    //0000af30-0000-1000-8000-00805f9b34fb
    UUID writeUUID = UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb");
    UUID readUUID = UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb");


    private Thread writeThread;
    private List<byte[]> GrayDataList;

    public static boolean bLog = false;

    private int nStatus = 0;

    long t1;
    long t2;
    private boolean bExitThread = false;
    @SuppressLint("MissingPermission")
    public joyBlePrinter(Context context, BluetoothDevice device) {



            // 模拟耗时操作，在后台线程执行







        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                //super.handleMessage(msg);
                String str = (String) msg.obj;
                if (str.equalsIgnoreCase("autosleep")) {
                    int n = msg.arg1;
                    if (autoSleepTimeCallback != null) {
                        autoSleepTimeCallback.onGetAutoSleepTime(n,sMacAddress);
                        autoSleepTimeCallback = null;
                    }
                }
                if (str.startsWith("FirmwareVersion:")) {
                    if (str.length() > 18) {
                        str = str.substring(16);
                        if (firmwareVersionCallback != null) {
                            firmwareVersionCallback.onGetFirmwareVersion(str,sMacAddress);
                        }
                    } else {
                        if (firmwareVersionCallback != null) {
                            firmwareVersionCallback.onGetFirmwareVersion("",sMacAddress);
                            firmwareVersionCallback = null;
                        }
                    }
                }
                if (str.equalsIgnoreCase("StatusCallback1")) {
                    int x = msg.arg1;
                    if (Statuscallback != null) {
                        Statuscallback.onPrinterStatus(x & 0xff,sMacAddress);
                    }
                    if (getBatteryCallback != null) {
                        int xx = msg.arg2;
                        int x2 = msg.what;
                        getBatteryCallback.onGetBattery(xx, x2,sMacAddress);
                        getBatteryCallback = null;

                    }
                }
//                if (str.equalsIgnoreCase("StatusCallback2")) {
//                    int x = msg.arg1;
//                    if (Statuscallback != null) {
//                        Statuscallback.onPrinterStatus(x << 8 & 0xff00);
//                    }
//                }
                if (str.equalsIgnoreCase("ConnectedCallback")) {
                    int x = msg.arg1;
                    if (Statuscallback != null) {
                        Statuscallback.onConnectedStatus(x,sMacAddress);
                    }
                }


            }
        };


        this.context = context;
        this.bleDevice = device;
        sName = device.getName();
        sMacAddress = device.getAddress();

        GrayDataList = new ArrayList<>();


    }

    boolean haspermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @SuppressLint("MissingPermission")
    public int WriteData() {

        if (haspermission())
        {
            if (isConnected()) {
                if (Write_characteristic != null) {
                    int nLen = 0;
                    if (mSentInx + nPacklen >= mSentCount) {
                        nLen = mSentCount - mSentInx;
                    } else {
                        nLen = nPacklen;
                    }
                    byte[] data1 = new byte[nLen];
                    System.arraycopy(mSentBuffer, mSentInx, data1, 0, nLen);
                    mSentInx += nLen;
                    bNeedSent = mSentInx < mSentCount;
                    Write_characteristic.setValue(data1);

                    mGatt.writeCharacteristic(Write_characteristic);
                    if (bNeedSent) {
                        return 1;
                    } else {
                        return 0;
                    }

                }
            }
        }
        return 0;
    }


    private void SentSentDataMsg() {
        Message msg = Message.obtain();
        msg.obj = "StatusCallback1";
        msg.arg1 = 0x90 & 0xff; //传输数据
        msg.arg2 = 1;
        msg.what = 0;
        mainHandler.sendMessage(msg);
    }

    private void SentStartPrintingMsg() {
        Message msg = Message.obtain();
        if(bLog)
        {
            Log.e(TAG,"strit pirnt");
        }
        msg.obj = "StatusCallback1";
        msg.arg1 = 0x80 & 0xff; //开始打印
        msg.arg2 = 1;
        msg.what = 0;
        mainHandler.sendMessage(msg);
    }

    public void onGetData(byte[] data, int nInx) {
        if (nInx == -8) {      //point
            bWriteOK = false;
            int len = data.length;
            nLineCount = len / 48;
            nLine = 0;
            m_data = new byte[len];
            System.arraycopy(data, 0, m_data, 0, len);
            nStep = 0;
            F_Sendquality(0x33);
            SentStartPrintingMsg();
            return;
        }
        if (nInx == 0)
        {
            bWriteOK = false;
            GrayDataList.clear();
            GrayDataList.add(data);
            if (bLog)
                Log.e(TAG, "data start----");
        }
        if (nInx == 1) {
            GrayDataList.add(data);
            if (bLog)
                Log.e(TAG, "data ----");
        }

        if (nInx < 0)       //收到处理好的数据OK，开始打印机打印流程。
        {
            bWriteOK = false;
            nLineCount = GrayDataList.size();
            nLine = 0;
            nStep = 0;
            t1 = System.currentTimeMillis();
            F_Sendquality(0x34);
            SentStartPrintingMsg();
            if (bLog) {
                Log.e(TAG, "data is OK");
            }

        }
    }

    byte crc_8(byte[] data1, int pos, int len) {
        byte[] data = new byte[len];
        System.arraycopy(data1, pos, data, 0, len);
        byte crc = 0;
        byte xx = 0x07;
        for (byte da : data) {
            crc = (byte) (da ^ crc);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc <<= 1;
                    crc = (byte) (crc ^ xx);
                } else {
                    crc <<= 1;
                }
            }
        }
        return (byte) crc;
    }

    private void F_Sendquality(int n) {
        //51 78 A4 00 01 00 33 99 FF
        mSentBuffer = new byte[9];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xA4;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x01;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = (byte) n;
        mSentBuffer[7] = crc_8(mSentBuffer, 6, 1);
        mSentBuffer[8] = (byte) 0xFF;
        mSentCount = 9;
        mSentInx = 0;
        WriteData();
        if (bLog)
            Log.e(TAG, "设定质量");
    }


    @SuppressLint("MissingPermission")
    public boolean enableNotification(BluetoothGatt bluetoothGatt, boolean enable, BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null || characteristic == null) {
            return false;
        }
        //if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
        {
            if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
                return false;
            }
            //获取到Notify当中的Descriptor通道  然后再进行注册
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (clientConfig == null) {
                return false;
            }
            if (enable) {
                clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            return bluetoothGatt.writeDescriptor(clientConfig);
        }

    }

    private final BluetoothGattCallback bleCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isConnected = (newState == BluetoothAdapter.STATE_CONNECTED);
            // boolean isSuccess = (status == BluetoothGatt.GATT_SUCCESS);
            if (isConnected) {

                if (nPacklen > 20) {
                    if (!gatt.requestMtu(nPacklen + 3)) {
                        nPacklen = 20;
                    }
                } else {
                    gatt.discoverServices();
                }
                //gatt.discoverServices();


                if (bLog)
                    Log.e(TAG, "connected");
            } else {
                gatt.close();
                mGatt = null;
                isOk = false;
                Write_characteristic = null;
                if (bLog)
                    Log.e(TAG, "Dis-connected");
                boolean b = false;
                //EventBus.getDefault().post(b,"onBlePrinterConnectedStatus");

                Message msg = Message.obtain();
                msg.obj = "ConnectedCallback";
                msg.arg1 = -1;
                mainHandler.sendMessage(msg);
//                if (Statuscallback != null) {
//                    Statuscallback.onConnectedStatus(-1);
//                }

                //if(writeThread.isAlive())
                {
                    bExitThread = true;
                    //Log.e(TAG,"writeThread interrupt!");
                    //writeThread.interrupt();
                }

            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (bLog) {
                Log.e(TAG, "mtu = " + mtu);
            }
            if (nPacklen > 20) {
                gatt.discoverServices();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (bLog)
                Log.e(TAG, "connected 11");
            mGatt = gatt;
            isOk = true;
            nStep = -1;
            BluetoothGattService service = mGatt.getService(ServiceUUID);
            Write_characteristic = service.getCharacteristic(writeUUID);
            BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(readUUID);
            boolean bRe = enableNotification(mGatt, true, notifyCharacteristic);//注册Notify通知

            Message msg = Message.obtain();
            msg.obj = "ConnectedCallback";
            msg.arg1 = 1;
            mainHandler.sendMessage(msg);

            bExitThread = false;
            writeThread =  new Thread(() -> {
                if(bLog)
                {
                    Log.e(TAG,"writeThread start");
                }
                while (!bExitThread)
                {
                    onWriteOK();
                }
                if(bLog)
                {
                    Log.e(TAG,"writeThread exit");
                }

            });
            writeThread.start();


        }

        //@Override


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //onWriteOK();
            bWriteOK = true;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] receiveData = characteristic.getValue();
            onRead(receiveData);
        }
    };


    @SuppressLint("MissingPermission")
    public void Disconnect() {
        if (isConnected()) {
            //if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            {
                mGatt.disconnect();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public int Connect() {
        if (bleDevice == null) {
            boolean b = false;
            //EventBus.getDefault().post(b,"onBlePrinterConnectedStatus");
//            if(Statuscallback!=null)
//            {
//                Statuscallback.onConnectedStatus(-2);
//            }

            Message msg = Message.obtain();
            msg.obj = "ConnectedCallback";
            msg.arg1 = -2;
            mainHandler.sendMessage(msg);

            return -1;
        }
        if (context == null)
            return -2;
        if (isConnected()) {
            Message msg = Message.obtain();
            msg.obj = "ConnectedCallback";
            msg.arg1 = 1;
            mainHandler.sendMessage(msg);
            return 0;
        }
        isOk = false;
        //if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
        {
            @SuppressLint("MissingPermission") BluetoothGatt gatt = bleDevice.connectGatt(context, false, bleCallback);
            if (gatt != null)
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            return -100;
        }

    }

    //设置打印浓度
    private void setConcentration_Value(int n) {
        //0x19c8 0x20d0 0x27d8 0x2ee0 0x35e8 0x3cf0 0x43f8
        if (n < 0) {
            n = 0;
        }
        if (n > 4) {
            n = 4;
        }

        mSentBuffer = new byte[20];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xAF;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x02;
        mSentBuffer[5] = 0x00;
        if (bLattice) {
            if (n == 0) {

                mSentBuffer[6] = (byte) 0xc8;
                mSentBuffer[7] = (byte) 0x19;
            }
            if (n == 1) {
                mSentBuffer[6] = (byte) 0xd8;
                mSentBuffer[7] = (byte) 0x27;
            }
            if (n == 2) {
                mSentBuffer[6] = (byte) 0xe0;
                mSentBuffer[7] = (byte) 0x2e;
            }
            if (n == 3) {
                mSentBuffer[6] = (byte) 0xe8;
                mSentBuffer[7] = (byte) 0x35;
            }
            if (n == 4) {
                mSentBuffer[6] = (byte) 0xf8;
                mSentBuffer[7] = (byte) 0x43;
            }
        } else {
            //0x0f0a，0x1324，0x173e，0x1b58，0x1f72，0X238C，0x27A6
            if (n == 0) {

                mSentBuffer[6] = (byte) 0x20;
                mSentBuffer[7] = (byte) 0x10;
            }
            if (n == 1) {
                mSentBuffer[6] = (byte) 0x24;
                mSentBuffer[7] = (byte) 0x13;
            }
            if (n == 2) {
                mSentBuffer[6] = (byte) 0x58;
                mSentBuffer[7] = (byte) 0x1b;
            }
            if (n == 3) {
                mSentBuffer[6] = (byte) 0x8c;
                mSentBuffer[7] = (byte) 0x23;
            }
            if (n == 4) {
                mSentBuffer[6] = (byte) 0xa6;
                mSentBuffer[7] = (byte) 0x27;
            }
        }

        mSentBuffer[8] = (byte) crc_8(mSentBuffer, 6, 2);
        mSentBuffer[9] = (byte) 0xff;
        mSentCount = 10;
        mSentInx = 0;
        if (bLog)
            Log.e(TAG, "设定浓度!");
        WriteData();
    }

    //51 78 BE 00 01 00 00 00 FF
    private void F_SentSpeed() {
        mSentBuffer = new byte[10];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xBE;
        mSentBuffer[3] = 0x00;

        mSentBuffer[4] = 0x02;
        mSentBuffer[5] = 0x00;

        mSentBuffer[6] = 0x00;
        mSentBuffer[7] = 0x01;
        mSentBuffer[8] = (byte) crc_8(mSentBuffer, 6, 1);
        mSentBuffer[9] = (byte) 0xFF;
        mSentInx = 0;
        mSentCount = 10;
        WriteData();
        if (bLog)
            Log.e(TAG, "设定打印速度");
    }

    private void setMotor_Value(int n) {
        mSentBuffer = new byte[20];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xBD;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x01;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = (byte) n;

        mSentBuffer[7] = (byte) crc_8(mSentBuffer, 6, 1);
        mSentBuffer[8] = (byte) 0xff;
        mSentCount = 9;
        mSentInx = 0;
        if (bLog)
            Log.e(TAG, "设定马达速度!");
        WriteData();
    }

    private void F_SendLine(byte[] data, int n) {
        if (bLattice) {
            //SystemClock.sleep(2);
            mSentBuffer = new byte[60];
            mSentBuffer[0] = 0x51;
            mSentBuffer[1] = 0x78;
            mSentBuffer[2] = (byte) 0xA2;
            mSentBuffer[3] = 0x00;
            mSentBuffer[4] = (byte) 48;
            mSentBuffer[5] = (byte) (48 >> 8);
            System.arraycopy(data, n * 48, mSentBuffer, 6, 48);
            mSentBuffer[54] = (byte) crc_8(mSentBuffer, 6, 48);
            mSentBuffer[55] = (byte) 0xff;
            mSentInx = 0;
            mSentCount = 56;
            WriteData();

        }


    }


    public void F_SetnDelay(int nDelayMs)
    {
        this.nDelayMs = nDelayMs;
    }
    private void F_SentGrayData_Line() {

        t2 = System.currentTimeMillis();
        if(nDelayMs>0) {
            long da = t2 - t1;
            if (da < nDelayMs) {
                if (bLog) {
                    Log.e(TAG, "delay gray");
                }
                SystemClock.sleep(nDelayMs - da);
            }
        }
        t1 = System.currentTimeMillis();
        mSentBuffer = GrayDataList.get(nLine);
        mSentInx = 0;
        mSentCount = mSentBuffer.length;
        WriteData();
        if (bLog)
            Log.e(TAG, "Send GrayLie " + nLine);

    }

    private void F_SetMovePage() {
        mSentBuffer = new byte[10];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xA1;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x02;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = (byte) 60;
        mSentBuffer[7] = (byte) (60 >> 8);
        mSentBuffer[8] = crc_8(mSentBuffer, 6, 2);
        mSentBuffer[9] = (byte) 0xFF;
        mSentInx = 0;
        mSentCount = 10;
        WriteData();
        if (bLog)
            Log.e(TAG, "走纸");
    }

    public void readBleStatusCmd() {
        mSentBuffer = new byte[10];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xA3;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x01;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = 0x00;
        mSentBuffer[7] = 0;
        mSentBuffer[8] = (byte) 0xFF;
        mSentCount = 9;
        mSentInx = 0;
        WriteData();
        if (bLog)
            Log.e(TAG, "获取状态");
        //SystemClock.sleep(100);
    }

    public boolean isConnected() {
        return isOk && (mGatt != null);
    }



    private volatile boolean bCanSent = true;
    public void  StopPrinting()
    {
        bCanSent = false;
    }
    public void  StartPrinting()
    {
        bCanSent = true;
    }


    private  volatile  boolean bWriteOK = false;
    private void onWriteOK() {
        if(!bWriteOK)
        {
            return;
        }
        bWriteOK = false;

//        if(!bCanSent) {
//            bNeedSent = false;
//            bBuffFull = false;
//            return;
//        }
        int n = 55;
//        boolean bd =true;
//        boolean bfu = false;
        while (bBuffFull) {
//            bfu = true;
//            if(bd)
//            {
//                bd = false;
////                Log.e(TAG,"data --- 1");
//            }
            SystemClock.sleep(100);
            n--;
            if(n==0) {
                if(bLog)
                {
                    Log.e(TAG,"time over");
                }
                break;
            }
        }
//        if(bfu) {
//  //          Log.e(TAG, "data --- 2");
//        }
        if (bNeedSent)    //超过蓝牙packlen，分包发送
        {
                WriteData();
                return;
        }
        if (nStep == 0) {
            nStep = 1;
            setConcentration_Value(nPrinterValue);
            return;

        }
        if (nStep == 1) {
            nStep = 2;
            F_SentSpeed();
            return;

        }


        if (bLattice) {
            if (nStep == 2) {
                nStep = 3;
                nLine = 0;
                setMotor_Value(0x14);
                return;
            }
            if (nStep == 3) {
                nStep = 4;
                F_SendLine(m_data, nLine++);
                if (bLog) {
                    Log.e(TAG, "seng nLne = " + nLine);
                }
                return;
            }
            if (nStep == 4) {
                if ((nLine % 200) == 0)
                {
                    nStep = 5;
                    setMotor_Value(0x14);
//                    SystemClock.sleep(500);
//                    if(bLog)
//                        Log.e(TAG,"delay !!!");
                } else {
                    if (nLine < nLineCount) {
                        F_SendLine(m_data, nLine++);
                        if (bLog) {
                            Log.e(TAG, "seng nLne = " + nLine);
                        }
                    } else {
                        nStep = 6;
                        setMotor_Value(0x14);

                    }
                }

                return;
            }
            if (nStep == 5) {
                nStep = 4;
                if (nLine < nLineCount) {
                    F_SendLine(m_data, nLine++);
                } else {
                    nStep = 6;
                    setMotor_Value(0x19);
                }
                return;
            }
        }
        else
        {
            if (nStep == 2) {
                nStep = 3;
                nLine = 0;
                setMotor_Value(0x28);
                return;
            }
            if (nStep == 3) {
                //发送 灰度数据
                nStep = 4;
                setMotor_Value(0x28);
                return;
            }

            if (nStep == 4) {
                nStep = 3;
                F_SentGrayData_Line();
                nLine++;


                if (nLine == nLineCount) {
                    nStep = 6;
                }

//                if((nLine % 20) == 0 && nStep !=6)      //20*20 行停0.6S发送
//                {
//                    SystemClock.sleep(1500);
//                    if(bLog)
//                    {
//                        Log.e(TAG,"delay 123");
//                    }
//                }

                return;
            }
        }
        if (nStep == 6) {
            nStep = 7;
            SystemClock.sleep(500);
            F_SetMovePage();
            return;
        }
        if (nStep == 7) {
            nStep = -1;
            F_SetMovePage();
            return;
        }
//            if (nStep == 8) {
//                nStep = 9;
//                setMotor_Value(0x19);
//                return;
//            }
//            if (nStep == 9) {
//                nStep = 10;
//                readBleStatusCmd(); //读取状态
//                return;
//            }
//            if (nStep == 10) {
//                nStep = 11;
//                readBleStatusCmd();    //多读几次
//                return;
//            }
//            if (nStep == 11) {
//                nStep = -1;
//                readBleStatusCmd();    //多读几次
//                return;
//            }

    }

    private void onRead(byte[] da) {
        if (da != null) {
            if (bLog) {
                StringBuilder str = new StringBuilder();
                for (byte da1 : da) {
                    String ss = String.format("%02X", da1);

                    str.append(ss).append(" ");
                }

                Log.e(TAG, str.toString());
            }

            if (da.length >= 9) {

                if (da[0] == 0x51 &&
                        da[1] == 0x78 &&
                        da[2] == (byte) 0x92 &&
                        da[3] == 0x01)    //
                {
                    if (da[6] == 0x01)   //自动关机时间
                    {
                        Message msg = Message.obtain();
                        msg.arg1 = da[7];
                        msg.obj = "autosleep";
                        mainHandler.sendMessage(msg);
                    }

                }

                if (da[0] == 0x51 &&
                        da[1] == 0x78 &&
                        da[2] == (byte) 0x90 &&
                        da[3] == 0x01)    //固件版本
                {
                    if (da.length >= 12) {
                        byte[] data = new byte[4];
                        System.arraycopy(da, 6, data, 0, 4);
                        String sP = new String(data);
                        int nVer = da[10] + da[11] * 0x100;
                        String sVer = String.format(Locale.getDefault(), "%s-%03d", sP, nVer);
                        Message msg = Message.obtain();
                        msg.obj = "FirmwareVersion:" + sVer;
                        mainHandler.sendMessage(msg);
                    }
                }

                if (da[0] == 0x51 &&
                        da[1] == 0x78 &&
                        da[2] == (byte) 0xA3 &&
                        da[3] == 0x01)    //打印机状态返回
                {
                    int Status = da[6];
                    getAvailableHandle.removeCallbacksAndMessages(null);
                    if (bIsGetAvailable) {
                        bIsGetAvailable = false;
                        if (isAvailableCallback != null) {
                            isAvailableCallback.onIsAvailable(true,sMacAddress);
                        }
                    }

                    nStatus = Status;
                    Message msg = Message.obtain();
                    msg.obj = "StatusCallback1";
                    msg.arg1 = Status;
                    //msg.arg2 = 0;
                    msg.what = 0;
                    if (da.length >= 13) {
                        msg.arg2 = da[9] & 0x7f;   //电量
                        msg.what = da[10] + da[11] * 0x100 + da[12] * 0x10000 + da[13] * 0x1000000;
                    } else {
                        msg.arg2 = -1;
                    }
                    mainHandler.sendMessage(msg);
                    /*
                    Bit0：缺纸
                    ➢ Bit1：开盖
                    ➢ Bit2：过热
                    ➢ Bit3：缺电
                    bit4 = UsB
                    ➢ 0x03：缺纸+纸仓开
                    ➢ 0x80：正在打印
                    ➢ 0x00：空闲态

                     */
                }
                if (da[0] == 0x51 &&
                        da[1] == 0x78 &&
                        da[2] == (byte) 0xAE &&
                        da[3] == 0x01)   //
                {
                    int s = da[6];
                    if (da[6] != 0) {
                        bBuffFull = true;
                        if (bLog)
                            Log.e(TAG, "data full");
                    } else {
                        if (bLog)
                            Log.e(TAG, "data empty");
                        bBuffFull = false;
                    }

//                    Message msg = Message.obtain();
//                    msg.obj = "StatusCallback2";
//                    msg.arg1 = s;
//                    mainHandler.sendMessage(msg);

                }
            }


        }
    }


    public joyBlePrinterClient.joyBlePrinter_StatusCallback Statuscallback = null;

    public joyBlePrinterClient.joyBlePrinter_FirmwareVersionCallback firmwareVersionCallback = null;

    public joyBlePrinterClient.joyBlePrinter_AutoSleepTimeCallback autoSleepTimeCallback = null;
    public joyBlePrinterClient.joyBlePrinter_getBatteryCallback getBatteryCallback = null;


    public joyBlePrinterClient.joyBlePrinter_isAvailableCallback isAvailableCallback = null;


    private boolean bIsGetAvailable = false;
    private final Handler getAvailableHandle = new Handler(Looper.getMainLooper());

    public void getIsAvailable() {
        bIsGetAvailable = true;
        getDeviceStatus();
        getAvailableHandle.removeCallbacksAndMessages(null);
        getAvailableHandle.postDelayed(() -> {
            bIsGetAvailable = false;
            if (isAvailableCallback != null) {
                isAvailableCallback.onIsAvailable(false,sMacAddress);
            }
        }, 1000);

    }

    public void getDeviceStatus() {
        mSentBuffer = new byte[10];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        mSentBuffer[2] = (byte) 0xA3;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x01;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = 0x00;
        mSentBuffer[7] = 0x00;
        mSentBuffer[8] = (byte) 0xFF;
        mSentCount = 9;
        mSentInx = 0;
        WriteData();
    }

    public void GetAutoSleepTime() {
        mSentBuffer = new byte[20];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        //mSentBuffer[2] = (byte)0xF2;
        mSentBuffer[2] = (byte) 0x92;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x06;


        mSentBuffer[6] = 0x00;
        mSentBuffer[7] = 0x01;
        mSentBuffer[8] = 0x00;
        mSentBuffer[9] = 0x00;
        mSentBuffer[10] = 0x00;
        mSentBuffer[11] = 0x00;
        mSentBuffer[12] = crc_8(mSentBuffer, 6, 6);
        ;
        mSentBuffer[13] = (byte) 0xff;
        mSentCount = 14;
        mSentInx = 0;
        WriteData();
    }

    public void SetAutoSleepTime(int n) {
        mSentBuffer = new byte[20];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        //mSentBuffer[2] = (byte)0xF2;
        mSentBuffer[2] = (byte) 0x92;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x06;
        mSentBuffer[5] = 0x00;

        mSentBuffer[6] = 0x01;
        mSentBuffer[7] = 0x01;
        mSentBuffer[8] = (byte) n;
        mSentBuffer[9] = 0x00;
        mSentBuffer[10] = 0x00;
        mSentBuffer[11] = 0x00;
        mSentBuffer[12] = crc_8(mSentBuffer, 6, 6);
        ;
        mSentBuffer[13] = (byte) 0xff;
        mSentCount = 14;
        mSentInx = 0;
        WriteData();
    }

    public void getFirmwareVersion() {
        mSentBuffer = new byte[20];
        mSentBuffer[0] = 0x51;
        mSentBuffer[1] = 0x78;
        //mSentBuffer[2] = (byte)0xA8;
        mSentBuffer[2] = (byte) 0x90;
        mSentBuffer[3] = 0x00;
        mSentBuffer[4] = 0x01;
        mSentBuffer[5] = 0x00;
        mSentBuffer[6] = 0x00;
        mSentBuffer[7] = 0x00;
        mSentBuffer[8] = (byte) 0xff;
        mSentCount = 9;
        mSentInx = 0;
        WriteData();
    }
}
