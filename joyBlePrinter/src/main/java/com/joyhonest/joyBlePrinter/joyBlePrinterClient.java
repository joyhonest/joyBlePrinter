package com.joyhonest.joyBlePrinter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;

public class joyBlePrinterClient {

    private static joyBlePrinterManager blePrinterManager = null;
    private  static joyBlePrinter mSelectedPrinter = null;





    //public static ByteBuffer mDirectBuffer;


    private static final String TAG = "JoyBlePrinter";
    static {
        try {
            System.loadLibrary("JoyBlePrinter");    //2024-07-09 //名称改为JoyCamera
      //      mDirectBuffer = ByteBuffer.allocateDirect(384*2000+1024);     //获取每帧数据，主要根据实际情况，分配足够的空间。
           // naSetDirectBuffer(mDirectBuffer, 384*2000+1024);
        } catch (UnsatisfiedLinkError Ule) {
            Log.e(TAG, "Cannot load JoyBlePrinter.so ...");
            //Ule.printStackTrace();
        } finally {
        }
    }

    //private  static void joyBlePrinter_Init(Context context)
    //public static int  joyBlePrinter_StartScan(scanPrinterCallback callback,int nSec)
    //public static void joyBlePrinter_SelectPrinter(joyBlePrinter printer,joyBlePrinter_StatusCallback callback);
    //public static int joyBlePrinter_SetBitbmp(Bitmap bmp,boolean bPiont,boolean bAutoRotate)  //bPiont = true 点阵  false 灰度 bAutoRotate  自动选择
    //public static  int  joyBlePrinter_Connect()
    //joyBlePrinter_isConnected();
    //public static  int  joyBlePrinter_StartPrintting()
    //public static  int  joyBlePrinter_StopScan()

    //public static  void joyBlePrinter_GetFirmwareVersion(joyBlePrinter_FirmwareVersionCallback callback)
    //public static  void joyBlePrinter_GetSDSize_Battery(joyBlePrinter_getBatteryCallback callback)
    //public static void joyBlePrinter_GetAutoSleepTime(joyBlePrinter_AutoSleepTimeCallback callback)
    //public static void joyBlePrinter_SetAutoSleepTime(int n)


    //firmwareVersionCallback

    public static  void joyBlePrinter_Disconnect()
    {
        if(mSelectedPrinter != null)
        {
            mSelectedPrinter.Disconnect();
        }
    }
    public static  int  joyBlePrinter_Connect()
    {
        if(mSelectedPrinter != null)
        {
            if(blePrinterManager.joyBlePrinterStopScaning() == 0) //如果还在扫描，就停止扫描并且等待200ms
            {
                new Handler(Looper.getMainLooper()).postDelayed(() -> mSelectedPrinter.Connect(),500);
            }
            else
            {
                return mSelectedPrinter.Connect();
            }
            return 0;
        }
        return  -1;

    }
    public static  int  joyBlePrinter_StartPrintting(int nDensity)
    {
        if(mSelectedPrinter != null)
        {
            if(mSelectedPrinter.isConnected())
            {
                mSelectedPrinter.StartPrinting();
                 mSelectedPrinter.nPrinterValue = nDensity;
                 return naStartPrinting();
            }
        }
        return  -1;
    }

    public   static void joyBlePrinter_Init(Context context)
    {
            blePrinterManager = joyBlePrinterManager.getInstance(context);
    }




    public static boolean joyBlePrinter_isScanning()
    {
        if(blePrinterManager!=null)
        {
            return blePrinterManager.bScanning;
        }
        else
        {
            return false;
        }
    }
    public static int  joyBlePrinter_StartScan(joyBlePrinter_ScanningCallback callback,int nSec)
    {
         if(blePrinterManager!=null)
         {
             return  blePrinterManager.joyBlePrinterStartScan(callback,nSec);
         }
         else
         {
             return -1;
         }
    }

    public static   void  joyBlePrinter_StopPrinting()
    {
        if(mSelectedPrinter!=null)
        {
            mSelectedPrinter.StopPrinting();
        }
        else
        {

        }
    }
    public  static  int joyBlePrinter_StopScan()
    {
        if(blePrinterManager!=null)
        {
            return  blePrinterManager.joyBlePrinterStopScaning();
        }
        else
        {
            return -1;
        }
    }

    public static void joyBlePrinter_SelectPrinter(joyBlePrinter printer,joyBlePrinter_StatusCallback callback)
    {
        mSelectedPrinter = printer;
        mSelectedPrinter.Statuscallback = callback;
    }

