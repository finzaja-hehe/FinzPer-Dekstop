package com.finzz.hyperdesk;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PAGE = "file:///android_asset/windows.html";
    private static final int FILE_REQ = 1001;

    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private final HashMap<String, String> ICONS = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ubah ke SCREEN_ORIENTATION_PORTRAIT / UNSPECIFIED kalau file aslimu beda
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        web = new WebView(this);
        web.setBackgroundColor(0xFF02010A);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false); // supaya wallpaper video autoplay

        web.addJavascriptInterface(new Bridge(), "Android");

        // Dipakai tombol "Pilih foto / video" di menu Pengaturan
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                    startActivityForResult(Intent.createChooser(i, "Pilih wallpaper"), FILE_REQ);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        web.loadUrl(PAGE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FILE_REQ && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Launcher: tombol back tidak menutup apa-apa
    }

    private String iconB64(String pkg, Drawable d) {
        String c = ICONS.get(pkg);
        if (c != null) return c;
        try {
            int sz = 96;
            Bitmap bm = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, sz, sz);
            d.draw(cv);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            ICONS.put(pkg, b64);
            return b64;
        } catch (Exception e) {
            return "";
        }
    }

    public class Bridge {
        @JavascriptInterface
        public String apps() {
            JSONArray arr = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
                for (ResolveInfo r : list) {
                    String p = r.activityInfo.packageName;
                    if (p.equals(getPackageName())) continue;
                    JSONObject o = new JSONObject();
                    o.put("n", r.loadLabel(pm).toString());
                    o.put("p", p);
                    o.put("i", iconB64(p, r.loadIcon(pm)));
                    arr.put(o);
                }
            } catch (Exception e) { /* kembalikan daftar yang sudah terkumpul */ }
            return arr.toString();
        }

        @JavascriptInterface
        public void launch(String pkg) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) startActivity(i);
            } catch (Exception e) { }
        }

        // Buka aplikasi dalam jendela mengambang (freeform).
        // Hanya berhasil di perangkat yang mendukung freeform; selain itu terbuka layar penuh.
        @JavascriptInterface
        public void launchFloat(final String pkg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                        if (i == null) return;
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        DisplayMetrics m = getResources().getDisplayMetrics();
                        int w = m.widthPixels, h = m.heightPixels;
                        ActivityOptions o = ActivityOptions.makeBasic();
                        if (Build.VERSION.SDK_INT >= 24) {
                            o.setLaunchBounds(new Rect(w / 6, h / 8, w * 5 / 6, h * 7 / 8));
                        }
                        startActivity(i, o.toBundle());
                    } catch (Exception e) { }
                }
            });
        }

        // Buka alamat web di browser HP (dipakai tombol ↗ di jendela Browser)
        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (url == null || !url.startsWith("http")) return;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception e) { }
                }
            });
        }
    }
    }
