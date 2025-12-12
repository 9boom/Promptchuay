package android.promptchuay.database;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.promptchuay.SharedManager;
import android.promptchuay.model.Report;

import java.util.ArrayList;
import java.util.List;

public class FirestoreManager {
    /*
    โค้ดระบบหลัก interface ในการจัดการ database server โดยใช้ firebase firestore
    สำหรับ ลบ... อ่าน... เขียน... และอัปเดตข้อมูลรายงาน
     */
    private static FirestoreManager instance;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private static final String REPORTS_COLLECTION = "reports";

    // Singleton Pattern
    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    // Interface สำหรับ Callback
    public interface OnSuccessListener<T> {
        void onSuccess(T result);
    }

    public interface OnFailureListener {
        void onFailure(Exception e);
    }

    // สร้าง Report ใหม่
    public void createReport(OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        Report sharedReport = SharedManager.getInstance().getSharedReport();

        if (sharedReport == null) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("Shared report is null"));
            }
            return;
        }

        String reportId = db.collection(REPORTS_COLLECTION).document().getId();
        sharedReport.setId(reportId);
        sharedReport.setTimestamp(System.currentTimeMillis());

        db.collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(sharedReport)
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) {
                        onSuccess.onSuccess(reportId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }

    // ดึง Report ตาม ID
    public void getReport(OnSuccessListener<Report> onSuccess, OnFailureListener onFailure) {
        Report shared = SharedManager.getInstance().getSharedReport();

        if (shared == null || shared.getId() == null) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("Report ID is not set"));
            }
            return;
        }

        db.collection(REPORTS_COLLECTION)
                .document(shared.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Report report = documentSnapshot.toObject(Report.class);
                        if (onSuccess != null) {
                            onSuccess.onSuccess(report);
                        }
                    } else {
                        if (onFailure != null) {
                            onFailure.onFailure(new Exception("Report not found"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }

    // ดึง Reports ทั้งหมดของผู้ใช้ (ไม่ได้ใช้ เก็บไว้ประดับ)
    public void getUserReports(String userId, OnSuccessListener<List<Report>> onSuccess, OnFailureListener onFailure) {
        if (userId == null || userId.trim().isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("User ID is null or empty"));
            }
            return;
        }

        db.collection(REPORTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Report> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Report report = doc.toObject(Report.class);
                        reports.add(report);
                    }
                    if (onSuccess != null) {
                        onSuccess.onSuccess(reports);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }

    // อัพเดท Report
    public void updateReport(OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Report sharedReport = SharedManager.getInstance().getSharedReport();

        if (sharedReport == null || sharedReport.getId() == null) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("Shared report or report id is null"));
            }
            return;
        }

        db.collection(REPORTS_COLLECTION)
                .document(sharedReport.getId())
                .set(sharedReport)
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }

    // ลบ Report
    public void deleteReport(String reportId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (reportId == null || reportId.trim().isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new Exception("Report ID is null or empty"));
            }
            return;
        }

        db.collection(REPORTS_COLLECTION)
                .document(reportId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }

    public void setupRealtimeListener(
            OnSuccessListener<List<Report>> onSuccess,
            OnFailureListener onFailure
    ) {
        listenerRegistration = db.collection(REPORTS_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        if (onFailure != null) {
                            onFailure.onFailure(error);
                        }
                        return;
                    }

                    if (snapshots != null) {
                        List<Report> reports = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Report report = doc.toObject(Report.class);
                            reports.add(report);
                        }

                        if (onSuccess != null) {
                            onSuccess.onSuccess(reports);
                        }
                    }
                });
    }

    public void removeRealtimeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // ดึง Reports ทั้งหมด
    public void getAllReports(OnSuccessListener<List<Report>> onSuccess, OnFailureListener onFailure) {
        db.collection(REPORTS_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Report> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Report report = doc.toObject(Report.class);
                        reports.add(report);
                    }
                    if (onSuccess != null) {
                        onSuccess.onSuccess(reports);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.onFailure(e);
                    }
                });
    }
}