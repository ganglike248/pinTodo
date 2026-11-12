package com.demo.remind;
import android.app.*; import android.os.Bundle;
/** 홍보 영상의 '보통 알림' 역할: am start ... --es t 제목 --es x 내용 --ei id 번호 */
public class Post extends Activity {
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel("r2", "알림", NotificationManager.IMPORTANCE_DEFAULT));
    Notification n = new Notification.Builder(this, "r2")
      .setSmallIcon(getResources().getIdentifier("bell", "drawable", getPackageName()))
      .setContentTitle(getIntent().getStringExtra("t"))
      .setContentText(getIntent().getStringExtra("x"))
      .setColor(0xFF8E96A8).build();
    nm.notify(getIntent().getIntExtra("id", 1), n);
    finish();
  }
}
