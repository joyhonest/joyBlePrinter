package com.joyhonest.joyBlePrinter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;

public class joyBlePrinterClient {
    private static joyBlePrinterManager blePrinterManager = null;
    private  static joyBlePrinter mSelectedPrinter = null;





    public static ByteBuffer mDirectBuffer;


    private static final String TAG = "JoyBlePrinter";
    static {
        try {
            System.loadLibrary("JoyBlePrinter");    //2024-07-09 //名称改为JoyCamera
            mDirectBuffer = ByteBuffer.allocateDirect(384*2000+1024);     //获取每帧数据，主要根据实际情况，分配足够的空间。
            naSetDirectBuffer(mDirectBuffer, 384*2000+1024);
        } catch (UnsatisfiedLinkError Ule) {
            Log.e(TAG, "Cannot load JoyBlePrinter.so ...");
            Ule.printStackTrace();
        } finally {
        }
    }

    //private  static void joyBlePrinter_Init(Context context)
    //public static int  joyBlePrinter_StartScan(scanPrinterCallback callback,int nSec)
    //public static void joyBlePrinter_SelectPrinter(joyBlePrinter printer,joyBlePrinter_StatusCallback callback);
    //public static void joyBlePrinter_SetBitbmp(Bitmap bmp,boolean bPiont)  //bPiont = true 点阵  false 灰度
    //public static  int  joyBlePrinter_Connect()
    //joyBlePrinter_isConnected();
    //public static  int  joyBlePrinter_StartPrintting()
    //public static  int  joyBlePrinter_StopScan()

    //public static  void joyBlePrinter_GetFirmwareVersion(joyBlePrinter_FirmwareVersionCallback callback)
    //public static  void joyBlePrinter_GetSDSize_Battery(joyBlePrinter_getBatteryCallback callback)
    //public static void joyBlePrinter_GetAutoSleepTime(joyBlePrinter_AutoSleepTimeCallback callback)


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
                SystemClock.sleep(250);
            }
            return mSelectedPrinter.Connect();
        }
        return  -1;

    }
    public static  int  joyBlePrinter_StartPrintting(int nDensity)
    {
        if(mSelectedPrinter != null)
        {
            if(mSelectedPrinter.isConnected())
            {
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

    public static int joyBlePrinter_SetBitbmp(Bitmap bmp,boolean bPiont)  //bPiont = true 点阵  false 灰度
    {
           if(mSelectedPrinter !=null)
           {
               mSelectedPrinter.bLattice = bPiont;
               return naSetBitbmp(bmp,bPiont);
           }
           else
           {
               return  -1;
           }

    }

    public static  void joyBlePrinter_GetFirmwareVersion(joyBlePrinter_FirmwareVersionCallback callback)
    {
          mSelectedPrinter.firmwareVersionCallback = callback;
          mSelectedPrinter.getFirmwareVersion();
    }

    public static void joyBlePrinter_GetAutoSleepTime(joyBlePrinter_AutoSleepTimeCallback callback)
    {
        mSelectedPrinter.autoSleepTimeCallback = callback;
        mSelectedPrinter.GetAutoSleepTime();

    }

    public static void joyBlePrinter_SetAutoSleepTime(int n)
    {
        mSelectedPrinter.SetAutoSleepTime(n);

    }

    public static  void joyBlePrinter_GetSDSize_Battery(joyBlePrinter_getBatteryCallback callback)
    {
        mSelectedPrinter.getBatteryCallback = callback;
        mSelectedPrinter.getDeviceStatus();
    }

    public  interface joyBlePrinter_ScanningCallback
    {
        public void   onFindPrinter(joyBlePrinter joyPrinter);
    }

    public interface  joyBlePrinter_StatusCallback
    {
         void onConnectedStatus(int nStatus);
        void onPrinterStatus(int nStatus);
    }

    public interface joyBlePrinter_getBatteryCallback
    {
        void onGetBattery(int nBatter,int nSDSize);
    }
    public interface joyBlePrinter_FirmwareVersionCallback
    {
        void onGetFirmwareVersion(String SVer);
    }

    public interface  joyBlePrinter_AutoSleepTimeCallback
    {
        void onGetAutoSleepTime(int nMin);
    }


    public interface  joyBlePrinter_isAvailableCallback
    {
        void onIsAvailable(boolean b);
    }


    public static boolean joyBlePrinter_isConnected()
    {
        return mSelectedPrinter !=null && mSelectedPrinter.isConnected();
    }
    public boolean isBleSupported() {
        return  blePrinterManager!=null && blePrinterManager.isBleSupported();
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
            callback.onIsAvailable(false);
            return;
        }
        if(!mSelectedPrinter.isConnected())
        {
            callback.onIsAvailable(false);
            return;
        }
        mSelectedPrinter.isAvailableCallback = callback;
        mSelectedPrinter.getIsAvailable();
    }

    public static  void naSetLog(boolean b)
    {
            joyBlePrinter.bLog = b;
    }

    private static native void naSetDirectBuffer(Object buffer, int nLen);

    private static native int naSetBitbmp(Bitmap bmp,boolean bPiont);

    private static  native int naStartPrinting();




}
