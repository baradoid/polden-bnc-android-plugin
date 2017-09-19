package com.example.murinets.myapplication;

import android.os.Handler;
import android.os.Message;
import android.os.BatteryManager;
import android.provider.Settings;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.unity3d.player.UnityPlayerActivity;
import java.io.RandomAccessFile;
import java.io.IOException;
import android.content.BroadcastReceiver;

import java.net.Socket;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by murinets on 03.10.2016.
 */
public class ftWorker implements SensorEventListener {
    public boolean isEnabled;
    private UnityPlayerActivity activity;
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev = null;
    public static int devCount = 0;
    public static int ftConnTryCnt = 0;
    static public int iavailable = 0;
    public boolean bReadThreadGoing = false;


    public static final int readLength = 512;

    volatile public static String lastString = new String("N/A\r\n");

    private SensorManager mSensorManager = null;
    private Sensor TempSensor = null;

    private volatile int xPos = 0;
    private volatile int yPos = 0;

    //volatile int iCpuTemp = -1488;
    volatile int iBatteryTemp = -1488;
    volatile int iHeadTemp = -1488;
    volatile int iDistance = -1;
    volatile int iPluginVer = 0x0914;
    private int cashCount = 0;

    public int getBatteryTemp() {
        return iBatteryTemp;
    }

    public int getDistance() {
        return iDistance;
    }

    public int getPluginVer() {
        return iPluginVer;
    }

    public int getHeadTemp() {
        return iHeadTemp;
    }

    public int getXpos() {
        return xPos;
    }

    public int getYpos() {
        return yPos;
    }
    //private volatile float cpuTemp = 0;

    final String fpath = "/sdcard/debugServ.txt";
    File configFile = new File(fpath);


    public readThread read_thread = new readThread();

    //public loggerThread logThred = new loggerThread();


    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            iBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
        }
    };

    public ftWorker(UnityPlayerActivity act) {
        activity = act;
//        mSensorManager = (SensorManager) act.getSystemService(act.SENSOR_SERVICE);
//        TempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
//        mSensorManager.registerListener(this, TempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        act.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (configFile.exists() == true) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fpath));
                String SERVERIP = br.readLine(); //"192.168.0.21";
                //logThred.setServerIp(SERVERIP);
                //logThred.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void turnAudioOn() {
        ftDev.write("son\n".getBytes());

    }

    public void turnAudioOff() {
        ftDev.write("soff\n".getBytes());

    }

    public void resetMe() {
        ftDev.write("reset\n".getBytes());
    }

    public int getFan0RPM()
    {
        return 0;

    }

    public int getFan1RPM()
    {
        return 0;

    }

    public int getFan2RPM()
    {
        return 0;

    }

    public int getCashCount()
    {
        return cashCount;
    }

    public boolean enabled() {
        return isEnabled;
    }

    public void rescanFt()
    {
        ftConnTryCnt++;

        try {
            System.out.println(String.format("ft scan try %d\n", ftConnTryCnt));
            try {
// Get FT_Device and Open the port
                if(ftDev != null)
                    return;

                if(ftD2xx == null)
                    ftD2xx = D2xxManager.getInstance(activity);
                devCount = ftD2xx.createDeviceInfoList(activity);

                System.out.println(String.format("devCount: %d\n", devCount));
                for (int i = 0; i < devCount; i++) {
                    ftDev = ftD2xx.openByIndex(activity, i);

                    if ((ftDev != null) && (ftDev.isOpen() == true))
                        break;
                }
////////// Configure the port to UART
                if ((ftDev != null) && (ftDev.isOpen() == true)) {
                    D2xxManager.FtDeviceInfoListNode fdiln = ftDev.getDeviceInfo();
                    System.out.println(String.format("device desc: %s\n sn: %s\n", fdiln.description, fdiln.serialNumber));
                    // Set Baud Rate
                    ftDev.setBaudRate(115200);
                    ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8,
                                                 D2xxManager.FT_STOP_BITS_1,
                                                 D2xxManager.FT_PARITY_NONE);
                    ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte)0x0b, (byte)0x0d);

                    if(bReadThreadGoing == false)
                    {
                        bReadThreadGoing = true;
                        read_thread.start();

                    }
                }
                else {
                    System.out.println("no ft device open\n");
                }
            } catch (D2xxManager.D2xxException ex) {
                ex.printStackTrace();
                ftDev.close();
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }



