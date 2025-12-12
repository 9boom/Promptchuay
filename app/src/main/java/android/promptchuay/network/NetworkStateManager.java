package android.promptchuay.network;

import android.promptchuay.SharedManager;
import android.promptchuay.database.FirestoreManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NetworkStateManager {
    /*
    * สำหรับจัดการ callback ตัวกลางระหว่าง NetworkMonitorService ไปยัง MainActivity
    *  */
    private static NetworkStateManager instance;
    private List<NetworkStateCallback> callbacks;
    private FirestoreManager firestoreManager;

    private NetworkStateManager() {
        callbacks = new ArrayList<>();
    }

    public static synchronized NetworkStateManager getInstance() {
        if (instance == null) {
            instance = new NetworkStateManager();
        }
        return instance;
    }

    // ลงทะเบียน Callback
    public void registerCallback(NetworkStateCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    // ยกเลิกการลงทะเบียน Callback
    public void unregisterCallback(NetworkStateCallback callback) {
        callbacks.remove(callback);
    }

    // เรียก Callback ทั้งหมดเมื่อเน็ตกลับมา
    public void notifyInternetConnected() {
        for (NetworkStateCallback callback : new ArrayList<>(callbacks)) {
            if (callback != null) {

                callback.onInternetConnected();
            }
        }
    }

    // เรียก Callback ทั้งหมดเมื่อเน็ตหาย
    public void notifyInternetLost() {
        for (NetworkStateCallback callback : new ArrayList<>(callbacks)) {
            if (callback != null) {
                callback.onInternetLost();
            }
        }
    }

    // Interface สำหรับ Callback
    public interface NetworkStateCallback {
        void onInternetConnected();
        void onInternetLost();
    }
}