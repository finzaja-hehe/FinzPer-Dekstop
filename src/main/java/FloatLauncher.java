package com.finzz.hyperdesk;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;

import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Membuka aplikasi HP dalam jendela freeform (mengambang) dan
 * membantu mengaktifkan mode freeform di sistem Android.
 * Letakkan file ini di folder yang sama dengan MainActivity.java.
 */
public final class FloatLauncher {
    private static final String SECURE = "android.permission.WRITE_SECURE_SETTINGS";
    private static int count = 0;

    private FloatLauncher() {}

    /** Buka aplikasi [pkg] dalam jendela mengambang. Mengembalikan "ok" atau pesan gagal. */
    public static String launch(Activity a, String pkg) {
        try {
            Intent i = a.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) return "aplikasi tidak ditemukan";
            // NEW_TASK + MULTIPLE_TASK: tiap buka = jendela baru
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            DisplayMetrics m = a.getResources().getDisplayMetrics();
            int w = (int) (m.widthPixels * 0.55f);
            int h = (int) (m.heightPixels * 0.65f);
            int step = (int) (32 * m.density);
            int n = count++ % 6;                       // jendela bertingkat
            int l = (int) (48 * m.density) + n * step;
            int t = (int) (24 * m.density) + n * step;
            if (l + w > m.widthPixels) l = Math.max(0, m.widthPixels - w);
            if (t + h > m.heightPixels) t = Math.max(0, m.heightPixels - h);

            ActivityOptions o = ActivityOptions.makeBasic();
            o.setLaunchBounds(new Rect(l, t, l + w, t + h));
            try { // mode freeform = 5 (API tersembunyi, boleh gagal)
                Method mm = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
                mm.invoke(o, 5);
            } catch (Throwable ignore) { }

            a.startActivity(i, o.toBundle());
            return "ok";
        } catch (Throwable e) {
            return "gagal: " + e.getMessage();
        }
    }

    /** Status dukungan freeform sebagai JSON untuk ditampilkan di Pengaturan. */
    public static String status(Context c) {
        JSONObject j = new JSONObject();
        try {
            ContentResolver r = c.getContentResolver();
            j.put("sdk", Build.VERSION.SDK_INT);
            j.put("feature", c.getPackageManager()
                    .hasSystemFeature("android.software.freeform_window_management"));
            j.put("secure", c.checkSelfPermission(SECURE) == PackageManager.PERMISSION_GRANTED);
            j.put("freeform", Settings.Global.getInt(r, "enable_freeform_support", 0) == 1);
            j.put("force", Settings.Global.getInt(r, "force_resizable_activities", 0) == 1);
        } catch (Throwable ignore) { }
        return j.toString();
    }

    /** Nyalakan freeform + paksa resize. Butuh izin WRITE_SECURE_SETTINGS (diberi sekali lewat ADB). */
    public static String enable(Context c) {
        try {
            if (c.checkSelfPermission(SECURE) != PackageManager.PERMISSION_GRANTED)
                return "izin belum diberikan (jalankan perintah ADB dulu)";
            ContentResolver r = c.getContentResolver();
            Settings.Global.putInt(r, "enable_freeform_support", 1);
            Settings.Global.putInt(r, "force_resizable_activities", 1);
            return "ok";
        } catch (Throwable e) {
            return "gagal: " + e.getMessage();
        }
    }
}