    public static int joyBlePrinter_SetBitbmp(Bitmap bmp,boolean bPiont,boolean bAutoRotate)
    {
        if(mSelectedPrinter !=null)
        {
            mSelectedPrinter.bLattice = bPiont;
            return naSetBitbmpB(bmp,bPiont,bAutoRotate);
        }
        else
        {
            return  -1;
        }
    }
    public static int joyBlePrinter_SetBitbmp(Bitmap bmp,boolean bPiont)  //bPiont = true 点阵  false 灰度
    {
           return joyBlePrinter_SetBitbmp(bmp,bPiont,true);

    }

    public static  void joyBlePrinter_GetFirmwareVersion(joyBlePrinter_FirmwareVersionCallback callback)
    {
        if(mSelectedPrinter == null) {
            return;
        }
          mSelectedPrinter.firmwareVersionCallback = callback;
          mSelectedPrinter.getFirmwareVersion();
    }

    public static void joyBlePrinter_GetAutoSleepTime(joyBlePrinter_AutoSleepTimeCallback callback)
    {
        if(mSelectedPrinter == null)
            return;
        mSelectedPrinter.autoSleepTimeCallback = callback;
        mSelectedPrinter.GetAutoSleepTime();

    }
    public static void joyBlePrinter_SetLogEnable(boolean b)
    {
        joyBlePrinter.bLog = b;
    }

    public static void joyBlePrinter_SetAutoSleepTime(int n)
    {
        if(mSelectedPrinter ==null)
            return;
        mSelectedPrinter.SetAutoSleepTime(n);

    }

    public static  void joyBlePrinter_GetSDSize_Battery(joyBlePrinter_getBatteryCallback callback)
    {
        if(mSelectedPrinter ==null)
            return;
        mSelectedPrinter.getBatteryCallback = callback;
        mSelectedPrinter.getDeviceStatus();
    }

    public  interface joyBlePrinter_ScanningCallback
    {
        public void   onFindPrinter(joyBlePrinter joyPrinter);
    }

    public interface  joyBlePrinter_StatusCallback
    {
         void onConnectedStatus(int nStatus,String sMac);
        void onPrinterStatus(int nStatus,String sMac);
    }

    public interface joyBlePrinter_getBatteryCallback
    {
        void onGetBattery(int nBatter,int nSDSize,String sMac);
    }
    public interface joyBlePrinter_FirmwareVersionCallback
    {
        void onGetFirmwareVersion(String SVer,String sMac);
    }

    public interface  joyBlePrinter_AutoSleepTimeCallback
    {
        void onGetAutoSleepTime(int nMin,String sMac);
    }


    public interface  joyBlePrinter_isAvailableCallback
    {
        void onIsAvailable(boolean b,String sMac);
    }


    public static boolean joyBlePrinter_isConnected()
    {
        return mSelectedPrinter !=null && mSelectedPrinter.isConnected();
    }
    public boolean isBleSupported() {
        return  blePrinterManager!=null && blePrinterManager.isBleSupported();
    }


    public static  void joyBlePrinter_GetPrinterStatus()
    {
        if(mSelectedPrinter!=null)
        {
            mSelectedPrinter.getDeviceStatus();
        }
    }

 // 检查蓝牙是否启用
    public boolean isBluetoothEnabled() {
        return blePrinterManager !=null && blePrinterManager.isBluetoothEnabled();
    }

    private static void onGetData(byte []data,int nInx)      //收到数据
    {
        //if(mPrintingCallback !=null)
//        {
//            mPrintingCallback.onGetData(data,nInx);
//        }
        if(mSelectedPrinter !=null)
        {
            mSelectedPrinter.onGetData(data,nInx);
        }
    }

    public static void joyBlePrinter_GetBlePrinterisAvailable(joyBlePrinter_isAvailableCallback callback)
    {
        if(mSelectedPrinter==null)
        {
            callback.onIsAvailable(false,"");
            return;
        }
        if(!mSelectedPrinter.isConnected())
        {
            callback.onIsAvailable(false, mSelectedPrinter.sMacAddress);
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mSelectedPrinter.isAvailableCallback = callback;
                mSelectedPrinter.getIsAvailable();
            }
        },1500);

    }



    public static void joyBlePrinter_SendDataDelay(int nMs)
    {
        if(mSelectedPrinter!=null)
        {

            mSelectedPrinter.F_SetnDelay(nMs);
        }
    }

    private static native int naSetBitbmpB(Bitmap bmp,boolean bPiont,boolean bRotate);
    private static  native int naStartPrinting();


    //public static native  void yuv420toBitmap(byte []data,Bitmap bmp,int w,int h);




}