//    private void updateCpuTemp()
//    {
//        try {
//            RandomAccessFile raf = new RandomAccessFile("/sys/class/hwmon/hwmon0/temp1_input", "r");
//            if(raf != null) {
//                String load = raf.readLine();
//                if(load != null) {
//                    cpuTemp = Integer.parseInt(load);
//                }
//                raf.close();
//
//                raf = null;
//            }
//
//        } catch (Exception ex) {
//
//        }
//    }

    private class readThread  extends Thread
    {


        readThread(){
        }

        @Override
        public void run()
        {
            int pollPeroiod = 2;
            int cpuTempSendPeriod = 2000;
            //int cpuTempSendPeriodCnt = cpuTempSendPeriod/pollPeroiod;

            //int periodNum = 0;
            byte[] dataBuf = new byte[readLength];
            char msg[] = new char[200];
            int curMsgInd = 0;
            long lastCpuTempSend = 0;

            ftDev.setLatencyTimer((byte)2);

//            int cpuTempUpdatePeriodNum = 0;
//            int cpuTempUpdatePeriod = 1000;
//            int cpuTempUpdatePeriodCnt = cpuTempSendPeriod/pollPeroiod;

            while(true == bReadThreadGoing)
            {
//                try {
//                    Thread.sleep(pollPeroiod);
//                } catch (InterruptedException e) {
//                }

                synchronized(ftDev)
                {
                    iavailable = ftDev.getQueueStatus();
                    if (iavailable > 0) {

                        if(iavailable > readLength){
                            iavailable = readLength;
                        }

                        ftDev.read(dataBuf, iavailable, 0);

                        for (int i = 0; i < iavailable; i++) {
                            msg[curMsgInd] = (char)dataBuf[i];
                            if((msg[curMsgInd++] == '\n') || (curMsgInd >= 199)){
                                curMsgInd=0;
                                //parseMessage(msg);
                                //parseMessage(msgStr);
                                lastString = new String(msg);
                                //System.out.println("> " + lastString);
                                try {
                                    xPos = Integer.parseInt(lastString.substring(0, 4), 16);
                                    yPos = Integer.parseInt(lastString.substring(5, 9), 16);

                                    iHeadTemp = Integer.parseInt(lastString.substring(10, 14), 10);
                                    iDistance = Integer.parseInt(lastString.substring(15, 19), 10);
                                    cashCount = Integer.parseInt(lastString.substring(40, 46), 10);


                                } catch (Exception e) {

                                }
                            }
                        }
                    }

//                    cpuTempUpdatePeriodNum++;
//                    if(cpuTempUpdatePeriodNum >= cpuTempUpdatePeriodCnt){
//                        //updateCpuTemp();
//                        cpuTempUpdatePeriodNum = 0;
//                    }


//                    periodNum++;
//                    if(periodNum >= cpuTempSendPeriodCnt) {
//                        //ftDev.write(String.format("%02d\r\n", (int) cpuTemp).getBytes());
//                        ftDev.write(String.format("t=%d\n", getBatteryTemp()).getBytes());
//                        periodNum = 0;
//                    }

                    if( (System.currentTimeMillis()- lastCpuTempSend)>cpuTempSendPeriod) {
                        lastCpuTempSend = System.currentTimeMillis();
                        //ftDev.write(String.format("%02d\r\n", (int) cpuTemp).getBytes());
                        ftDev.write(String.format("t=%d\n", getBatteryTemp()).getBytes());
                    }

                }
            }
        }
    }




    public boolean isConnected()
    {
        boolean ret = false;
        if(ftDev == null)
            ret = false;
        else
            ret = ftDev.isOpen();

        return ret;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        //cpuTemp = event.values[0];
    }



    private class loggerThread  extends Thread {

        public static final int SERVERPORT = 6340;
        String serverIp = "";


        loggerThread() {
            this.setPriority(Thread.MIN_PRIORITY);
        }

        void setServerIp(String svIp){
            serverIp = svIp;
        }

        @Override
        public void run() {
            Socket socket = null;

            while(true) {
                try {
                    if (socket == null) {
                        socket = new Socket(serverIp, SERVERPORT); //"192.168.0.21";
                    } else {
                        if (socket.isConnected() == true) {
                            socket.getOutputStream().write(lastString.getBytes());
                            //socket.getOutputStream().write("\r\n".getBytes());socket.getOutputStream().flush();
                        }
                        else {
                            socket.close();
                            socket = null;
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    socket = null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}

