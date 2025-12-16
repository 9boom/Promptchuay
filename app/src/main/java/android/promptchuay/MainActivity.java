package android.promptchuay;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.promptchuay.database.FirestoreManager;
import android.promptchuay.location.GPSLocationService;
import android.promptchuay.map.MultiMapsActivity;
import android.promptchuay.map.SingleMapsActivity;
import android.promptchuay.model.Location;
import android.promptchuay.model.Report;
import android.promptchuay.network.NetworkStateManager;
import android.promptchuay.network.NetworkUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import android.promptchuay.network.NetworkMonitorService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity
        implements NetworkStateManager.NetworkStateCallback {
    /*
     * MainActivity หลักของแอป เรียกครั้งแรกเมื่อเปิดแอป จัดการทุกอย่างจากหลายโมดูลที่นี่
     * */
    // UI Components
    private MaterialButton btnVictimMode;
    private MaterialButton btnRescuerMode;
    private ImageView ivConnectionStatus;
    private View victimModeContent;
    private View rescuerModeContent;

    // Victim Mode Components
    private MaterialCardView cardConnectionStatus;
    private ImageView ivConnectionIcon;
    private TextView tvConnectionTitle;
    private TextView tvConnectionDescription;
    private View sosButtonContainer;
    private View btnSendSOS;
    private View btnSendGotHelp;
    private View btnViewAllMap;
    private View sosFormContainer;

    // Sent status card
    private MaterialCardView cardConnectionSentStatus;
    private ImageView ivConnectionSentIcon;
    private TextView tvConnectionSentTitle;
    private TextView tvConnectionSentDescription;

    // Form Components
    private TextInputEditText etName;
    private TextInputEditText etContact;
    private TextInputEditText etDetails;
    private AutoCompleteTextView etLevel;
    private AutoCompleteTextView etType;
    private MaterialButton btnGetLocation;
    private TextView tvLocationInfo;
    private MaterialButton btnSubmitSOS;
    private ImageView btnCloseForm;

    // SharedPreferences Manager
    private PreferenceManager preferencesManager;

    private FirestoreManager firestoreManager;

    // Rescuer Mode
    private TextView tvReportsHeader;
    private RecyclerView rvReports;
    private ReportsAdapter reportsAdapter;

    // Location
    private GPSLocationService locationService;
    private boolean isLocationRequestInProgress = false;

    // Data
    private Mode currentMode = Mode.VICTIM;
    private boolean isOnline = true;
    private Location currentLocation = null;
    private ArrayList<Report> reports = new ArrayList<>();

    private ExecutorService exec;
    String[] severityLevels;
    String[] type;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    public enum Mode {
        VICTIM,
        RESCUER
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        exec = Executors.newSingleThreadExecutor();

//            exec.execute(()-> {
//
//                runOnUiThread(()->{
//
//                });
//            }
//            );
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize
        locationService = new GPSLocationService(this);
        preferencesManager = new PreferenceManager(this);
        firestoreManager = FirestoreManager.getInstance();
        preferencesManager.report.setQueued(false);

        requestLocationPermissions();

        severityLevels = new String[]{
                "🟢 ต่ำ - ไม่เร่งด่วน",
                "🟡 ปานกลาง - ต้องการความช่วยเหลือเร่งด่วน",
                "🔴 วิกฤติ - อันตรายถึงชีวิต"
        };

        type = new String[]{
                "แผ่นดินไหว",
                "น้ำท่วม",
                "สึนามิ",
                "ถูกลักพาตัว",
                "เหตุกราดยิง",
                "อุบัติเหตุ",
                "อื่นๆ"
        };

        // Initialize views
        initializeViews();
        // Setup listeners
        setupListeners();

        // ดำเนินการต่างๆ หากผู้ใช้เคยรายงานสำเร็จแล้วแล้ว
        checkForReportAvalible();

        // โหลดรายงานทั้งหมดจากฐานข้อมูลครั้งแรก
        startupLoadReportsList();

        // Setup RecyclerView
        setupRecyclerView();

        // Set initial mode
        updateMode(Mode.VICTIM);
        updateConnectionStatus(isOnline);

        // Check for saved location when app starts
        checkSavedLocation();

        // จัดการ Foreground
        initializeNetworkStatus();
        checkConnectionSentStatusAndRecognize();
        startNetworkMonitorService();
        NetworkStateManager.getInstance().registerCallback(this);

    }

    private void requestLocationPermissions(){
        // Check that gps is allowed first
        // ตรวจสอบ permission
        if (!locationService.hasLocationPermission()) {
            locationService.requestLocationPermission(this);
            // จะดำเนินการต่อใน onRequestPermissionsResult
            return;
        }

        // ตรวจสอบ GPS
        if (!locationService.isLocationEnabled()) {
            locationService.requestEnableGPS(this);
            // จะดำเนินการต่อใน onActivityResult
            return;
        }
    }

    private void initializeNetworkStatus() {
        if (NetworkUtils.hasInternetConnection(this)) {
            whenInternetConnected();
        } else {
            whenInternetGone();
        }
    }

    private void checkConnectionSentStatusAndRecognize() {
        if (isOnline) {
            // ถ้าตอนนี้อุปกรณ์นี้เชื่อมต่ออินเทอร์เน็ต ให้ไปเช็คในฐานข้อมูลว่า report id ในเครื่องนี้ มีในฐานข้อมูลไหม
            // ถ้ามีแปลว่ารายงานของผู้ใช้ิยู่ในฐานข้อมูลจริง และจะอัปเดต Connection Sent Status Card
            firestoreManager.getReport(
                    report -> {
                        if (report != null) {
                            updateConnectionSentStatus(true);
                        } else {
                            updateConnectionSentStatus(false);
                        }
                    },
                    e -> {
                        Toast.makeText(MainActivity.this, "ไม่พบรายงาน: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateConnectionSentStatus(false);

                    }
            );
        } else {
            // ถ้าออฟไลน์ จะใช้ผลลัพธ์ก่อนหน้านี้ที่บันทึกแทน
            if (SharedManager.getInstance().getSharedReport().getIsOnDatabase()) {
                updateConnectionSentStatus(true);
            } else {
                updateConnectionSentStatus(false);
            }
        }
    }

    private void startNetworkMonitorService() {
        Intent serviceIntent = new Intent(this, NetworkMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void whenInternetConnected() {
        if (SharedManager.getInstance().getSharedReport().getQueued()) {
            storeAndSaveReport();
        }
        checkConnectionSentStatusAndRecognize();
        updateConnectionStatus(true);
    }

    @Override
    public void onInternetConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "เน็ตกลับมาแล้ว", Toast.LENGTH_SHORT).show();
            whenInternetConnected();
        });
    }

    private void whenInternetGone() {
        checkConnectionSentStatusAndRecognize();

        updateConnectionStatus(false);
    }

    @Override
    public void onInternetLost() {
        runOnUiThread(() -> {
            Toast.makeText(this, "เน็ตหาย", Toast.LENGTH_SHORT).show();
            whenInternetGone();
        });
    }

    private void updateSharedReport() {
        SharedManager.getInstance().getSharedReport().setIsOnDatabase(preferencesManager.report.getIsOnDatabase());
        SharedManager.getInstance().getSharedReport().setId(preferencesManager.report.getId());
        SharedManager.getInstance().getSharedReport().setUserId(preferencesManager.report.getUserID());
        SharedManager.getInstance().getSharedReport().setName(preferencesManager.report.getName());
        SharedManager.getInstance().getSharedReport().setContact(preferencesManager.report.getContact());
        SharedManager.getInstance().getSharedReport().setDetails(preferencesManager.report.getDetails());
        SharedManager.getInstance().getSharedReport().setLocation(new Location(preferencesManager.report.getLat(), preferencesManager.report.getLng()));
        SharedManager.getInstance().getSharedReport().setTime(preferencesManager.report.getTime());
        SharedManager.getInstance().getSharedReport().setTimestamp(Long.parseLong(preferencesManager.report.getTimestamp()));
        SharedManager.getInstance().getSharedReport().setLevel(preferencesManager.report.getLevel());
        SharedManager.getInstance().getSharedReport().setStatus(preferencesManager.report.getStatus());
        SharedManager.getInstance().getSharedReport().setType(preferencesManager.report.getType());
        SharedManager.getInstance().getSharedReport().setQueued(preferencesManager.report.getQueued());
    }

    private void checkForReportAvalible() {
        if (preferencesManager.report.isReportOnPreferences()) {
            // ถ้าผู้ใช้ report คร้งก่อนแล้ว จะทำการ อัปเดต SharedManager จาก preferences เอง
            // และจะทำการอัปดด form จาก SharedManaher ต่อ
            // และจะโชว์หน้า form ให้กรอกรายงานทันทีเลย
            updateSharedReport();
            updateForm();
            showSOSForm();
        } else {
            // ถ้าผู้ใช้ไม่เคยรายงาน ก็จะขึ้นหน้าแดงๆ ให้กดก่อนเพื่อแสดงหน้า form รายงาน
            hideSOSForm();
        }
    }

    public static String generateUserId() {
        return UUID.randomUUID().toString();
    }

    private void hideGotHelpBtn() {
        btnSendGotHelp.setVisibility(View.GONE);
    }

    private void showGotHelpBtn() {
        if (preferencesManager.report.isReportOnPreferences()) {
            btnSendGotHelp.setVisibility(View.VISIBLE);
        }
    }

    private void showSOSForm() {
        sosButtonContainer.setVisibility(View.GONE);
        sosFormContainer.setVisibility(View.VISIBLE);
        showGotHelpBtn();
        if (preferencesManager.report.isReportOnPreferences()) {
            // บางครั้งผู้ใช้จะเปิด form แต่ยังไม่เคยรายงาน ดังนั้นต้องเช็คเงื่อนไขนี้ด้วย ไม่งั้น
            // update form จะไม่มีอะไรให้ update
            updateForm();
        }
    }

    private void hideSOSForm() {
        sosButtonContainer.setVisibility(View.VISIBLE);
        sosFormContainer.setVisibility(View.GONE);
        hideGotHelpBtn();
        // เวลาปิด form ก็เคลียด้วย
        clearForm();
    }

    private void updateForm() {
        etName.setText(SharedManager.getInstance().getSharedReport().getName());
        etContact.setText(SharedManager.getInstance().getSharedReport().getContact());
        etDetails.setText(SharedManager.getInstance().getSharedReport().getDetails());
        etLevel.setText(SharedManager.getInstance().getSharedReport().getLevel(), false);
        etType.setText(SharedManager.getInstance().getSharedReport().getType(), false);
        tvLocationInfo.setVisibility(View.VISIBLE);
        currentLocation = new Location(
                SharedManager.getInstance().getSharedReport().getLocation().lat,
                SharedManager.getInstance().getSharedReport().getLocation().lng
        );
        tvLocationInfo.setText(
                getString(
                        R.string.coordinates,
                        String.format(
                                Locale.getDefault(),
                                "%.6f",
                                currentLocation.lat),
                        String.format(
                                Locale.getDefault(),
                                "%.6f",
                                currentLocation.lng)));
        Toast.makeText(this, "ใช้ตำแหน่งที่บันทึกไว้ล่าสุด", Toast.LENGTH_SHORT).show();

    }

    private void clearDropdowns() {
        etLevel.getText().clear();
        etType.getText().clear();
    }
    private void clearForm() {
        etName.getText().clear();
        etContact.getText().clear();
        etDetails.getText().clear();
        clearDropdowns();
        currentLocation = null;
        tvLocationInfo.setVisibility(View.GONE);
        btnGetLocation.setText(getString(R.string.get_location));
    }

    private void requestLocation() {
        if (isLocationRequestInProgress) return;

        isLocationRequestInProgress = true;
        btnGetLocation.setText("กำลังดึงตำแหน่ง...");
        btnGetLocation.setEnabled(false);


        // ตั้งค่า listener
        locationService.setLocationListener(
                new GPSLocationService.LocationListener() {
                    @Override
                    public void onLocationReceived(android.location.Location location) {
                        runOnUiThread(
                                () -> {
                                    isLocationRequestInProgress = false;
                                    btnGetLocation.setEnabled(true);
                                    locationService.stopLocationService(); // หยุด service

                                    if (location != null) {
                                        currentLocation =
                                                new Location(
                                                        location.getLatitude(),
                                                        location.getLongitude());

                                        tvLocationInfo.setText(
                                                getString(
                                                        R.string.coordinates,
                                                        String.format(
                                                                Locale.getDefault(),
                                                                "%.6f",
                                                                currentLocation.lat),
                                                        String.format(
                                                                Locale.getDefault(),
                                                                "%.6f",
                                                                currentLocation.lng)));
                                        tvLocationInfo.setVisibility(View.VISIBLE);
                                        btnGetLocation.setText(getString(R.string.location_set));

                                        Toast.makeText(
                                                        MainActivity.this,
                                                        "ได้ตำแหน่งแล้ว: "
                                                                + location.getLatitude()
                                                                + ", "
                                                                + location.getLongitude(),
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    } else {
                                        btnGetLocation.setText(getString(R.string.get_location));
                                    }
                                });
                    }

                    @Override
                    public void onLocationError(String error) {
                        runOnUiThread(
                                () -> {
                                    isLocationRequestInProgress = false;
                                    btnGetLocation.setEnabled(true);
                                    btnGetLocation.setText(getString(R.string.get_location));

                                    Toast.makeText(
                                                    MainActivity.this,
                                                    "ผิดพลาด: " + error,
                                                    Toast.LENGTH_SHORT)
                                            .show();

                                    useSavedLocation();
                                });
                    }

                    @Override
                    public void onGPSEnabled() {
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    "GPS เปิดแล้ว กำลังดึงตำแหน่ง...",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }
                });

        requestLocationPermissions();

        // ทุกอย่างพร้อม เริ่มดึงตำแหน่ง
        locationService.startLocationService();
    }

    private void initializeViews() {
        // เก็บตัวแปร widget views ต่างๆ สำหรับนำมาใช้จัดการในคลาส
        // เก็บปุ่มโหมด
        btnVictimMode = findViewById(R.id.btnVictimMode);
        btnRescuerMode = findViewById(R.id.btnRescuerMode);

        // ไอคอนสถานะตรงขวาบน
        ivConnectionStatus = findViewById(R.id.ivConnectionStatus);

        // เก็บหน้าการแสดงผล ระหว่าง โหมดกู้ภัย กับ ผู้ประสบภัย
        victimModeContent = findViewById(R.id.victimModeContent);
        rescuerModeContent = findViewById(R.id.rescuerModeContent);

        // เก็บ views ที่อยู่ในโหมดผู้ประสบภับ
        cardConnectionStatus = findViewById(R.id.cardConnectionStatus);
        ivConnectionIcon = findViewById(R.id.ivConnectionIcon);
        tvConnectionTitle = findViewById(R.id.tvConnectionTitle);
        tvConnectionDescription = findViewById(R.id.tvConnectionDescription);
        cardConnectionSentStatus = findViewById(R.id.cardConnectionSentStatus);
        ivConnectionSentIcon = findViewById(R.id.ivConnectionSentIcon);
        tvConnectionSentTitle = findViewById(R.id.tvConnectionSentTitle);
        tvConnectionSentDescription = findViewById(R.id.tvConnectionSentDescription);
        sosButtonContainer = findViewById(R.id.sosButtonContainer);
        btnSendSOS = findViewById(R.id.btnSendSOS);
        btnSendGotHelp = findViewById(R.id.btnSendGotHelp);
        sosFormContainer = findViewById(R.id.sosFormContainer);

        // แบบฟอร์มรายงาน
        etName = findViewById(R.id.etName);
        etContact = findViewById(R.id.etContact);
        etDetails = findViewById(R.id.etDetails);
        etLevel = findViewById(R.id.etLevel);
        etType = findViewById(R.id.etType);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        tvLocationInfo = findViewById(R.id.tvLocationInfo);
        btnSubmitSOS = findViewById(R.id.btnSubmitSOS);
        btnCloseForm = findViewById(R.id.btnCloseForm);

        // viewsในโหมดกู้ภัย
        tvReportsHeader = findViewById(R.id.tvReportsHeader);
        rvReports = findViewById(R.id.rvReports);
        btnViewAllMap = findViewById(R.id.btnViewAllMap);

        setupDropdowns();
    }

    private void setupSeverityDropdown() {
        etLevel = findViewById(R.id.etLevel);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this, android.R.layout.simple_dropdown_item_1line, severityLevels);
        etLevel.setAdapter(adapter);
        etLevel.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selectedSeverity = severityLevels[position];
                        Toast.makeText(
                                        MainActivity.this,
                                        "เลือก: " + selectedSeverity,
                                        Toast.LENGTH_SHORT)
                                .show();

                        // สามารถเก็บค่าไว้ใช้ตอนส่งข้อมูลได้
                        // เช่น int severityLevel = position;
                    }
                });
    }

    private void setupTypeDropdown() {
        etType = findViewById(R.id.etType);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, type);
        etType.setAdapter(adapter);

        // จัดการเมื่อมีการเลือกรายการ
        etType.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selectedSeverity = type[position];
                        Toast.makeText(
                                        MainActivity.this,
                                        "เลือก: " + selectedSeverity,
                                        Toast.LENGTH_SHORT)
                                .show();

                        // สามารถเก็บค่าไว้ใช้ตอนส่งข้อมูลได้
                        // เช่น int severityLevel = position;
                    }
                });
    }

    private void deleteMyReport() {
        if (isOnline) {
            firestoreManager.deleteReport(SharedManager.getInstance().getSharedReport().getId(),
                    success -> {
                        Toast.makeText(
                                        MainActivity.this,
                                        "ขอแสดงความยินดีด้วยครับ",
                                        Toast.LENGTH_SHORT)
                                .show();
                    },
                    e -> {
                        Toast.makeText(
                                        MainActivity.this,
                                        "เกิดข้อผิดพลาด: " + e,
                                        Toast.LENGTH_SHORT)
                                .show();
                        Log.d("E:", e + "");
                    }
            );
            preferencesManager.clear();
            SharedManager.getInstance().clearSharedReport();
            hideSOSForm();
        } else {
            Toast.makeText(
                            MainActivity.this,
                            "ต้องใช้อินเทอร์เน็ตเพื่อดำเนินการ",
                            Toast.LENGTH_SHORT)
                    .show();
        }
        checkConnectionSentStatusAndRecognize();
    }

    private void setupRealtimeListener() {
        firestoreManager.setupRealtimeListener(
                reports -> {
                    runOnUiThread(() -> {
                        // อัปเดตรายงานแบบเรียลไทม์
                        MainActivity.this.reports.clear();
                        MainActivity.this.reports.addAll(reports);

                        // อัปเดต RecyclerView
                        reportsAdapter.notifyDataSetChanged();

                        // อัปเดต header
                        updateReportsHeader();
                    });
                },
                e -> {
                    runOnUiThread(() -> {
                        // แสดงข้อผิดพลาดเฉพาะในโหมดผู้ช่วยเหลือ
                        if (currentMode == Mode.RESCUER) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "การเชื่อมต่อแบบเรียลไทม์ขัดข้อง: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        e.printStackTrace();
                    });
                }
        );
    }

    private void setupDropdowns() {
        setupSeverityDropdown();
        setupTypeDropdown();
    }

    private void setupListeners() {
        btnViewAllMap.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!reports.isEmpty()) {
                            Intent intent = new Intent(MainActivity.this, MultiMapsActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    "ข้อมูลว่างเปล่าไม่สามารถโหลดได้",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );
        // Mode switching
        btnVictimMode.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateMode(Mode.VICTIM);
                    }
                });

        btnSendGotHelp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // แสดง Dialog ถามยืนยันก่อนลบ
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("ยืนยันว่าคุณได้รับการช่วยเหลือ")
                                .setMessage("ระบบจะลบรายงานของคุณออกจากฐานข้อมูลคำร้องการช่วยเหลือ ?")
                                .setPositiveButton("ลบ", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteMyReport();
                                    }
                                })
                                .setNegativeButton("ฉันกดผิด", null)
                                .show();
                    }
                }
        );

        btnRescuerMode.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateMode(Mode.RESCUER);
                    }
                });


        // SOS button
        btnSendSOS.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSOSForm();
                    }
                });

        // Form actions
        btnCloseForm.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideSOSForm();
                    }
                });

        btnGetLocation.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocation();
                    }
                });

        btnSubmitSOS.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitSOSReport();
                    }
                });
    }

    private void updateMode(Mode mode) {
        currentMode = mode;

        switch (mode) {
            case VICTIM:
                // Update button styles
                btnVictimMode.setBackgroundColor(ContextCompat.getColor(this, R.color.red_600));
                btnVictimMode.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                btnRescuerMode.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100));
                btnRescuerMode.setTextColor(ContextCompat.getColor(this, R.color.gray_700));

                // Show/hide content
                victimModeContent.setVisibility(View.VISIBLE);
                rescuerModeContent.setVisibility(View.GONE);
                break;

            case RESCUER:
                // Update button styles
                btnVictimMode.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100));
                btnVictimMode.setTextColor(ContextCompat.getColor(this, R.color.gray_700));

                btnRescuerMode.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_600));
                btnRescuerMode.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                // Show/hide content
                victimModeContent.setVisibility(View.GONE);
                rescuerModeContent.setVisibility(View.VISIBLE);

                // Update reports count
                updateReportsHeader();
                break;
        }
    }

    private void updateConnectionSentStatus(boolean isSent) {
        if (isSent) {
            ivConnectionSentIcon.setImageResource(R.drawable.ic_send);
            ivConnectionSentIcon.setColorFilter(ContextCompat.getColor(this, R.color.green_700));

            cardConnectionSentStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.green_50));
            cardConnectionSentStatus.setStrokeColor(ContextCompat.getColor(this, R.color.green_200));

            tvConnectionSentTitle.setText(getString(R.string.report_sent));
            tvConnectionSentDescription.setText(getString(R.string.report_sent_desc));

            SharedManager.getInstance().getSharedReport().setIsOnDatabase(true);
            preferencesManager.report.setIsOnDatabase(true);
        } else {
            ivConnectionSentIcon.setImageResource(R.drawable.ic_send);
            ivConnectionSentIcon.setColorFilter(ContextCompat.getColor(this, R.color.yellow_600));

            cardConnectionSentStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.yellow_50));
            cardConnectionSentStatus.setStrokeColor(ContextCompat.getColor(this, R.color.yellow_200));

            tvConnectionSentTitle.setText(getString(R.string.report_notsent));
            tvConnectionSentDescription.setText(getString(R.string.report_notsent_desc));

            SharedManager.getInstance().getSharedReport().setIsOnDatabase(false);
            preferencesManager.report.setIsOnDatabase(false);
        }
    }

    private void updateConnectionStatus(boolean online) {
        isOnline = online;

        if (online) {
            // Online mode
            ivConnectionStatus.setImageResource(R.drawable.ic_wifi);
            ivConnectionStatus.setColorFilter(ContextCompat.getColor(this, R.color.green_600));

            ivConnectionIcon.setImageResource(R.drawable.ic_wifi);
            ivConnectionIcon.setColorFilter(ContextCompat.getColor(this, R.color.green_700));

            cardConnectionStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.green_50));
            cardConnectionStatus.setStrokeColor(ContextCompat.getColor(this, R.color.green_200));

            tvConnectionTitle.setText(getString(R.string.connection_online));
            tvConnectionDescription.setText(getString(R.string.connection_online_desc));
        } else {
            // Offline mode

            ivConnectionStatus.setImageResource(R.drawable.ic_wifi_off);
            ivConnectionStatus.setColorFilter(ContextCompat.getColor(this, R.color.gray_400));

            ivConnectionIcon.setImageResource(R.drawable.ic_radio);
            ivConnectionIcon.setColorFilter(ContextCompat.getColor(this, R.color.yellow_600));

            cardConnectionStatus.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.yellow_50));
            cardConnectionStatus.setStrokeColor(ContextCompat.getColor(this, R.color.yellow_200));

            tvConnectionTitle.setText(getString(R.string.connection_offline));
            tvConnectionDescription.setText(getString(R.string.connection_offline_desc));
        }
    }

    private void useSavedLocation() {
        // ลองใช้ตำแหน่งที่บันทึกไว้ใน SharedPreferences
        android.location.Location savedLocation = locationService.getLastLocationFromPrefs();

        if (savedLocation != null) {
            currentLocation =
                    new Location(
                            savedLocation.getLatitude(), savedLocation.getLongitude());

            tvLocationInfo.setText(
                    getString(
                            R.string.coordinates,
                            String.format(Locale.getDefault(), "%.6f", currentLocation.lat),
                            String.format(Locale.getDefault(), "%.6f", currentLocation.lng))
                            + " (ตำแหน่งที่บันทึกไว้)");
            tvLocationInfo.setVisibility(View.VISIBLE);
            btnGetLocation.setText(getString(R.string.location_set));

            Toast.makeText(this, "ใช้ตำแหน่งที่บันทึกไว้ล่าสุด", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSavedLocation() {
        // ตรวจสอบว่ามีตำแหน่งบันทึกไว้หรือไม่
        if (locationService.hasSavedLocation()) {
            android.location.Location savedLocation = locationService.getLastLocationFromPrefs();
            if (savedLocation != null) {
                currentLocation =
                        new Location(
                                savedLocation.getLatitude(), savedLocation.getLongitude());

                // แจ้งเตือนว่ามีตำแหน่งบันทึกไว้
                Toast.makeText(this, "มีตำแหน่งที่บันทึกไว้พร้อมใช้งาน", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitSOSReport() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String details = etDetails.getText().toString().trim();
        String level = etLevel.getText().toString().trim();
        String type = etType.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกชื่อ-นามสกุล", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contact.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกช่องทางติดต่อ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (details.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกรายละเอียดสถานการณ์", Toast.LENGTH_SHORT).show();
            return;
        }

        if (level.isEmpty()) {
            Toast.makeText(this, "กรุณาเลือกระดับการร้องขอของคุณ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (type.isEmpty()) {
            Toast.makeText(this, "กรุณาเลือกประเภท SOS", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLocation == null) {
            Toast.makeText(this, "คุณไม่ได้ระบุตำแหน่ง GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new report
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm น.", new Locale("th", "TH"));
        String dateTime = dateTimeFormat.format(new Date());

        //reports.add(0, newReport);
        SharedManager.getInstance().getSharedReport().setUserId(generateUserId());
        SharedManager.getInstance().getSharedReport().setName(name);
        SharedManager.getInstance().getSharedReport().setContact(contact);
        SharedManager.getInstance().getSharedReport().setDetails(details);
        SharedManager.getInstance().getSharedReport().setLocation(currentLocation);
        SharedManager.getInstance().getSharedReport().setTime(dateTime);
        SharedManager.getInstance().getSharedReport().setLevel(level);
        SharedManager.getInstance().getSharedReport().setStatus("รอความช่วยเหลือ");
        SharedManager.getInstance().getSharedReport().setType(type);
        SharedManager.getInstance().getSharedReport().setQueued(!isOnline);

        // ดองไว้
        //reportsAdapter.notifyItemInserted(0);
        //updateReportsHeader();

        if (!SharedManager.getInstance().getSharedReport().getQueued()) {
            // queued ถ้าเป็น true แปลว่าผู้ใช้ส่งแบบ form แต่ไม่มีอินเทอร์เน็ต ในกรณีงื่อนไขนี้เช็คว่า
            // ผู้ใช้ส่งแบบ form แบบมีเน็ต
            // จะบันทึกลง preferences และอัปขึ้น firestore database ปกติ
            storeAndSaveReport();
            // set queued false อีกรอบเพื่อความสบายใจ จริงๆไม่จะเป็น
            SharedManager.getInstance().getSharedReport().setQueued(false);
        } else {
            // ใน scope เงื่อนไขนี้ แปลว่า get queued เป็น true จะต้องส่งรายงานอีกทีเมื่ออินเทอร์เน็ตกลับมา
            // การส่งจะไม่ใช่หน้าที่ของฟังชันก์ storeAndSaveReport() อีกต่อไปแต่คือ NetworkMonitorService
            // (Foreground service)
            SharedManager.getInstance().getSharedReport().setQueued(true);
            // get queued เป็น true เพื่อเข้าเงื่อนไขใน NetworkMonitorService
            Toast.makeText(this, "ไม่พบอินเทอร์เน็ต ระบบจะส่งอัตโนมัติเมื่อการเชื่อมต่อกลับมา", Toast.LENGTH_SHORT).show();

        }
    }

    private void storeAndSaveReport() {
        // ฟังชันก์นี้สามารถทำได้ทั้ง สร้างรายงาน และอัปเดตรายงาน ลงฐานข้อมูล
        if (!preferencesManager.report.isReportOnPreferences()) {
            // ถ้ายังไม่เคยรายงานแปลว่าต้องสร้าง รายงาน ใหม่ลงฐานข้อมูล
            firestoreManager.createReport(
                    reportId -> {
                        Toast.makeText(this, "สร้างรีพอร์ตสำเร็จ: " + reportId, Toast.LENGTH_SHORT).show();
                    },
                    e -> {
                        Toast.makeText(this, "เกิดข้อผิดพลาด: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            );
        } else if (preferencesManager.report.isReportOnPreferences()) {
            // ถ้าเคยรายงานแล้ว ต้องอัปเดต ไม่ใช่สร้าง
            firestoreManager.updateReport(
                    reportId -> {
                        Toast.makeText(this, "อัพเดทรีพอร์ตสำเร็จ", Toast.LENGTH_SHORT).show();
                    },
                    e -> {
                        Toast.makeText(this, "เกิดข้อผิดพลาด: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
            );
        }

        // ปิดท้ายด้วย การบันทึกลง preferences เพื่อโหลดข้อมูลเดิมใหม่แม้ปิดแอปได้ในอนาคต
        // ต้องทำขั้นตอนนี้ตอนท้าย เพราะ ในบางกระบวนการของ FirestoreManager และการเช็คเงื่อนไข
        // มันมีการ set shared report id หลังจาก createReport เลยต้อง preferences storageReport
        // หลังจากนั้น
        if (preferencesManager.report.storageReport()) {
            Toast.makeText(this, "ขอความช่วยเหลือสำเร็จ", Toast.LENGTH_SHORT).show();
            hideSOSForm();
        } else {
            Toast.makeText(this, "เกิดข้อผิดพลาดขณะบันทึกข้อมูลลง SharedPreferences", Toast.LENGTH_SHORT).show();
        }
        checkConnectionSentStatusAndRecognize();

    }

    private void startupLoadReportsList() {
        firestoreManager.getAllReports(
                // OnSuccess
                reportsList -> {
                    // ซ่อน ProgressBar
                    // Clear list เดิม
                    reports.clear();

                    // เพิ่มข้อมูลใหม่
                    reports.addAll(reportsList);

                    // Notify adapter
                    reportsAdapter.notifyDataSetChanged();

                    // แสดงจำนวนรายการ
                    Toast.makeText(this,
                            "โหลดรายงาน " + reportsList.size() + " รายการ",
                            Toast.LENGTH_SHORT).show();

                    // ถ้าไม่มีข้อมูล
                    if (reportsList.isEmpty()) {
                        Toast.makeText(this, "ไม่มีรายงาน", Toast.LENGTH_SHORT).show();
                    }
                    setupRealtimeListener();
                },
                // OnFailure
                e -> {
                    // แสดง error
                    Toast.makeText(this,
                            "เกิดข้อผิดพลาด: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    setupRealtimeListener();
                }
        );
    }

    private void setupRecyclerView() {
        reportsAdapter =
                new ReportsAdapter(
                        reports,
                        new ReportsAdapter.OnViewMapClickListener() {
                            @Override
                            public void onViewMapClick(Report report) {
                                Toast.makeText(
                                                MainActivity.this,
                                                "เปิดแผนที่สำหรับ " + report.getName(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                                if (!reports.isEmpty()) {
                                    Intent intent = new Intent(MainActivity.this, SingleMapsActivity.class);
                                    intent.putExtra("report", report);
                                    startActivity(intent);
                                }
                            }
                        });

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(reportsAdapter);
    }

    private void updateReportsHeader() {
        tvReportsHeader.setText(getString(R.string.all_reports, reports.size()));
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GPSLocationService.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ได้รับสิทธิ์การเข้าถึงตำแหน่งแล้ว", Toast.LENGTH_SHORT)
                        .show();

                // ตรวจสอบ GPS
                if (!locationService.isLocationEnabled()) {
                    locationService.requestEnableGPS(this);
                } else {
                    // เริ่มดึงตำแหน่ง
                    locationService.startLocationService();
                }
            } else {
                isLocationRequestInProgress = false;
                btnGetLocation.setEnabled(true);
                btnGetLocation.setText(getString(R.string.get_location));
                Toast.makeText(this, "ต้องการสิทธิ์เข้าถึงตำแหน่ง", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPSLocationService.REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "GPS เปิดแล้ว กำลังดึงตำแหน่ง...", Toast.LENGTH_SHORT).show();
                locationService.startLocationService();
            } else {
                isLocationRequestInProgress = false;
                btnGetLocation.setEnabled(true);
                btnGetLocation.setText(getString(R.string.get_location));
                Toast.makeText(this, "ต้องการ GPS เพื่อระบุตำแหน่ง", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void destroy() {
        exec.shutdown();
        // หยุดการติดตามตำแหน่งเมื่อ Activity ถูกทำลาย
        if (locationService != null) {
            locationService.stopLocationService();
        }

        firestoreManager.removeRealtimeListener();
        NetworkStateManager.getInstance().unregisterCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //ทำให้แอปช้าลงเมื่อกดออกจากหน้า google map
        //แยกออกจาก Main Thread ในอนาคต
        //initializeNetworkStatus();
        checkConnectionSentStatusAndRecognize();
    }


}
