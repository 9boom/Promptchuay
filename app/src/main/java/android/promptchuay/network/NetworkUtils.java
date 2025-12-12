package android.promptchuay.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

public class NetworkUtils {
    /*
    ฟังชันก์หลัก ที่เช็คว่ามีเน็ตยัง
     */

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    public static boolean hasInternetConnection(Context context) {
        // ตรวจสอบว่าเชื่อมต่อกับอินเทอร์เน็ตจริงหรือไม่ (ไม่ใช่แค่เชื่อมต่อ WiFi)
        return isInternetAvailable(context) && canPing();
    }

    private static boolean canPing() {
        try {
            Process process = Runtime.getRuntime().exec("ping -c 1 firebase.google.com");
            int returnVal = process.waitFor();
            return returnVal == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
