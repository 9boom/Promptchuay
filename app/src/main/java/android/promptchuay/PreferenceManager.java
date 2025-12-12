package android.promptchuay;

import android.content.Context;
import android.content.SharedPreferences;
import android.promptchuay.model.Report;
import android.util.Log;

public class PreferenceManager {
    /*
    สำหรับจัดการการบันทึกและโหลดข้อมูลรายงานของเครื่องผู้ใช้บนอุปกรณ์นี้ แม้ออกแอปเข้าใหม่ข้อมูลก็จะไม่หายไป
    แล้วนำค่าที่ได้ไปโหลดลง SharedManager เพื่อนำไปใช้ต่อไป
    (!!! ยกเว้นลบแอปหรือล้างข้อมูล) และไม่เกี่ยวกับ Database server
     */
    private Context context;
    public static final String SECTION = "MY_REPORTS_PREF";
    private SharedPreferences prefs;
    public ReportPreferencesManager report;

    public PreferenceManager(Context context) {
        this.context = context;
        report = new ReportPreferencesManager(this);
    }

    public void addData(String key, String value) {
        prefs = context.getSharedPreferences(SECTION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String readData(String key) {
        prefs = context.getSharedPreferences(SECTION, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }
    
    public Boolean isKeyAvalible(String key){
        prefs = context.getSharedPreferences(SECTION, Context.MODE_PRIVATE);
        String check = prefs.getString(key, null);
        if (check != null){
            return true;
        }
        return false;
    }

    public void deleteData(String key) {
        prefs = context.getSharedPreferences(SECTION, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }

    public void clear() {
        prefs = context.getSharedPreferences(SECTION, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static class ReportPreferencesManager {
        PreferenceManager preferencesManager;
        public ReportPreferencesManager(PreferenceManager preferencesManager) {
            this.preferencesManager = preferencesManager;
        }

        public boolean storageReport() {
            Report report = SharedManager.getInstance().getSharedReport();

            preferencesManager.addData("ID", report.getId());
            preferencesManager.addData("USER_ID", report.getUserId());
            preferencesManager.addData("NAME", report.getName());
            preferencesManager.addData("CONTACT", report.getContact());
            preferencesManager.addData("DETAIL", report.getDetails());
            Log.d("DEBUGGGGG", "WTF is going on at " + report.getLocation().lat + " and " + report.getLocation().lng);
            preferencesManager.addData("LOCATION_LAT", String.valueOf(report.getLocation().lat));
            preferencesManager.addData("LOCATION_LNG", String.valueOf(report.getLocation().lng));
            preferencesManager.addData("TIME", report.getTime());
            preferencesManager.addData("TIMESTAMP", String.valueOf(report.getTimestamp()));
            preferencesManager.addData("LEVEL", report.getLevel());
            Log.d("DEBUGGGGG", "" + report.getLevel());

            preferencesManager.addData("TYPE", report.getType());
            preferencesManager.addData("STATUS", report.getStatus());
            Log.d("DEBUGGGGG", "" + report.getStatus());

            preferencesManager.addData("QUEUED", String.valueOf(report.getQueued()));
            Log.d("DEBUGGGGG", "" + report.getQueued());

            if (isReportOnPreferences()){
                return true;
            } else {
                return false;
            }
        }
        
        public Boolean isReportOnPreferences() {
            if (preferencesManager.isKeyAvalible("ID")){
                return true;
            }
            else {
                return false;
            }
        }
        public void setIsOnDatabase(Boolean isOnDB){
            preferencesManager.addData("IS_ON_DB", String.valueOf(isOnDB));
        }

        public Boolean getIsOnDatabase(){
            return (Boolean.parseBoolean(preferencesManager.readData("IS_ON_DB")));
        }

        public String getId() {
            return (preferencesManager.readData("ID"));
        }

        public String getUserID(){
            return (preferencesManager.readData("USER_ID"));
        }

        public String getName() {
            return (preferencesManager.readData("NAME"));
        }
        
        public String getContact() {
            return (preferencesManager.readData("CONTACT"));
        }
        
        public String getDetails() {
            return (preferencesManager.readData("DETAIL"));
        }
        
        public Double getLat() {
            return Double.parseDouble(preferencesManager.readData("LOCATION_LAT"));
        }
        
        public Double getLng() {
            return Double.parseDouble(preferencesManager.readData("LOCATION_LNG"));
        }
        
        public String getTimestamp() {
            return (preferencesManager.readData("TIMESTAMP"));
        }

        public  String getTime(){
            return (preferencesManager.readData("TIME"));
        }
        
        public String getLevel() {
            return (preferencesManager.readData("LEVEL"));
        }
        
        public String getType() {
            return (preferencesManager.readData("TYPE"));
        }
        
        public String getStatus() {
            return (preferencesManager.readData("STATUS"));
        }
        
        public Boolean getQueued() {
            return Boolean.parseBoolean(preferencesManager.readData("QUEUED"));
        }

        public void setId(String id) {
            preferencesManager.addData("ID", id);
        }

        public void setUserId(String userId) {
            preferencesManager.addData("USER_ID", userId);
        }

        public void setName(String name) {
            preferencesManager.addData("NAME", name);
        }

        public void setContact(String contact) {
            preferencesManager.addData("CONTACT", contact);
        }

        public void setDetails(String details) {
            preferencesManager.addData("DETAIL", details);
        }

        public void setLat(Double lat) {
            preferencesManager.addData("LOCATION_LAT", String.valueOf(lat));
        }

        public void setLng(Double lng) {
            preferencesManager.addData("LOCATION_LNG", String.valueOf(lng));
        }

        public void setTimestamp(String timestamp) {
            preferencesManager.addData("TIMESTAMP", timestamp);
        }

        public void setTime(String time) {
            preferencesManager.addData("TIME", time);
        }

        public void setLevel(String level) {
            preferencesManager.addData("LEVEL", level);
        }

        public void setType(String type) {
            preferencesManager.addData("TYPE", type);
        }

        public void setStatus(String status) {
            preferencesManager.addData("STATUS", status);
        }

        public void setQueued(Boolean queued) {
            preferencesManager.addData("QUEUED", String.valueOf(queued));
        }
    }
}
