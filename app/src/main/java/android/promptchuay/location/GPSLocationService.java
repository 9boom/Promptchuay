package android.promptchuay.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

public class GPSLocationService {
    /*
    จัดการ GPS ครบจบ
     */
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences prefs;
    private LocationCallback locationCallback;
    private LocationListener listener;
    
    private static final String PREF_NAME = "LocationPref";
    private static final String KEY_LAT = "last_latitude";
    private static final String KEY_LNG = "last_longitude";
    private static final String KEY_TIME = "last_time";
    
    public static final int PERMISSION_REQUEST_CODE = 1001;
    public static final int REQUEST_CHECK_SETTINGS = 1002;
    
    // Interval ในการอัพเดทตำแหน่ง (มิลลิวินาที)
    private static final long UPDATE_INTERVAL = 10000; // 10 วินาที
    private static final long FASTEST_INTERVAL = 5000; // 5 วินาที
    private static final float MIN_DISTANCE = 10; // 10 เมตร
    
    public interface LocationListener {
        void onLocationReceived(Location location);
        void onLocationError(String error);
        void onGPSEnabled(); // เพิ่มสำหรับแจ้งเมื่อ GPS เปิดสำเร็จ
    }
    
    public GPSLocationService(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * ตรวจสอบว่ามี Permission หรือไม่
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * ขอ Permission จากผู้ใช้
     */
    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, 
            new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, 
            PERMISSION_REQUEST_CODE);
    }
    
    /**
     * ตรวจสอบว่า GPS เปิดอยู่หรือไม่
     */
    public boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    /**
     * ตรวจสอบว่า Location Services เปิดอยู่หรือไม่ (GPS หรือ Network)
     */
    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    
    /**
     * ขอให้ผู้ใช้เปิด GPS (แสดง Dialog ให้เปิดโดยตรง)
     */
    public void requestEnableGPS(Activity activity) {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMinUpdateDistanceMeters(MIN_DISTANCE)
            .build();
        
        requestEnableGPS(activity, locationRequest);
    }
    
    /**
     * ขอให้ผู้ใช้เปิด GPS พร้อม LocationRequest ที่กำหนดเอง
     */
    public void requestEnableGPS(Activity activity, LocationRequest locationRequest) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true); // แสดง dialog แม้ GPS ปิดอยู่
        
        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        
        task.addOnSuccessListener(activity, locationSettingsResponse -> {
            // GPS เปิดอยู่แล้ว
            if (listener != null) {
                listener.onGPSEnabled();
            }
        });
        
        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // แสดง dialog ให้ผู้ใช้เปิด GPS
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (Exception sendEx) {
                    if (listener != null) {
                        listener.onLocationError("Cannot open GPS settings: " + sendEx.getMessage());
                    }
                }
            } else {
                if (listener != null) {
                    listener.onLocationError("Location settings error: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * เปิด Settings เพื่อให้ผู้ใช้เปิด Location manually
     */
    public void openLocationSettings(Activity activity) {
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivity(intent);
    }
    
    /**
     * ดึงตำแหน่งล่าสุดที่บันทึกไว้ใน SharedPreferences
     */
    public Location getLastLocationFromPrefs() {
        float lat = prefs.getFloat(KEY_LAT, 0);
        float lng = prefs.getFloat(KEY_LNG, 0);
        long time = prefs.getLong(KEY_TIME, 0);
        
        if (lat == 0 && lng == 0) return null;
        
        Location location = new Location("saved");
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setTime(time);
        return location;
    }
    
    /**
     * บันทึกตำแหน่งลง SharedPreferences
     */
    private void saveLocationToPrefs(Location location) {
        if (location == null) return;
        
        prefs.edit()
            .putFloat(KEY_LAT, (float) location.getLatitude())
            .putFloat(KEY_LNG, (float) location.getLongitude())
            .putLong(KEY_TIME, location.getTime())
            .apply();
    }
    
    /**
     * เริ่มต้น Location Service
     */
    public void startLocationService(LocationListener listener) {
        this.listener = listener;
        
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("Location permission not granted");
            }
            return;
        }
        Log.d("DEV-DEBUG","iLE: "+isLocationEnabled()+" listener:"+listener);
        if (!isLocationEnabled()) {
            if (listener != null) {
                listener.onLocationError("Location services are disabled");
            }
            return;
        }
        
        // ดึงตำแหน่งล่าสุดจากระบบก่อน
        getLastKnownLocation();
        
        // สร้าง LocationRequest
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // ความแม่นยำสูง (GPS + Network)
                UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMinUpdateDistanceMeters(MIN_DISTANCE)
            .setWaitForAccurateLocation(false)
            .build();
        
        // สร้าง LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    saveLocationToPrefs(location);
                    if (GPSLocationService.this.listener != null) {
                        GPSLocationService.this.listener.onLocationReceived(location);
                    }
                }
            }
        };
        
        // เริ่ม request location updates
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }
    
    /**
     * ดึงตำแหน่งล่าสุดจากระบบ (ไม่รอ update ใหม่)
     */
    public void getLastKnownLocation() {
        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("Location permission not granted");
            }
            return;
        }
        
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToPrefs(location);
                        if (listener != null) {
                            listener.onLocationReceived(location);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onLocationError("Failed to get last location: " + e.getMessage());
                    }
                });
        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }
    
    /**
     * หยุด Location Service
     */
    public void stopLocationService() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        listener = null;
    }
    
    /**
     * ล้างข้อมูลตำแหน่งที่บันทึกไว้
     */
    public void clearSavedLocation() {
        prefs.edit()
            .remove(KEY_LAT)
            .remove(KEY_LNG)
            .remove(KEY_TIME)
            .apply();
    }
    
    public void startLocationService() {
    startLocationService(this.listener);
}
    
    /**
     * ตรวจสอบว่ามีข้อมูลตำแหน่งบันทึกไว้หรือไม่
     */
    public boolean hasSavedLocation() {
        return prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG);
    }
    
    /**
     * ตั้งค่า Listener
     */
    public void setLocationListener(LocationListener listener) {
        this.listener = listener;
    }
}