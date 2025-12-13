[# 🚀 ชื่อโปรเจค

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-25.1-green.svg)
![Platform](https://img.shields.io/badge/platform-Android-lightgrey.svg)

**คำอธิบายสั้น ๆ เกี่ยวกับโปรเจคนี้**

---

## 📋 สารบัญ
- [ความต้องการสำหรับผู้ที่ต้องการแก้ไขโค้ดแอปและสร้างแอปใหม่](#ความต้องการสำหรับผู้ที่ต้องการแก้ไขโค้ดแอปและสร้างแอปใหม่)
- [ไฟล์ติดตั้ง](#ไฟล์ติดตั้ง)
- [สนับสนุนผู้พัฒนา](#สนับสนุนผู้พัฒนา)

---

## 🛠 ความต้องการสำหรับผู้ที่ต้องการแก้ไขโค้ดแอปและสร้างแอปใหม่
### * Google Services
เพื่อให้ firestore database สามารถใช้งานได้ รับ
google-services.json จาก firebase console ตอน Add app
หลังจากนั้นให้นำมาวางใน app/ จะเป็น app/google-services.json

### * Google Map Api Key
ในส่วนของ GOOGLE_MAP_API_KEY_HERE ใน AndroidManifest.xml ต้องสมัคร https://cloud.google.com/maps-platform แล้วนำ key มาแทนที่

```xml
<meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="GOOGLE_MAP_API_KEY_HERE"/>
```



## 📦 ไฟล์ติดตั้ง

### ดาวน์โหลดเวอร์ชันล่าสุด
| ระบบปฏิบัติการ | เวอร์ชัน | ดาวน์โหลด                                 |
|----------------|----------|-------------------------------------------|
| Android        | 25.1     | [ดาวน์โหลด (.apk)](media/app-release.apk) |

---

## 💖 สนับสนุนผู้พัฒนา

เผื่อสนับสนุนค่าขนมและโปรเจคในอนาคตครับบ

**PromptChuay เอ้ยย PromptPay:**


![QR PromptPay](media/promptpay.jpeg)

---