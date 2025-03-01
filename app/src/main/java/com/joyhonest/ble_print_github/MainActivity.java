package com.joyhonest.ble_print_github;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.joyhonest.ble_print_github.databinding.ActivityMainBinding;
import com.joyhonest.joyBlePrinter.joyBlePrinter;
import com.joyhonest.joyBlePrinter.joyBlePrinterClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "blePrinter";
    ActivityMainBinding binding;
    PermissionAsker  mAsker;

    List<String> dataList;
    ArrayAdapter adapter;

    private Handler hiddenMesageBoxHandler;


    private boolean bStartPrinting = false;


    List<joyBlePrinter>  blePrinterList;
    boolean blattice = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hiddenMesageBoxHandler = new Handler(getMainLooper());
        binding.btnScan.setOnClickListener(this);
        binding.btnPrint.setOnClickListener(this);
        binding.MessageView.setOnClickListener(this);


        blePrinterList = new ArrayList<>();

        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,dataList);
        binding.listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        binding.listView.setOnItemClickListener((parent, view, position, id) -> {
            //选择具体的哪一台打印机

            joyBlePrinterClient.joyBlePrinter_SelectPrinter(blePrinterList.get(position), new joyBlePrinterClient.joyBlePrinter_StatusCallback() {
                @Override
                public void onConnectedStatus(int nStatus) {
                    if(nStatus == 1)   //已经连接
                    {
                        F_DispMessage("已经连接上打印机", false);
                        HiddenmesageBox();
                        joyBlePrinterClient.joyBlePrinter_GetBlePrinterisAvailable(b -> Log.e(TAG,"data = "+b));
                    }
                    if(nStatus <0)  //断开连接
                    {
                        F_DispMessage("已经断开打印机", false);
                        HiddenmesageBox();
                    }
                }
                @Override
                public void onPrinterStatus(int nStatue)
                {
                    Log.e(TAG,"Status =  "+ nStatue);

                        if (nStatue == 0) {
                            if(bStartPrinting) {
                                bStartPrinting = false;
                                F_DispMessage("打印完成！", false);
                                HiddenmesageBox();
                            }
                        }

                    if(nStatue == 0x80)
                    {
                        bStartPrinting = true;
                        F_DispMessage("开始打印", false);
                    }
                    if((nStatue & 0x0F)!=0)
                    {
                        bStartPrinting = false;
                        joyBlePrinterClient.joyBlePrinter_StopPrinting();
                    }
                    if((nStatue & 0x04)!=0)
                    {
                        F_DispMessage("打印头过热！", false);
                        HiddenmesageBox();
                    }
                    if((nStatue & 0x03)!=0)
                    {
                        F_DispMessage("打印头开盖或缺纸！", false);
                        HiddenmesageBox();
                    }
                 /*
                 /*
                Bit0：缺纸
                ➢ Bit1：开盖
                ➢ Bit2：过热
                ➢ Bit3：缺电
                ➢ 0x03：缺纸+纸仓开
                ➢ 0x80：正在打印
                ➢ 0x00：空闲态
                  */
                }
            });
            joyBlePrinterClient.joyBlePrinter_Connect();
            F_DispMessage("正在连接蓝牙打印机", false);
        });

        //1 初始化



        mAsker = new PermissionAsker(20, this::scanning_printer, this::F_DispDialg);



        binding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(binding.btnGrayscale.isChecked())
                {
                    blattice = false;
                }
                if(binding.btnLattice.isChecked())
                {
                    blattice = true;
                }

            }
        });

        binding.btnLattice.setChecked(blattice);
        binding.btnGrayscale.setChecked(!blattice);


    }

    private void F_DispDialg()
    {
        Log.e(TAG,"请打开蓝牙相关权限！");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mAsker.onRequestPermissionsResult(grantResults);
    }
    private void F_Check()
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            mAsker.askPermission(this,
                    android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        else
        {
            //android.Manifest.permission.ACCESS_COARSE_LOCATION,
            mAsker.askPermission(this,
                    android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION);
        }

    }

    private void scanning_printer()
    {
        joyBlePrinterClient.joyBlePrinter_Init(getApplicationContext());
        joyBlePrinterClient.joyBlePrinter_SetLogEnable(true);



        if(!joyBlePrinterClient.joyBlePrinter_isScanning()) {
            blePrinterList.clear();
            dataList.clear();
            adapter.notifyDataSetChanged();
            joyBlePrinterClient.joyBlePrinter_StartScan(joyPrinter -> {
                blePrinterList.add(joyPrinter);
                dataList.add(joyPrinter.sName + " " + joyPrinter.sMacAddress);
                adapter.notifyDataSetChanged();
            }, 5);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        joyBlePrinterClient.joyBlePrinter_Disconnect();
    }

    @Override
    public void onClick(View v) {
        if(v == binding.MessageView)
        {
            binding.MessageView.setVisibility(View.GONE);
        }
        if(v == binding.btnScan)
        {
            F_Check();
        }
        if(v == binding.btnPrint)
        {
            if(joyBlePrinterClient.joyBlePrinter_isConnected()) {
//                joyBlePrinterClient.joyBlePrinter_GetSDSize_Battery(new joyBlePrinterClient.joyBlePrinter_getBatteryCallback() {
//                    @Override
//                    public void onGetBattery(int nBatter, int nSDSize) {
//                        Log.e("","battery = "+nBatter +" sd size = "+nSDSize +"MB");
//                    }
//                });

//                SystemClock.sleep(500);
//                joyBlePrinterClient.joyBlePrinter_GetFirmwareVersion(new joyBlePrinterClient.joyBlePrinter_FirmwareVersionCallback() {
//                    @Override
//                    public void onGetFirmwareVersion(String sVer) {
//                        Log.e("","ver = "+sVer);
//                    }
//                });
//
//                SystemClock.sleep(500);
//                joyBlePrinterClient.joyBlePrinter_SetAutoSleepTime(3);
//                SystemClock.sleep(500);
//                joyBlePrinterClient.joyBlePrinter_GetAutoSleepTime(new joyBlePrinterClient.joyBlePrinter_AutoSleepTimeCallback() {
//                    @Override
//                    public void onGetAutoSleepTime(int nMin) {
//                        Log.e("tag","auto sleep = "+nMin);
//                    }
//                });




                Bitmap mBmp = BitmapFactory.decodeResource(this.getResources(), R.mipmap.t02);
                joyBlePrinterClient.joyBlePrinter_SetBitbmp(mBmp, blattice,false);
                joyBlePrinterClient.joyBlePrinter_StartPrintting(2);
                F_DispMessage("正处理数据", false);
            }
            else
            {
                F_DispMessage("请先选择打印机", false);
                HiddenmesageBox();
            }

        }
    }


    private void F_DispMessage(String str, boolean bDispBtn)
    {
        if(binding.MessageView.getVisibility() != View.VISIBLE)
        {
            binding.MessageView.setVisibility(View.VISIBLE);
        }
        if(bDispBtn)
        {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)binding.messageTxt.getLayoutParams();
            params.verticalBias=0.20f;
            binding.messageTxt.setLayoutParams(params);
            binding.btnSetting.setVisibility(View.VISIBLE);
        }
        else
        {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)binding.messageTxt.getLayoutParams();
            params.verticalBias=0.5f;
            binding.messageTxt.setLayoutParams(params);
            binding.btnSetting.setVisibility(View.INVISIBLE);
        }
        binding.messageTxt.setText(str);
    }
    private void F_DispMessage(int  n,boolean bDispBtn)
    {
        String str = getString(n);
        F_DispMessage(str,bDispBtn);

    }


    private void HiddenmesageBox()
    {
        hiddenMesageBoxHandler.removeCallbacksAndMessages(null);
        hiddenMesageBoxHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.MessageView.setVisibility(View.GONE);
            }
        },1000);
    }
}