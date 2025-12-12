package android.promptchuay;

import android.promptchuay.model.Report;

public class SharedManager {
    /*
    คลาส สำหรับทำให้สามารถเข้าถึง คลาส Report ได้จากทุถคลาส ได้แก่ อ่านและแก้ไขข้อมูล จะได้ไม่ตั้งเขียน new keyword
    แล้วโยน obj ไปมา
     */
    /*
    เป็นคลาสหลัก ในการอ่านและแก้ไขข้อมูล Report ของผู้ใช้ เพื่อนำค่าที่ได้ไปใช้ในโค้ดจริง โดยรับมาจาก preferenceManager อีกที
     */
    private static final SharedManager instance = new SharedManager();

    private Report currentReport;

    private SharedManager() {
        currentReport = new Report(); // new แค่ครั้งเดียว
    }

    public static SharedManager getInstance() {
        return instance;
    }
    public Report getSharedReport() {
        return currentReport;
    }
    public void clearSharedReport() {
        currentReport = new Report();
    }
}
