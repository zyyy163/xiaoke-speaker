package com.xiaoke.speaker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * 小科科普助手 - Android APK
 *
 * 核心策略：
 * - WebView 壳，加载远程 Web 版页面
 * - 所有语音逻辑（录音/STT/TTS）走 Web 前端（MediaRecorder + API）
 * - APK 只负责：TTS 朗读（Brige.speak）、唤醒检测、保活
 * - 不依赖 Google SpeechRecognizer（鸿蒙/华为兼容）
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String SERVER_URL = "https://192.168.3.12:3443";

    private WebView webView;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        setupWebView();

        initTTS();
        checkPermissions();
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed(); // 局域网环境自签名证书
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NativeBridge(), "NativeBridge");
        webView.loadUrl(SERVER_URL);
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.CHINESE);
                textToSpeech.setPitch(1.1f);
                textToSpeech.setSpeechRate(0.95f);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { callJS("window.__onTTSStatus('start')"); }
                    @Override public void onDone(String utteranceId) { callJS("window.__onTTSStatus('done')"); }
                    @Override public void onError(String utteranceId) { callJS("window.__onTTSStatus('error')"); }
                });
            }
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean g = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (g) {
                Toast.makeText(this, "麦克风已授权 ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "麦克风权限被拒绝，语音不可用", Toast.LENGTH_LONG).show();
            }
            callJS("window.__onPermissionResult(" + g + ")");
        }
    }

    // ============ JS Bridge ============
    public class NativeBridge {
        @JavascriptInterface
        public void speak(final String text) {
            if (textToSpeech == null || text == null || text.isEmpty()) return;
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_" + System.currentTimeMillis());
                } else {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
            });
        }

        @JavascriptInterface
        public void stopSpeaking() {
            runOnUiThread(() -> {
                if (textToSpeech != null && textToSpeech.isSpeaking()) textToSpeech.stop();
            });
        }

        @JavascriptInterface
        public boolean isSpeaking() {
            return textToSpeech != null && textToSpeech.isSpeaking();
        }

        @JavascriptInterface
        public boolean hasPermission() {
            return ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public void requestPermission() {
            runOnUiThread(() -> checkPermissions());
        }

        @JavascriptInterface
        public String getPlatform() {
            return "android_native";
        }
    }

    private void callJS(final String script) {
        runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(script, null);
                } else {
                    webView.loadUrl("javascript:" + script);
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
