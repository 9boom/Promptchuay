package android.promptchuay.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

// 1. Report Model Class
public class Report implements Serializable {

    private String id;
    private String userId;
    private String name;
    private String contact;
    private String details;
    private Location location;
    private long timestamp;
    private String time;
    private String level;
    private String type;
    private String status;
    private Boolean queued;
    private Boolean isOnDatabase;

    // Constructor เปล่า (จำเป็นสำหรับ Firestore)
    public Report() {
        // ตั้งค่า default เพื่อป้องกัน null
        this.queued = false;
        this.isOnDatabase = false;
        this.timestamp = 0L;
    }

    // Constructor แบบเต็ม
    public Report(String id, String userId, String name,
                  String contact, String details, Location location,
                  long timestamp, String time, String level,
                  String type, String status, Boolean queued, Boolean isOnDatabase) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.contact = contact;
        this.details = details;
        this.location = location;
        this.timestamp = timestamp;
        this.time = time;
        this.level = level;
        this.type = type;
        this.status = status;
        this.queued = queued != null ? queued : false;
        this.isOnDatabase = isOnDatabase != null ? isOnDatabase : false;
    }

    // Getters with Null Safety
    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getContact() {
        return contact;
    }

    @Nullable
    public String getDetails() {
        return details;
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public String getTime() {
        return time;
    }

    @Nullable
    public String getLevel() {
        return level;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @NonNull
    public Boolean getQueued() {
        return queued != null ? queued : false;
    }

    @NonNull
    public Boolean getIsOnDatabase() {
        return isOnDatabase != null ? isOnDatabase : false;
    }

    // Setters with Null Safety
    public void setId(@Nullable String id) {
        this.id = id;
    }

    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void setContact(@Nullable String contact) {
        this.contact = contact;
    }

    public void setDetails(@Nullable String details) {
        this.details = details;
    }

    public void setLocation(@Nullable Location location) {
        this.location = location;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTime(@Nullable String time) {
        this.time = time;
    }

    public void setLevel(@Nullable String level) {
        this.level = level;
    }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    public void setQueued(@Nullable Boolean queued) {
        this.queued = queued != null ? queued : false;
    }

    public void setIsOnDatabase(@Nullable Boolean isOnDatabase) {
        this.isOnDatabase = isOnDatabase != null ? isOnDatabase : false;
    }

}