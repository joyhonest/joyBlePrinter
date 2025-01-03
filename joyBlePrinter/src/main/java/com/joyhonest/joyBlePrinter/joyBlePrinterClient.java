package com.joyhonest.joyBlePrinter;

import android.content.Context;
import android.graphics.Bitmap;
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
            blePrinterManager.joyBlePrinterStopScaning();

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

    public static void joyBlePrinter_SelectPrinter(joyBlePrinter printer,joyBlePrinter_StatusCallback callback)
    {
        mSelectedPrinter = printer;
        mSelectedPrinter.callback = callback;
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


    public  interface joyBlePrinter_ScanningCallback
    {
        public void   onFindPrinter(joyBlePrinter joyPrinter);
    }

    public interface  joyBlePrinter_StatusCallback
    {
         void onConnectedStatus(int nStatus);
        void onPrinterStatus(int nStatus);
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

    private static native void naSetDirectBuffer(Object buffer, int nLen);

    private static native int naSetBitbmp(Bitmap bmp,boolean bPiont);

    private static  native int naStartPrinting();

}
