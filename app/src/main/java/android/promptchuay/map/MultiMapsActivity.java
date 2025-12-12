package android.promptchuay.map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.promptchuay.R;
import android.promptchuay.database.FirestoreManager;
import android.promptchuay.model.Report;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;

public class MultiMapsActivity extends FragmentActivity implements OnMapReadyCallback {
    /*
    จัดการระบบแผนที่
     */
    private GoogleMap mMap;
    private ArrayList<Report> reports = new ArrayList<>();
    private ArrayList<Report> green_reports_group = new ArrayList<>();
    private ArrayList<Report> yellow_reports_group = new ArrayList<>();
    private ArrayList<Report> red_reports_group = new ArrayList<>();
    private FirestoreManager firestoreManager;
    private HashMap<Marker, Report> markerMap;
    private boolean isMapReady = false;
    private TextView total_pins_info;
    private TextView total_green_pin_info;
    private TextView total_yellow_pin_info;
    private TextView total_red_pin_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initializedViews();
        setup();
        firestoreManager = FirestoreManager.getInstance();
        setupRealtimeListener();
        markerMap = new HashMap<>();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void clearGroups(){
        green_reports_group.clear();
        yellow_reports_group.clear();
        red_reports_group.clear();
    }
    private void updatePinColorTotals(){
        clearGroups();
    if (reports != null){
        for(Report report : reports){
            switch (Objects.requireNonNull(report.getLevel())){
                case "🟢 ต่ำ - ไม่เร่งด่วน":
                    green_reports_group.add(report);
                    break;
                case "🟡 ปานกลาง - ต้องการความช่วยเหลือเร่งด่วน":
                    yellow_reports_group.add(report);
                    break;
                case "🔴 วิกฤติ - อันตรายถึงชีวิต":
                    red_reports_group.add(report);
                    break;
            }
        }
        total_green_pin_info.setText(getString(R.string.total_green_pins, green_reports_group.size()));
        total_yellow_pin_info.setText(getString(R.string.total_yellow_pins, yellow_reports_group.size()));
        total_red_pin_info.setText(getString(R.string.total_red_pins, red_reports_group.size()));
    }
    }
    private void initializedViews(){
        total_pins_info = findViewById(R.id.tvTotalPins);
        total_green_pin_info = findViewById(R.id.tvGreenPins);
        total_yellow_pin_info = findViewById(R.id.tvYellowPins);
        total_red_pin_info = findViewById(R.id.tvRedPins);
    }
    private void updateTotalNumbers(){
        if (reports != null) {
            total_pins_info.setText(getString(R.string.total_pins_number,reports.size()));
            updatePinColorTotals();
        }
    }
    private void setup(){
        total_pins_info.setVisibility(View.VISIBLE);
    }
    private float getMarkerColor(String colorName) {
        // เปลี่ยนสีตามชื่อสถานที่
        if (colorName.contains("orange")) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        } else if (colorName.contains("blue")) {
            return BitmapDescriptorFactory.HUE_BLUE;
        } else if (colorName.contains("green")) {
            return BitmapDescriptorFactory.HUE_GREEN;
        } else if (colorName.contains("violet")) {
            return BitmapDescriptorFactory.HUE_VIOLET;
        } else if (colorName.contains("red")){
            return BitmapDescriptorFactory.HUE_RED;
        }else if (colorName.contains("yellow")){
            return BitmapDescriptorFactory.HUE_YELLOW;}
        else {
            return BitmapDescriptorFactory.HUE_AZURE;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        isMapReady = true;
        addMarkersToMap();
        mMap.setOnMarkerClickListener(marker -> {
            showLocationDialog(marker);
            return true;
        });
        if (!reports.isEmpty()) {
            Report firstLocation = reports.get(0);
            LatLng firstPos = new LatLng(firstLocation.getLocation().lat, firstLocation.getLocation().lng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPos, 12));
        }
    }

    private void addMarkersToMap() {
        if (!isMapReady || mMap == null || reports == null || reports.isEmpty()) {
            return;
        }
        mMap.clear();
        markerMap.clear();
        for (Report report : reports) {
            addSingleMarker(report);
        }
    }

    private void setupRealtimeListener () {
        firestoreManager.setupRealtimeListener(
                reports -> {
                    runOnUiThread(() -> {
                        if (MultiMapsActivity.this.reports != null) {
                            MultiMapsActivity.this.reports.clear();
                        }
                        MultiMapsActivity.this.reports.addAll(reports);
                        updateTotalNumbers();
                        addMarkersToMap();
                    });
                },
                e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(
                                getApplicationContext(),
                                "การเชื่อมต่อแบบเรียลไทม์ขัดข้อง: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    e.printStackTrace();
                }
        );
    }
    private void addSingleMarker(Report report) {
        String selfColorMarker = "";
        LatLng position = new LatLng(report.getLocation().lat, report.getLocation().lng);
        if (report.getLevel().equals("🟢 ต่ำ - ไม่เร่งด่วน")) {
            selfColorMarker = "green";
        }
        if (report.getLevel().equals("🟡 ปานกลาง - ต้องการความช่วยเหลือเร่งด่วน")) {
            selfColorMarker = "yellow";
        }
        if (report.getLevel().equals("🔴 วิกฤติ - อันตรายถึงชีวิต")) {
            selfColorMarker = "red";
        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(report.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(selfColorMarker)))
        );

        // เก็บความสัมพันธ์ระหว่าง Marker กับ LocationData
        markerMap.put(marker, report);
    }
    private void showLocationDialog(Marker marker) {
        Report report = markerMap.get(marker);
        if (report != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("รายละเอียดผู้ขอความช่วยเหลือ ระดับ " + report.getLevel());

            String info =
                    "ชื่อ: " + report.getName() + "\n" +
                            "รายงานเมื่อ: " + report.getTime() + "\n"+
                            "ละติจูด: " + report.getLocation().lat + "\n" +
                            "ลองจิจูด: " + report.getLocation().lng + "\n"+
                            "ช่องทางติดต่อ: " + report.getContact() + "\n"+
                            "ฉันอยู่ในสถานการณ์: " + report.getType() + "\n"+
                            "รายละเอียด: " + report.getDetails();

            builder.setMessage(info);

            // ปุ่มนำทาง
            builder.setPositiveButton("นำทาง", (dialog, which) -> {
                openGoogleMapsNavigation(report.getLocation().lat, report.getLocation().lng);
            });

            builder.setNeutralButton("คัดลอกตำแหน่ง", (dialog, which) -> {
                String copyText = report.getLocation().lat + ", " + report.getLocation().lng;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("location", copyText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "คัดลอกตำแหน่งแล้ว", Toast.LENGTH_SHORT).show();
            });

            builder.setNegativeButton("ปิด", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    // เพิ่มฟังก์ชันเปิด Google Maps
    private void openGoogleMapsNavigation(double lat, double lng) {
        try {
            // สร้าง URI สำหรับเปิด Google Maps ในโหมดนำทาง
            String uri = "google.navigation:q=" + lat + "," + lng;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // ถ้าไม่มี Google Maps ติดตั้ง ให้เปิดใน browser
            String uri = "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        }
    }

//    // ฟังก์ชันเพิ่มเติม: เพิ่มหมุดใหม่แบบ dynamic
//    public void addNewMarker(String name, double lat, double lng) {
//        Report newLocation = new Report(name, lat, lng);
//        locations.add(newLocation);
//        addSingleMarker(newLocation);
//    }
}