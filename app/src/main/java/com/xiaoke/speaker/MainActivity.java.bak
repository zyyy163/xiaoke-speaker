package com.xiaoke.speaker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String SERVER_URL = "https://192.168.3.12:3443";

    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();

    // 语音识别状态
    private boolean isListening = false;
    private boolean isProcessing = false;

    // 待发送文本（识别结果）
    private String pendingText = null;

    // 语音识别回调
    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            runOnUiThread(() -> callJS("window.__onRecStatus('listening')"));
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            runOnUiThread(() -> callJS("window.__onRecStatus('processing')"));
        }

        @Override
        public void onError(int error) {
            isListening = false;
            String errorMsg;
            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "no_match";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "speech_timeout";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = "busy";
                    break;
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = "audio_error";
                    break;
                default:
                    errorMsg = "error_" + error;
            }
            final String msg = errorMsg;
            runOnUiThread(() -> callJS("window.__onRecError('" + msg + "')"));
        }

        @Override
        public void onResults(Bundle results) {
            isListening = false;
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                final String text = matches.get(0);
                runOnUiThread(() -> callJS("window.__onRecResult('" + jsEscape(text) + "')"));
            } else {
                runOnUiThread(() -> callJS("window.__onRecError('no_match')"));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                final String text = matches.get(0);
                runOnUiThread(() -> callJS("window.__onRecPartial('" + jsEscape(text) + "')"));
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 WebView
        webView = findViewById(R.id.webView);
        setupWebView();

        // 初始化语音识别
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(recognitionListener);

        // 初始化语音合成
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.CHINESE);
                textToSpeech.setPitch(1.1f);
                textToSpeech.setSpeechRate(1.0f);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        runOnUiThread(() -> callJS("window.__onTTSStatus('start')"));
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> callJS("window.__onTTSStatus('done')"));
                    }
                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> callJS("window.__onTTSStatus('error')"));
                    }
                });
            }
        });

        // 检查权限
        checkAndRequestPermissions();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // 重要：忽略 SSL 证书错误（自签名证书）
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(android.webkit.WebView view,
                                           android.webkit.SslErrorHandler handler,
                                           android.net.http.SslError error) {
                // 自签名证书，放行（局域网安全环境）
                handler.proceed();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // 添加 JS 桥
        webView.addJavascriptInterface(new NativeBridge(), "NativeBridge");

        // 加载页面
        webView.loadUrl(SERVER_URL);
    }

    // ============ JS Bridge ============
    public class NativeBridge {

        @JavascriptInterface
        public void startSpeechRec() {
            runOnUiThread(() -> {
                if (isListening || isProcessing) return;

                // 检查权限
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    callJS("window.__onRecError('no_permission')");
                    return;
                }

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                isListening = true;
                isProcessing = true;
                speechRecognizer.startListening(intent);
            });
        }

        @JavascriptInterface
        public void stopSpeechRec() {
            runOnUiThread(() -> {
                if (isListening) {
                    speechRecognizer.stopListening();
                    isListening = false;
                    isProcessing = false;
                }
            });
        }

        @JavascriptInterface
        public void cancelSpeechRec() {
            runOnUiThread(() -> {
                speechRecognizer.cancel();
                isListening = false;
                isProcessing = false;
                callJS("window.__onRecStatus('idle')");
            });
        }

        @JavascriptInterface
        public void speak(final String text) {
            if (textToSpeech == null) return;
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
                if (textToSpeech != null && textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
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
            runOnUiThread(() -> checkAndRequestPermissions());
        }

        @JavascriptInterface
        public String getPlatform() {
            return "android_native";
        }
    }

    // ============ 权限 ============
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "麦克风权限已获取", Toast.LENGTH_SHORT).show();
                callJS("window.__onPermissionResult(true)");
            } else {
                Toast.makeText(this, "麦克风权限被拒绝，语音识别不可用", Toast.LENGTH_LONG).show();
                callJS("window.__onPermissionResult(false)");
            }
        }
    }

    // ============ 工具 ============
    private void callJS(final String script) {
        runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(script, null);
                } else {
                    webView.loadUrl("javascript:" + script);
                }
            } catch (Exception e) {
                // 忽略
            }
        });
    }

    private String jsEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
