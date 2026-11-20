package com.finzz.hyperdesk;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class MainActivity extends Activity {

    // Ganti kalau desain barumu bernama lain, misalnya "index3.html"
    private static final String PAGE = "index.html";

    private WebView web;
    private final HashMap<String, String> ICONS = new HashMap<>();

    private static final String JS =
        "try{"
        + "window.icon=function(n){if(!window.Android)return '';"
        + "if(!R){try{R=JSON.parse(Android.apps()).sort(function(a,b){return a.n.localeCompare(b.n)})}catch(e){R=[]}}"
        + "for(var k=0;k<R.length;k++)if(R[k].n===n&&R[k].i)return R[k].i;return ''};"
        + "var _t=window.tile;"
        + "window.tile=function(n){var s=icon(n);if(!s)return _t?_t(n):'';"
        + "return '<span class=\"ib\"><img alt=\"\" src=\"data:image/png;base64,'+s+'\" style=\"width:100%;height:100%;border-radius:10px\"></span>'};"
        + "draw();grid();"
        + "}catch(e){}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        web = new WebView(this);
        web.setBackgroundColor(0xFF06142E);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(JS, null);
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/" + PAGE);
    }

    @Override
    public void onBackPressed() {
        // sengaja kosong
    }

    private String iconB64(Drawable d) {
        int size = 96;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, o);
        return Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
    }

    public class Bridge {

        @JavascriptInterface
        public String apps() {
            try {
                PackageManager pm = getPackageManager();
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                JSONArray arr = new JSONArray();
                for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                    String p = r.activityInfo.packageName;
                    if (p.equals(getPackageName())) continue;
                    JSONObject o = new JSONObject();
                    o.put("n", r.loadLabel(pm).toString());
                    o.put("p", p);
                    String ic = ICONS.get(p);
                    if (ic == null) {
                        try {
                            ic = iconB64(r.loadIcon(pm));
                        } catch (Exception e) {
                            ic = "";
                        }
                        ICONS.put(p, ic);
                    }
                    o.put("i", ic);
                    arr.put(o);
                }
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public void launch(String p) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(p);
                if (i != null) startActivity(i);
            } catch (Exception e) {
            }
        }
    }
}
