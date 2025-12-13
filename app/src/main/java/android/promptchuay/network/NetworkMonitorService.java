package android.promptchuay.network;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.promptchuay.PreferenceManager;
import android.promptchuay.SharedManager;
import android.promptchuay.database.FirestoreManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkMonitorService extends Service {
    /*
    Foreground service เพื่อทำ callback ว่าปัจจุบันอุปกรณ์นี้เชื่อมต่ออินเทอร์เน็ตอยู่
    เอามาใช้ประโยชน์ใน MainActivity แต่ถ้าไม่มีเน็ต จะใช้เป็นตัวส่งแบบฟอร์มอัตโนมัติเมื่ออินเทอร์เน็จจะกลับมา
    */
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private PreferenceManager preferenceManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    private static final int NOTIFICATION_ID = 1001;
    public static final String CHANNEL_ID = "network_monitor_channel";

    // Broadcast Actions
    public static final String ACTION_INTERNET_CONNECTED = "android.promptchuay.INTERNET_CONNECTED";
    public static final String ACTION_INTERNET_LOST = "android.promptchuay.INTERNET_LOST";

    @Override
    public void onCreate() {
        super.onCreate();
        // ต้องเรียกทันที โดยเร็ว เดี๋ยว error
        preferenceManager = new PreferenceManager(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Network Monitor",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        setupNetworkCallback();
    }

    private void setupNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // ตรวจสอบการเชื่อมต่ออินเทอร์เน็ตจริงๆ ใน background thread
                executorService.execute(() -> {
                    boolean hasInternet = NetworkUtils.hasInternetConnection(getApplicationContext());

                    if (hasInternet) {
                        // เน็ตกลับมาแล้ว - ส่ง Broadcast (จริงๆ ไม่ได้ใช้)
                        sendBroadcast(new Intent(ACTION_INTERNET_CONNECTED));

                        // ประมวลผล report ที่ค้างไว้
                        processQueuedReport();

                        // เรียก Callback ที่ลงทะเบียนไว้
                        NetworkStateManager.getInstance().notifyInternetConnected();
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                // เน็ตหาย - ส่ง Broadcast
                sendBroadcast(new Intent(ACTION_INTERNET_LOST));

                // เรียก Callback
                NetworkStateManager.getInstance().notifyInternetLost();
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void processQueuedReport() {
        /* เนื่องจากตอนที่ผู้ใช้กด submit form ตอนที่ไม่มีอินเทอร์เน็ตแล้วทำให้ getQueued เป็น true
        ฟังชันก์นี้จะทำงานทันทีเมื่ออินเทอร์เน็ตกลับมาอีกรอบ
        */

        //หลักการเหมือนฟังชันก์ storeAndSaveReport ใน MainActivity เลย แต่เรามาทำใน Foreground
        if (SharedManager.getInstance().getSharedReport().getQueued()) {
            if (!preferenceManager.report.isReportOnPreferences()) {
                // สร้าง report ใหม่
                FirestoreManager.getInstance().createReport(
                        reportId -> {
                            showToast("สร้างรีพอร์ตสำเร็จ: " + reportId);
                            preferenceManager.report.setQueued(false);
                        },
                        e -> {
                            showToast("เกิดข้อผิดพลาด: " + e.getMessage());
                            preferenceManager.report.setQueued(true);
                        }
                );
            } else if (preferenceManager.report.isReportOnPreferences()) {
                // อัพเดท report ที่มีอยู่
                FirestoreManager.getInstance().updateReport(
                        reportId -> {
                            showToast("อัพเดทรีพอร์ตสำเร็จ");
                            preferenceManager.report.setQueued(false);
                        },
                        e -> {
                            showToast("เกิดข้อผิดพลาด: " + e.getMessage());
                            preferenceManager.report.setQueued(true);
                        }
                );
            }

            // บันทึกข้อมูลลง SharedPreferences
            if (preferenceManager.report.storageReport()) {
                showToast("ขอความช่วยเหลือสำเร็จ");
                preferenceManager.report.setQueued(false);
            } else {
                showToast("เกิดข้อผิดพลาดขณะบันทึกข้อมูลลง SharedPreferences");
                preferenceManager.report.setQueued(true);
            }
        }
    }

    private void showToast(String message) {
        mainHandler.post(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Network Monitor Service")
                .setContentText("เช็คสถานะอินเทอร์เน็ตและระบบอัตโนมัติ")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}