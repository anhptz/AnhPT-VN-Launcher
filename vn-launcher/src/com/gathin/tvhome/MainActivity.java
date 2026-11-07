package vn.cleanyourtv.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CleanYourTV Vietnamese Dashboard Launcher v5.0 for Mecool KM2:
 * 1. Top-left editable custom header title (click OK to edit text).
 * 2. Top-right icon-only status bar: Notification (with numeric badge), Wi-Fi, LAN, Wallpaper, Settings.
 * 3. Left Widget: Clean Clock & Calendar without header or countdowns:
 *    - Time HH:mm (Largest)
 *    - Solar Weekday & Date (Medium-large)
 *    - Lunar Date & Can Chi Year (Smallest)
 * 4. Center Widget: Weather (swapped with Market widget).
 * 5. Right Widget: Market info (SJC/9999 Gold, VN-Index & VN30-Index with change indicator, USD/VND).
 * 6. Bottom 2x6 App Grid: Full-bleed (tràn viền) Google TV style banners, bottom-right slot = All Apps.
 * 7. Wallpaper Engine: Auto-rotating free internet wallpapers (Bing Daily / Picsum / Unsplash) or manual selection.
 */
public class MainActivity extends Activity {

    private static final String PREFS_NAME = "vn_tv_dashboard_v5_prefs";
    private static final int PICK_WALLPAPER_REQUEST = 9001;

    private static final int APP_ROWS = 2;
    private static final int APP_COLS = 6;
    private static final int TOTAL_SLOTS = APP_ROWS * APP_COLS; // 12 slots (0..10 apps, 11 = All Apps)
    private static final int ALL_APPS_SLOT = TOTAL_SLOTS - 1;  // Index 11 (bottom-right corner)

    private static final String[] CITY_NAMES = {
        "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
        "Nha Trang", "Huế", "Đà Lạt", "Vinh", "Hạ Long"
    };
    private static final double[] CITY_LATS = {
        21.0285, 10.8231, 16.0544, 20.8449, 10.0452,
        12.2388, 16.4637, 11.9404, 18.6796, 20.9599
    };
    private static final double[] CITY_LONS = {
        105.8542, 106.6297, 108.2022, 106.6881, 105.7469,
        109.1967, 107.5909, 108.4583, 105.6813, 107.0425
    };

    private static final String[] PRIORITY_PKGS = {
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "com.google.android.youtube.tv",
        "vn.mytvnet.b2cott",
        "vn.vtv.vtvgotv",
        "com.vieon.tv",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.spotify.tv.android",
        "org.xbmc.kodi",
        "org.videolan.vlc",
        "com.esaba.downloader",
        "com.android.vending",
        "com.apple.atve.androidtv.appletv",
        "com.soundcloud.android",
        "com.tcl.browser",
        "com.phlox.tvwebbrowser"
    };

    // Curated Free High-Res 1080p Wallpapers (Unsplash CDN + Picsum)
    private static final String[] PRESET_WALLPAPER_NAMES = {
        "1. Vịnh biển & Núi non hùng vĩ (Full HD)",
        "2. Cực quang Bắc Cực đêm đông (Aurora)",
        "3. Đỉnh núi Tuyết lúc Hoàng hôn (Alpine Sunset)",
        "4. Rừng thông Sương mù buổi sớm (Misty Forest)",
        "5. Dải Ngân Hà & Bầu trời sao (Milky Way)",
        "6. Hồ trên núi Phản chiếu (Mountain Lake)",
        "7. Bờ biển Đêm Trăng tĩnh lặng (Night Ocean)",
        "8. Thành phố Hiện đại về đêm (Night City)",
        "9. Sóng màu Trừu tượng tối (Dark Abstract)",
        "10. Ảnh đẹp trong ngày của Microsoft Bing"
    };

    private static final String[] PRESET_WALLPAPER_URLS = {
        "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1920&q=80",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=1920&q=80",
        "BING_DAILY_0"
    };

    private float density;
    private SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkPool = Executors.newFixedThreadPool(3);
    private final Map<String, Bitmap> bannerCache = new HashMap<>();

    // Wallpaper & Theme views
    private ImageView wallpaperImageView;
    private View wallpaperScrimView;
    private View clockLunarCardView;
    private View marketCardView;
    private final List<View> statMiniBoxViews = new ArrayList<>();

    // Top-left editable header
    private LinearLayout brandTitleBtn;
    private TextView brandTitleText;

    // Top-right icon-only status bar views
    private LinearLayout notifBtn;
    private StatusIconView notifIconView;

    private LinearLayout wifiBtn;
    private StatusIconView wifiIconView;

    private LinearLayout lanBtn;
    private StatusIconView lanIconView;

    private LinearLayout wallpaperBtn;
    private StatusIconView wallpaperIconView;

    private LinearLayout settingsBtn;
    private StatusIconView settingsIconView;

    // Left Widget: Clean Clock (Largest), Solar Date (Medium), Lunar Date (Smallest)
    private TextView clockBigText;
    private TextView solarDateWidgetText;
    private TextView lunarDateWidgetText;
    private TextView lunarCanChiSubText;

    // Center Widget: Weather
    private View weatherCard;
    private TextView cityLabel;
    private WeatherIconView mainWeatherIcon;
    private TextView mainTempText;
    private TextView mainConditionText;
    private TextView mainHighLowText;
    private TextView rainProbVal;
    private TextView humidityVal;
    private TextView aqiVal;
    private TextView uvVal;
    private final TextView[] forecastDayText = new TextView[5];
    private final WeatherIconView[] forecastIconView = new WeatherIconView[5];
    private final TextView[] forecastTempText = new TextView[5];

    // Right Widget: Gold, VN-Index, USD
    private TextView marketUpdateText;
    private TextView sjcBuyText, sjcSellText, sjcTrendBadge;
    private TextView ringBuyText, ringSellText, ringTrendBadge;
    private TextView vnIndexPointText, vnIndexChangeBadge;
    private TextView vn30PointText, vn30ChangeBadge;
    private TextView usdVcbText, usdTrendBadge;

    // Bottom 2x6 Full-Bleed App Grid views
    private TextView selectedAppPrompt;
    private final FrameLayout[] slotCards = new FrameLayout[TOTAL_SLOTS];
    private final ImageView[] slotBannerViews = new ImageView[TOTAL_SLOTS];
    private final View[] slotBorderOverlays = new View[TOTAL_SLOTS];
    private final TextView[] slotBottomLabels = new TextView[TOTAL_SLOTS];
    private final String[] activeSlotPkgs = new String[TOTAL_SLOTS];
    private int currentFocusedSlot = 0;

    private final List<String> systemNotifications = new ArrayList<>();
    private boolean lastWifiConnected = false;
    private boolean lastLanConnected = false;
    private String lastWifiSsid = null;

    private final Runnable clockAndStatusTicker = new Runnable() {
        @Override
        public void run() {
            updateClockAndDates();
            updateNetworkAndNotificationStatus();
            checkAutoWallpaperRotation();
            mainHandler.postDelayed(this, 10_000L);
        }
    };

    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bannerCache.clear();
            updateNetworkAndNotificationStatus();
            refreshAppGridSlots();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        View root = buildDashboardLayout();
        setContentView(root);

        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addDataScheme("package");
        registerReceiver(systemReceiver, pkgFilter);

        IntentFilter netFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        netFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(systemReceiver, netFilter);

        loadCachedData();
        loadWallpaperOnStartup();
        applyWidgetOpacityTheme();
        updateClockAndDates();
        updateNetworkAndNotificationStatus();
        refreshAppGridSlots();

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (slotCards[0] != null) {
                    slotCards[0].requestFocus();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainHandler.removeCallbacks(clockAndStatusTicker);
        mainHandler.post(clockAndStatusTicker);
        updateNetworkAndNotificationStatus();
        refreshAppGridSlots();
        fetchLiveWeatherAndMarket();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainHandler.removeCallbacks(clockAndStatusTicker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(systemReceiver);
        } catch (Exception ignored) {
        }
        networkPool.shutdownNow();
    }

    @Override
    public void onBackPressed() {
        // Home screen ignores Back key
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    private GradientDrawable roundedBox(int fillColor, float radiusDp, int strokeWidthPx, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeWidthPx > 0) {
            gd.setStroke(strokeWidthPx, strokeColor);
        }
        return gd;
    }

    private void applyTextShadow(TextView tv) {
        tv.setShadowLayer(dp(3.0f), 0f, dp(1.0f), 0x99000000);
    }

    private int getWidgetGlassFillColor(boolean focused) {
        int pct = prefs.getInt("widget_opacity_pct", 42);
        if (focused) {
            pct = Math.min(92, pct + 14);
        }
        int alpha = Math.max(0, Math.min(255, Math.round(pct * 255f / 100f)));
        // Cool frosted slate-blue glass tint (#1E2636) instead of pitch black
        return (alpha << 24) | 0x001E2636;
    }

    private int getWidgetGlassBorderColor(boolean focused) {
        if (focused) return 0xFFFFFFFF;
        int pct = prefs.getInt("widget_opacity_pct", 42);
        if (pct <= 5) return 0x1AFFFFFF;
        return 0x38FFFFFF;
    }

    private void applyWidgetOpacityTheme() {
        int fill = getWidgetGlassFillColor(false);
        int border = getWidgetGlassBorderColor(false);
        if (clockLunarCardView != null) {
            clockLunarCardView.setBackground(roundedBox(fill, 16, dp(1), border));
        }
        if (weatherCard != null) {
            boolean wf = weatherCard.hasFocus();
            weatherCard.setBackground(roundedBox(getWidgetGlassFillColor(wf), 16,
                    dp(wf ? 2 : 1), getWidgetGlassBorderColor(wf)));
        }
        if (marketCardView != null) {
            marketCardView.setBackground(roundedBox(fill, 16, dp(1), border));
        }

        int pct = prefs.getInt("widget_opacity_pct", 42);
        int pillAlpha = Math.max(18, Math.min(180, Math.round(pct * 1.4f)));
        int pillFill = (pillAlpha << 24) | 0x00283347;
        int pillBorder = pct <= 5 ? 0x00000000 : 0x24FFFFFF;
        for (View box : statMiniBoxViews) {
            box.setBackground(roundedBox(pillFill, 8, dp(1), pillBorder));
        }

        LinearLayout[] topBtns = {notifBtn, wifiBtn, lanBtn, wallpaperBtn, settingsBtn};
        for (LinearLayout b : topBtns) {
            if (b != null) {
                boolean bf = b.hasFocus();
                b.setBackground(roundedBox(getWidgetGlassFillColor(bf), 17,
                        dp(bf ? 2 : 1), getWidgetGlassBorderColor(bf)));
            }
        }

        String wpMode = prefs.getString("wp_mode", "auto_bing");
        if ("none".equals(wpMode)) {
            wallpaperScrimView.setBackgroundColor(0x00000000);
        } else {
            int dimPct = prefs.getInt("wallpaper_dim_pct", 16);
            int dimAlpha = Math.max(0, Math.min(220, Math.round(dimPct * 255f / 100f)));
            wallpaperScrimView.setBackgroundColor(dimAlpha << 24);
        }
    }

    // =========================================================================
    // MAIN LAYOUT BUILDER (WITH WALLPAPER BACKGROUND LAYER)
    // =========================================================================

    private View buildDashboardLayout() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(0xFF0E0F12);

        // Layer 1: Full-screen Wallpaper ImageView
        wallpaperImageView = new ImageView(this);
        wallpaperImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootFrame.addView(wallpaperImageView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Layer 2: Translucent dark scrim so widgets and text stay crisp over any photo
        wallpaperScrimView = new View(this);
        wallpaperScrimView.setBackgroundColor(0x00000000);
        rootFrame.addView(wallpaperScrimView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Layer 3: Foreground Dashboard Content
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(36), dp(14), dp(36), dp(12));
        content.setClipChildren(false);
        content.setClipToPadding(false);
        rootFrame.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. TOP STATUS BAR (Left = Editable Brand Title | Right = Icon-only buttons)
        LinearLayout topBar = buildTopStatusBar();
        LinearLayout.LayoutParams topBarLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36));
        topBarLp.bottomMargin = dp(10);
        content.addView(topBar, topBarLp);

        // 2. MIDDLE 3 WIDGETS ROW:
        //    Left = Clock (Largest) + Solar Date (Medium) + Lunar Calendar (Smallest)
        //    Center = Weather (Swapped to Center)
        //    Right = Market Info: Gold, VN-Index, USD (Swapped to Right)
        LinearLayout cardsRow = new LinearLayout(this);
        cardsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardsRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        cardsRowLp.bottomMargin = dp(10);
        content.addView(cardsRow, cardsRowLp);

        // Left Card: Clock & Calendar (weight 0.96)
        clockLunarCardView = buildClockAndLunarWidget();
        LinearLayout.LayoutParams c1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.96f);
        c1Lp.rightMargin = dp(14);
        cardsRow.addView(clockLunarCardView, c1Lp);

        // Center Card: Weather (weight 1.32)
        weatherCard = buildWeatherWidget();
        LinearLayout.LayoutParams c2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.32f);
        c2Lp.rightMargin = dp(14);
        cardsRow.addView(weatherCard, c2Lp);

        // Right Card: Market Info - Gold + VN-Index + USD (weight 1.12)
        marketCardView = buildMarketWidget();
        LinearLayout.LayoutParams c3Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.12f);
        cardsRow.addView(marketCardView, c3Lp);

        // 3. BOTTOM APP GRID (2 Rows x 6 Columns = 12 Slots, Full-Bleed Google TV Banners)
        LinearLayout dockSection = buildBottomAppGridSection();
        content.addView(dockSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return rootFrame;
    }

    // =========================================================================
    // 1. TOP BAR: EDITABLE TITLE (LEFT) + ICON-ONLY BUTTONS (RIGHT)
    // =========================================================================

    private LinearLayout buildTopStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        // Left: Focusable & Editable Custom Title (blank by default, no Mecool KM2 Smart Dashboard text)
        brandTitleBtn = new LinearLayout(this);
        brandTitleBtn.setOrientation(LinearLayout.HORIZONTAL);
        brandTitleBtn.setGravity(Gravity.CENTER_VERTICAL);
        brandTitleBtn.setPadding(dp(10), dp(4), dp(12), dp(4));
        brandTitleBtn.setBackground(roundedBox(0x00000000, 12, 0, 0));
        brandTitleBtn.setFocusable(true);
        brandTitleBtn.setMinimumWidth(dp(120));
        brandTitleBtn.setMinimumHeight(dp(30));

        brandTitleText = new TextView(this);
        String savedTitle = prefs.getString("custom_header_title", "");
        if (savedTitle.contains("MECOOL KM2")) {
            savedTitle = savedTitle.replace("MECOOL KM2 · SMART DASHBOARD", "")
                    .replace("MECOOL KM2", "").trim();
            prefs.edit().putString("custom_header_title", savedTitle).apply();
        }
        brandTitleText.setText(savedTitle);
        brandTitleText.setTextColor(0xE6F1F5F9);
        brandTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        brandTitleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        brandTitleText.setLetterSpacing(0.05f);
        brandTitleText.setSingleLine(true);
        brandTitleText.setEllipsize(TextUtils.TruncateAt.END);
        brandTitleBtn.addView(brandTitleText);

        brandTitleBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String current = prefs.getString("custom_header_title", "");
                v.setBackground(roundedBox(hasFocus ? 0xCC1E293B : 0x00000000, 12,
                        dp(hasFocus ? 1.8f : 0), hasFocus ? 0xFFFFFFFF : 0x00000000));
                if (hasFocus && current.isEmpty()) {
                    brandTitleText.setText("✎ Nhấn OK để nhập chữ...");
                    brandTitleText.setTextColor(0xFF94A3B8);
                } else {
                    brandTitleText.setText(current);
                    brandTitleText.setTextColor(hasFocus ? 0xFFFFFFFF : 0xE6F1F5F9);
                }
            }
        });
        brandTitleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditHeaderTitleDialog();
            }
        });

        LinearLayout leftWrapper = new LinearLayout(this);
        leftWrapper.setOrientation(LinearLayout.HORIZONTAL);
        leftWrapper.setGravity(Gravity.CENTER_VERTICAL);
        leftWrapper.addView(brandTitleBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(leftWrapper, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Right: Icon-only buttons container
        LinearLayout rightGroup = new LinearLayout(this);
        rightGroup.setOrientation(LinearLayout.HORIZONTAL);
        rightGroup.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(rightGroup, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1A. Notification Icon Button (with numeric badge, NO text)
        notifBtn = createIconOnlyStatusButton();
        notifIconView = new StatusIconView(this, StatusIconView.ICON_BELL);
        notifIconView.setBadgeCount(1);
        notifBtn.addView(notifIconView, new LinearLayout.LayoutParams(dp(22), dp(22)));
        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotificationsDialog();
            }
        });
        rightGroup.addView(notifBtn, new LinearLayout.LayoutParams(dp(36), dp(34)));

        // 1B. Wi-Fi Icon Button (Connected / Disconnected icon state, NO text)
        wifiBtn = createIconOnlyStatusButton();
        wifiIconView = new StatusIconView(this, StatusIconView.ICON_WIFI);
        wifiBtn.addView(wifiIconView, new LinearLayout.LayoutParams(dp(22), dp(22)));
        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = lastWifiConnected
                        ? ("Wi-Fi: Đã kết nối" + (lastWifiSsid != null ? (" (" + lastWifiSsid + ")") : ""))
                        : "Wi-Fi: Không kết nối";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                openWifiSettings();
            }
        });
        LinearLayout.LayoutParams wifiBtnLp = new LinearLayout.LayoutParams(dp(36), dp(34));
        wifiBtnLp.leftMargin = dp(8);
        rightGroup.addView(wifiBtn, wifiBtnLp);

        // 1C. LAN Icon Button (Connected / Disconnected icon state, NO text)
        lanBtn = createIconOnlyStatusButton();
        lanIconView = new StatusIconView(this, StatusIconView.ICON_LAN);
        lanBtn.addView(lanIconView, new LinearLayout.LayoutParams(dp(22), dp(22)));
        lanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = lastLanConnected ? "Mạng LAN: Đã kết nối" : "Mạng LAN: Không kết nối";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                openWifiSettings();
            }
        });
        LinearLayout.LayoutParams lanBtnLp = new LinearLayout.LayoutParams(dp(36), dp(34));
        lanBtnLp.leftMargin = dp(8);
        rightGroup.addView(lanBtn, lanBtnLp);

        // 1D. Wallpaper Settings Icon Button (Icon only, NO text)
        wallpaperBtn = createIconOnlyStatusButton();
        wallpaperIconView = new StatusIconView(this, StatusIconView.ICON_WALLPAPER);
        wallpaperBtn.addView(wallpaperIconView, new LinearLayout.LayoutParams(dp(22), dp(22)));
        wallpaperBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWallpaperSettingsDialog();
            }
        });
        LinearLayout.LayoutParams wpBtnLp = new LinearLayout.LayoutParams(dp(36), dp(34));
        wpBtnLp.leftMargin = dp(8);
        rightGroup.addView(wallpaperBtn, wpBtnLp);

        // 1E. System Settings Icon Button (Icon only, NO text)
        settingsBtn = createIconOnlyStatusButton();
        settingsIconView = new StatusIconView(this, StatusIconView.ICON_SETTINGS);
        settingsBtn.addView(settingsIconView, new LinearLayout.LayoutParams(dp(22), dp(22)));
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSystemSettings();
            }
        });
        LinearLayout.LayoutParams setBtnLp = new LinearLayout.LayoutParams(dp(36), dp(34));
        setBtnLp.leftMargin = dp(8);
        rightGroup.addView(settingsBtn, setBtnLp);

        return bar;
    }

    private LinearLayout createIconOnlyStatusButton() {
        final LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(roundedBox(getWidgetGlassFillColor(false), 17, dp(1), getWidgetGlassBorderColor(false)));
        btn.setFocusable(true);
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(roundedBox(getWidgetGlassFillColor(hasFocus), 17,
                        dp(hasFocus ? 2 : 1), getWidgetGlassBorderColor(hasFocus)));
                v.setScaleX(hasFocus ? 1.08f : 1.0f);
                v.setScaleY(hasFocus ? 1.08f : 1.0f);
            }
        });
        return btn;
    }

    private EditText createDarkTvEditText(String initialText, String hint) {
        final EditText input = new EditText(this);
        input.setText(initialText != null ? initialText : "");
        // Use dark text (0xFF0F172A) on a crisp light box (0xFFF8FAFC) so that BOTH the dialog box
        // AND Android TV's fullscreen keyboard ExtractEditText (which copies textColor onto a white bar)
        // display the typed text with 100% high contrast.
        input.setTextColor(0xFF0F172A);
        input.setHint(hint);
        input.setHintTextColor(0xFF64748B);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(roundedBox(0xFFF8FAFC, 10, dp(2.5f), 0xFF38BDF8));
        if (initialText != null) {
            input.setSelection(initialText.length());
        }
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(roundedBox(0xFFF8FAFC, 10, dp(hasFocus ? 3f : 1.5f),
                        hasFocus ? 0xFF0284C7 : 0xFF94A3B8));
            }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });
        return input;
    }

    private void showEditHeaderTitleDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(14));
        box.setBackground(roundedBox(0xFF161B26, 12, dp(1), 0xFF2D3748));

        String currentVal = prefs.getString("custom_header_title", "");
        if (currentVal.contains("MECOOL KM2")) {
            currentVal = currentVal.replace("MECOOL KM2 · SMART DASHBOARD", "")
                    .replace("MECOOL KM2", "").trim();
            prefs.edit().putString("custom_header_title", currentVal).apply();
        }

        final TextView livePreview = new TextView(this);
        livePreview.setText("Xem trước: " + (currentVal.isEmpty() ? "(Đang để trống)" : currentVal));
        livePreview.setTextColor(0xFF38BDF8);
        livePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        livePreview.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams pvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pvLp.bottomMargin = dp(10);
        box.addView(livePreview, pvLp);

        final EditText input = createDarkTvEditText(currentVal, "Nhập nội dung chữ tùy ý (hoặc để trống)...");
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String txt = s != null ? s.toString().trim() : "";
                livePreview.setText("Xem trước: " + (txt.isEmpty() ? "(Đang để trống)" : txt));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tip = new TextView(this);
        tip.setText("Mẹo: Nhập lời chào, tên gia đình, hoặc nhấn 'Xóa trống' để ẩn hoàn toàn chữ góc trái.");
        tip.setTextColor(0xFF94A3B8);
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tipLp.topMargin = dp(8);
        box.addView(tip, tipLp);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Nhập chữ góc trên bên trái")
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> {
                    String val = input.getText().toString().trim();
                    prefs.edit().putString("custom_header_title", val).apply();
                    brandTitleText.setText(val);
                    brandTitleText.setTextColor(0xE6F1F5F9);
                })
                .setNeutralButton("Xóa trống", (d, w) -> {
                    prefs.edit().putString("custom_header_title", "").apply();
                    brandTitleText.setText("");
                })
                .setNegativeButton("Hủy", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        dialog.show();
    }

    // =========================================================================
    // 2A. LEFT WIDGET: TIME (LARGEST) + SOLAR DATE (MEDIUM) + LUNAR (SMALLEST)
    //     (No header title, no countdown rows)
    // =========================================================================

    private View buildClockAndLunarWidget() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(14), dp(10), dp(14));
        card.setBackground(roundedBox(getWidgetGlassFillColor(false), 16, dp(1), getWidgetGlassBorderColor(false)));

        // 1. TIME (LARGEST & BOLD - Lớn nhất & Đậm nổi bật)
        clockBigText = new TextView(this);
        clockBigText.setText("21:40");
        clockBigText.setTextColor(0xFFFFFFFF);
        clockBigText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76);
        clockBigText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        clockBigText.setLetterSpacing(0.02f);
        clockBigText.setIncludeFontPadding(false);
        clockBigText.setGravity(Gravity.CENTER);
        clockBigText.setShadowLayer(dp(6.0f), 0f, dp(2.0f), 0x99000000);
        card.addView(clockBigText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. SOLAR WEEKDAY & DATE (MEDIUM-LARGE - To vừa)
        solarDateWidgetText = new TextView(this);
        solarDateWidgetText.setText("Thứ Ba, 03 tháng 11, 2026");
        solarDateWidgetText.setTextColor(0xFFFFFFFF);
        solarDateWidgetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        solarDateWidgetText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        solarDateWidgetText.setGravity(Gravity.CENTER);
        solarDateWidgetText.setSingleLine(true);
        applyTextShadow(solarDateWidgetText);
        LinearLayout.LayoutParams solarLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        solarLp.topMargin = dp(8);
        card.addView(solarDateWidgetText, solarLp);

        // Subtle luminous accent divider
        View accentLine = new View(this);
        accentLine.setBackground(roundedBox(0x6694A3B8, 2, 0, 0));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(68), dp(2));
        divLp.topMargin = dp(12);
        divLp.bottomMargin = dp(10);
        card.addView(accentLine, divLp);

        // 3. LUNAR CALENDAR (SMALLEST - Nhỏ nhất)
        lunarDateWidgetText = new TextView(this);
        lunarDateWidgetText.setText("Ngày 25 tháng 9 Âm lịch");
        lunarDateWidgetText.setTextColor(0xFFFDE047);
        lunarDateWidgetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        lunarDateWidgetText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        lunarDateWidgetText.setGravity(Gravity.CENTER);
        lunarDateWidgetText.setSingleLine(true);
        applyTextShadow(lunarDateWidgetText);
        card.addView(lunarDateWidgetText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        lunarCanChiSubText = new TextView(this);
        lunarCanChiSubText.setText("Năm Bính Ngọ");
        lunarCanChiSubText.setTextColor(0xFFE2E8F0);
        lunarCanChiSubText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
        lunarCanChiSubText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        lunarCanChiSubText.setGravity(Gravity.CENTER);
        lunarCanChiSubText.setSingleLine(true);
        applyTextShadow(lunarCanChiSubText);
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.topMargin = dp(2);
        card.addView(lunarCanChiSubText, ccLp);

        return card;
    }

    // =========================================================================
    // 2B. CENTER WIDGET: WEATHER CARD (SWAPPED TO CENTER)
    // =========================================================================

    private View buildWeatherWidget() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(12), dp(15), dp(12));
        card.setBackground(roundedBox(getWidgetGlassFillColor(false), 16, dp(1), getWidgetGlassBorderColor(false)));
        card.setFocusable(true);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(roundedBox(getWidgetGlassFillColor(hasFocus), 16,
                        dp(hasFocus ? 2 : 1), getWidgetGlassBorderColor(hasFocus)));
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCityPickerDialog();
            }
        });

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cityLabel = new TextView(this);
        cityLabel.setText("⌖ Hà Nội");
        cityLabel.setTextColor(0xFFFFFFFF);
        cityLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        cityLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        applyTextShadow(cityLabel);
        topRow.addView(cityLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView weatherTitle = new TextView(this);
        weatherTitle.setText("Thời tiết");
        weatherTitle.setTextColor(0xFFCBD5E1);
        weatherTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        weatherTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(weatherTitle);
        topRow.addView(weatherTitle);

        // Hero Weather Row
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(4);
        card.addView(heroRow, heroLp);

        mainWeatherIcon = new WeatherIconView(this, WeatherIconView.TYPE_SUN);
        heroRow.addView(mainWeatherIcon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        mainTempText = new TextView(this);
        mainTempText.setText("29°");
        mainTempText.setTextColor(0xFFFFFFFF);
        mainTempText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
        mainTempText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mainTempText.setIncludeFontPadding(false);
        mainTempText.setShadowLayer(dp(4.0f), 0f, dp(1.5f), 0x8C000000);
        LinearLayout.LayoutParams tempLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tempLp.leftMargin = dp(8);
        heroRow.addView(mainTempText, tempLp);

        LinearLayout condCol = new LinearLayout(this);
        condCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams condLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        condLp.leftMargin = dp(10);
        heroRow.addView(condCol, condLp);

        mainConditionText = new TextView(this);
        mainConditionText.setText("Trời quang");
        mainConditionText.setTextColor(0xFFFFFFFF);
        mainConditionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mainConditionText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        applyTextShadow(mainConditionText);
        condCol.addView(mainConditionText);

        mainHighLowText = new TextView(this);
        mainHighLowText.setText("Cao 33° · Thấp 25°");
        mainHighLowText.setTextColor(0xFFE2E8F0);
        mainHighLowText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        mainHighLowText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(mainHighLowText);
        condCol.addView(mainHighLowText);

        // 4 Mini Stat Pills
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statsLp.topMargin = dp(8);
        card.addView(statsRow, statsLp);

        statMiniBoxViews.clear();
        rainProbVal = addStatMiniPill(statsRow, "Khả năng mưa", "12%", true);
        humidityVal = addStatMiniPill(statsRow, "Độ ẩm", "76%", true);
        aqiVal = addStatMiniPill(statsRow, "Không khí AQI", "88 · TB", true);
        uvVal = addStatMiniPill(statsRow, "Chỉ số UV", "7 · Cao", false);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.topMargin = dp(8);
        divLp.bottomMargin = dp(6);
        card.addView(div, divLp);

        // 5-Day Forecast Row
        LinearLayout forecastRow = new LinearLayout(this);
        forecastRow.setOrientation(LinearLayout.HORIZONTAL);
        forecastRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(forecastRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String[] defDays = {"T4", "T5", "T6", "T7", "CN"};
        int[] defTypes = {
            WeatherIconView.TYPE_PARTLY_CLOUDY, WeatherIconView.TYPE_SUN,
            WeatherIconView.TYPE_SUN, WeatherIconView.TYPE_PARTLY_CLOUDY,
            WeatherIconView.TYPE_RAIN
        };
        String[] defTemps = {"30° / 22°", "29° / 21°", "30° / 22°", "28° / 21°", "27° / 20°"};

        for (int i = 0; i < 5; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            forecastRow.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView dTv = new TextView(this);
            dTv.setText(defDays[i]);
            dTv.setTextColor(0xFFE2E8F0);
            dTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            dTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            applyTextShadow(dTv);
            col.addView(dTv);

            WeatherIconView icon = new WeatherIconView(this, defTypes[i]);
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(20), dp(20));
            icLp.topMargin = dp(2);
            icLp.bottomMargin = dp(2);
            col.addView(icon, icLp);

            TextView tTv = new TextView(this);
            setForecastTempSpan(tTv, defTemps[i]);
            tTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            applyTextShadow(tTv);
            col.addView(tTv);

            forecastDayText[i] = dTv;
            forecastIconView[i] = icon;
            forecastTempText[i] = tTv;
        }

        return card;
    }

    private void setForecastTempSpan(TextView tv, String raw) {
        int slash = raw.indexOf('/');
        if (slash > 0) {
            SpannableString ss = new SpannableString(raw);
            ss.setSpan(new ForegroundColorSpan(0xFFFFFFFF), 0, slash, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD), 0, slash, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(0xFFCBD5E1), slash, raw.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(ss);
        } else {
            tv.setText(raw);
            tv.setTextColor(0xFFFFFFFF);
        }
    }

    private TextView addStatMiniPill(LinearLayout parent, String label, String defValue, boolean addRightMargin) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(roundedBox(0x38283347, 8, dp(1), 0x24FFFFFF));
        box.setPadding(dp(6), dp(5), dp(6), dp(5));
        statMiniBoxViews.add(box);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        if (addRightMargin) lp.rightMargin = dp(5);
        parent.addView(box, lp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFCBD5E1);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.8f);
        lbl.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        lbl.setSingleLine(true);
        lbl.setEllipsize(TextUtils.TruncateAt.END);
        applyTextShadow(lbl);
        box.addView(lbl);

        TextView val = new TextView(this);
        val.setText(defValue);
        val.setTextColor(0xFFFFFFFF);
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        val.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        val.setSingleLine(true);
        applyTextShadow(val);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vLp.topMargin = dp(1);
        box.addView(val, vLp);

        return val;
    }

    // =========================================================================
    // 2C. RIGHT WIDGET: MARKET INFO (GOLD + VN-INDEX + USD/VND) (SWAPPED TO RIGHT)
    // =========================================================================

    private View buildMarketWidget() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(12), dp(15), dp(12));
        card.setBackground(roundedBox(getWidgetGlassFillColor(false), 16, dp(1), getWidgetGlassBorderColor(false)));

        // Header row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Giá vàng · VN-Index · Ngoại tệ");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        applyTextShadow(title);
        topRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        marketUpdateText = new TextView(this);
        marketUpdateText.setText("Cập nhật 21:40");
        marketUpdateText.setTextColor(0xFFCBD5E1);
        marketUpdateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        marketUpdateText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(marketUpdateText);
        topRow.addView(marketUpdateText);

        // Section 1: Gold Header
        LinearLayout goldHeader = new LinearLayout(this);
        goldHeader.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ghLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ghLp.topMargin = dp(6);
        card.addView(goldHeader, ghLp);

        TextView ghLeft = new TextView(this);
        ghLeft.setText("Vàng · triệu đ/lượng");
        ghLeft.setTextColor(0xFFCBD5E1);
        ghLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ghLeft.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(ghLeft);
        goldHeader.addView(ghLeft, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView ghBuy = new TextView(this);
        ghBuy.setText("Mua");
        ghBuy.setTextColor(0xFFCBD5E1);
        ghBuy.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ghBuy.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ghBuy.setGravity(Gravity.END);
        applyTextShadow(ghBuy);
        goldHeader.addView(ghBuy, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView ghSell = new TextView(this);
        ghSell.setText("Bán");
        ghSell.setTextColor(0xFFCBD5E1);
        ghSell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ghSell.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ghSell.setGravity(Gravity.END);
        applyTextShadow(ghSell);
        goldHeader.addView(ghSell, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Spacer matching the width of the trend icon badge column (22dp + 6dp margin = 28dp)
        View ghTrendSpacer = new View(this);
        goldHeader.addView(ghTrendSpacer, new LinearLayout.LayoutParams(dp(28), dp(1)));

        TextView[] sjcViews = addGoldRow(card, "Vàng miếng SJC", "139,0", "142,0", 1);
        sjcBuyText = sjcViews[0];
        sjcSellText = sjcViews[1];
        sjcTrendBadge = sjcViews[2];

        TextView[] ringViews = addGoldRow(card, "Vàng nhẫn 9999", "138,0", "141,0", 1);
        ringBuyText = ringViews[0];
        ringSellText = ringViews[1];
        ringTrendBadge = ringViews[2];

        // Divider 1
        View div1 = new View(this);
        div1.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams div1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        div1Lp.topMargin = dp(5);
        div1Lp.bottomMargin = dp(5);
        card.addView(div1, div1Lp);

        // Section 2: VN-Index & VN30-Index
        LinearLayout stockHeader = new LinearLayout(this);
        stockHeader.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(stockHeader, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView shLeft = new TextView(this);
        shLeft.setText("Chứng khoán");
        shLeft.setTextColor(0xFFCBD5E1);
        shLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        shLeft.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(shLeft);
        stockHeader.addView(shLeft, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView shRight = new TextView(this);
        shRight.setText("So với phiên trước");
        shRight.setTextColor(0xFFCBD5E1);
        shRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        shRight.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(shRight);
        stockHeader.addView(shRight);

        TextView[] vniViews = addStockIndexRow(card, "VN-Index", "1.699,62", "▼ -11,10 (-0,65%)", false);
        vnIndexPointText = vniViews[0];
        vnIndexChangeBadge = vniViews[1];

        TextView[] vn30Views = addStockIndexRow(card, "VN30-Index", "1.815,60", "▲ +2,19 (+0,12%)", true);
        vn30PointText = vn30Views[0];
        vn30ChangeBadge = vn30Views[1];

        // Divider 2
        View div2 = new View(this);
        div2.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams div2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        div2Lp.topMargin = dp(5);
        div2Lp.bottomMargin = dp(5);
        card.addView(div2, div2Lp);

        // Section 3: USD/VND
        TextView[] usdViews = addTwoColPriceRow(card, "USD/VND (VCB)", "25.744", 1);
        usdVcbText = usdViews[0];
        usdTrendBadge = usdViews[1];

        return card;
    }

    private TextView[] addStockIndexRow(LinearLayout parent, String indexName, String points, String changeStr, boolean isUp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.15f);
        parent.addView(row, lp);

        TextView lbl = new TextView(this);
        lbl.setText(indexName);
        lbl.setTextColor(0xFFFFFFFF);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        lbl.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        lbl.setSingleLine(true);
        applyTextShadow(lbl);
        row.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView ptTv = new TextView(this);
        ptTv.setText(points);
        ptTv.setTextColor(0xFFFFFFFF);
        ptTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        ptTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        applyTextShadow(ptTv);
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptLp.rightMargin = dp(8);
        row.addView(ptTv, ptLp);

        TextView badge = new TextView(this);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.setPadding(dp(7), dp(2), dp(7), dp(2));
        badge.setSingleLine(true);
        applyTextShadow(badge);
        styleStockChangeBadge(badge, ptTv, changeStr, isUp ? 1 : -1);
        row.addView(badge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return new TextView[]{ptTv, badge};
    }

    private void styleStockChangeBadge(TextView badge, TextView pointTv, String text, int dir) {
        badge.setText(text);
        if (dir > 0) {
            pointTv.setTextColor(0xFF4ADE80);
            badge.setTextColor(0xFF4ADE80);
            badge.setBackground(roundedBox(0x3816A34A, 6, dp(1), 0x664ADE80));
        } else if (dir < 0) {
            pointTv.setTextColor(0xFFF87171);
            badge.setTextColor(0xFFF87171);
            badge.setBackground(roundedBox(0x38DC2626, 6, dp(1), 0x66F87171));
        } else {
            pointTv.setTextColor(0xFFFBBF24);
            badge.setTextColor(0xFFFBBF24);
            badge.setBackground(roundedBox(0x38D97706, 6, dp(1), 0x66FBBF24));
        }
    }

    private void styleGoldTrendBadge(TextView badge, int dir) {
        if (dir > 0) {
            badge.setText("▲");
            badge.setTextColor(0xFF4ADE80);
            badge.setBackground(roundedBox(0x3816A34A, 6, dp(1), 0x664ADE80));
        } else if (dir < 0) {
            badge.setText("▼");
            badge.setTextColor(0xFFF87171);
            badge.setBackground(roundedBox(0x38DC2626, 6, dp(1), 0x66F87171));
        } else {
            badge.setText("▬");
            badge.setTextColor(0xFFFBBF24);
            badge.setBackground(roundedBox(0x38D97706, 6, dp(1), 0x66FBBF24));
        }
    }

    private TextView[] addGoldRow(LinearLayout parent, String label, String buy, String sell, int defaultDir) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        parent.addView(row, lp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFF8FAFC);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        lbl.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        lbl.setSingleLine(true);
        applyTextShadow(lbl);
        row.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView bTv = new TextView(this);
        bTv.setText(buy);
        bTv.setTextColor(0xFFFFFFFF);
        bTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        bTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        bTv.setGravity(Gravity.END);
        applyTextShadow(bTv);
        row.addView(bTv, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sTv = new TextView(this);
        sTv.setText(sell);
        sTv.setTextColor(0xFFFFFFFF);
        sTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sTv.setGravity(Gravity.END);
        applyTextShadow(sTv);
        row.addView(sTv, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView trendBadge = new TextView(this);
        trendBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        trendBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        trendBadge.setGravity(Gravity.CENTER);
        trendBadge.setSingleLine(true);
        applyTextShadow(trendBadge);
        styleGoldTrendBadge(trendBadge, defaultDir);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(dp(22), dp(19));
        tbLp.leftMargin = dp(6);
        row.addView(trendBadge, tbLp);

        return new TextView[]{bTv, sTv, trendBadge};
    }

    private TextView[] addTwoColPriceRow(LinearLayout parent, String label, String price, int defaultDir) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.95f);
        parent.addView(row, lp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFF8FAFC);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        lbl.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        lbl.setSingleLine(true);
        applyTextShadow(lbl);
        row.addView(lbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView pTv = new TextView(this);
        pTv.setText(price);
        pTv.setTextColor(0xFFFFFFFF);
        pTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        pTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        pTv.setGravity(Gravity.END);
        applyTextShadow(pTv);
        row.addView(pTv);

        TextView trendBadge = new TextView(this);
        trendBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        trendBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        trendBadge.setGravity(Gravity.CENTER);
        trendBadge.setSingleLine(true);
        applyTextShadow(trendBadge);
        styleGoldTrendBadge(trendBadge, defaultDir);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(dp(22), dp(19));
        tbLp.leftMargin = dp(6);
        row.addView(trendBadge, tbLp);

        return new TextView[]{pTv, trendBadge};
    }

    // =========================================================================
    // 3. BOTTOM 2x6 APP GRID (FULL-BLEED GOOGLE TV BANNERS + ALL APPS BUTTON)
    // =========================================================================

    private LinearLayout buildBottomAppGridSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setClipChildren(false);
        section.setClipToPadding(false);

        // Selected App Prompt Row
        LinearLayout promptRow = new LinearLayout(this);
        promptRow.setOrientation(LinearLayout.HORIZONTAL);
        promptRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams promptLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        promptLp.bottomMargin = dp(6);
        section.addView(promptRow, promptLp);

        selectedAppPrompt = new TextView(this);
        selectedAppPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        selectedAppPrompt.setTextColor(0xFFFFFFFF);
        selectedAppPrompt.setSingleLine(true);
        applyTextShadow(selectedAppPrompt);
        promptRow.addView(selectedAppPrompt, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView holdHint = new TextView(this);
        holdHint.setText("Nhấn giữ OK trên ứng dụng để đổi vị trí");
        holdHint.setTextColor(0xFFCBD5E1);
        holdHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        holdHint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applyTextShadow(holdHint);
        promptRow.addView(holdHint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2 Rows x 6 Columns of Refined 16:9 Google TV Style App Tiles
        for (int row = 0; row < APP_ROWS; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setClipChildren(false);
            rowLayout.setClipToPadding(false);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (row == 0) {
                rowLp.bottomMargin = dp(9);
            }
            section.addView(rowLayout, rowLp);

            for (int col = 0; col < APP_COLS; col++) {
                final int slotIndex = row * APP_COLS + col;
                LinearLayout slotCol = new LinearLayout(this);
                slotCol.setOrientation(LinearLayout.VERTICAL);
                slotCol.setGravity(Gravity.CENTER_HORIZONTAL);
                slotCol.setClipChildren(false);
                slotCol.setClipToPadding(false);
                LinearLayout.LayoutParams slotLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (col < APP_COLS - 1) {
                    slotLp.rightMargin = dp(24);
                }

                FrameLayout card = new FrameLayout(this);
                card.setFocusable(true);
                card.setFocusableInTouchMode(false);
                card.setElevation(dp(4));
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                        dp(128), dp(72));
                slotCol.addView(card, cardLp);

                if (slotIndex == ALL_APPS_SLOT) {
                    StatusIconView gridIcon = new StatusIconView(this, StatusIconView.ICON_ALL_APPS);
                    card.addView(gridIcon, new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER));
                } else {
                    // Full-bleed (tràn viền) ImageView filling 100% of the exact 16:9 tile
                    ImageView bannerIv = new ImageView(this);
                    bannerIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    slotBannerViews[slotIndex] = bannerIv;
                    card.addView(bannerIv, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }

                // Crisp focus ring overlay on top of the full-bleed banner
                View borderOverlay = new View(this);
                slotBorderOverlays[slotIndex] = borderOverlay;
                card.addView(borderOverlay, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                TextView bottomLabel = new TextView(this);
                bottomLabel.setGravity(Gravity.CENTER);
                bottomLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                bottomLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                bottomLabel.setTextColor(0xFFE2E8F0);
                bottomLabel.setSingleLine(true);
                bottomLabel.setEllipsize(TextUtils.TruncateAt.END);
                applyTextShadow(bottomLabel);
                // Hide redundant text under tiles for the clean Google TV Favorite Apps look
                bottomLabel.setVisibility(View.GONE);
                LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bottomLp.topMargin = dp(2);
                slotCol.addView(bottomLabel, bottomLp);

                slotCards[slotIndex] = card;
                slotBottomLabels[slotIndex] = bottomLabel;

                card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            currentFocusedSlot = slotIndex;
                            updateSelectedAppPrompt(slotIndex);
                        }
                        styleAppSlot(slotIndex, hasFocus);
                    }
                });

                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (slotIndex == ALL_APPS_SLOT) {
                            showAllAppsDialog(-1);
                        } else {
                            launchAppSlot(slotIndex);
                        }
                    }
                });

                card.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (slotIndex == ALL_APPS_SLOT) {
                            showAllAppsDialog(-1);
                        } else {
                            showAllAppsDialog(slotIndex);
                        }
                        return true;
                    }
                });

                rowLayout.addView(slotCol, slotLp);
            }
        }

        return section;
    }

    // =========================================================================
    // FULL-BLEED (TRÀN VIỀN) GOOGLE TV BANNER RENDERER
    // =========================================================================

    private Bitmap getOrCreateFullBleedBanner(String pkg) {
        if (pkg == null) return null;
        Bitmap cached = bannerCache.get(pkg);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        // Exact 16:9 aspect ratio (128x72 = 16:9) with Google TV's sleek 6dp corner radius
        int w = dp(128);
        int h = dp(72);
        float radius = dp(6);
        Bitmap raw = renderRawFullBleedBanner(pkg, w, h);
        applySubtleGoogleTvVignette(raw, w, h);
        Bitmap rounded = clipBitmapToRoundedRect(raw, w, h, radius);
        bannerCache.put(pkg, rounded);
        return rounded;
    }

    private void applySubtleGoogleTvVignette(Bitmap bmp, int w, int h) {
        Canvas c = new Canvas(bmp);
        Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float r = (float) Math.hypot(cx, cy);
        vp.setShader(new android.graphics.RadialGradient(
                cx, cy, r,
                new int[]{0x00000000, 0x05000000, 0x22000000},
                new float[]{0.0f, 0.62f, 1.0f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, vp);
    }

    private Bitmap renderRawFullBleedBanner(String pkg, int w, int h) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        if ("__SETTINGS__".equals(pkg)) {
            Paint grad = new Paint(Paint.ANTI_ALIAS_FLAG);
            grad.setShader(new LinearGradient(0, 0, w, h, 0xFF334155, 0xFF1E293B, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, grad);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xFFF8FAFC);
            tp.setTextSize(dp(15));
            tp.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            tp.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = tp.getFontMetrics();
            float ty = h * 0.5f - (fm.ascent + fm.descent) * 0.5f;
            canvas.drawText("⚙  Cài đặt", w * 0.5f, ty, tp);
            return out;
        }

        if ("__ADD_APP__".equals(pkg)) {
            // Exact Google TV Favorite Apps "+" (Add App) tile
            canvas.drawColor(0xFF2B2838);
            Paint plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            plusPaint.setColor(0xFFB8B4C4);
            plusPaint.setStyle(Paint.Style.STROKE);
            plusPaint.setStrokeWidth(dp(2.6f));
            plusPaint.setStrokeCap(Paint.Cap.ROUND);
            float cx = w * 0.5f;
            float cy = h * 0.5f;
            float arm = dp(11);
            canvas.drawLine(cx - arm, cy, cx + arm, cy, plusPaint);
            canvas.drawLine(cx, cy - arm, cx, cy + arm, plusPaint);
            return out;
        }

        PackageManager pm = getPackageManager();

        // 1. Try loading official wide Android TV Banner first
        Drawable bannerDr = null;
        try {
            Intent leanbackIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setPackage(pkg);
            List<ResolveInfo> ris = pm.queryIntentActivities(leanbackIntent, 0);
            if (ris != null) {
                for (ResolveInfo ri : ris) {
                    if (ri.activityInfo != null) {
                        bannerDr = ri.activityInfo.loadBanner(pm);
                        if (bannerDr != null) break;
                    }
                }
            }
            if (bannerDr == null) {
                bannerDr = pm.getApplicationBanner(pkg);
            }
        } catch (Exception ignored) {
        }

        if (bannerDr != null) {
            // If the TV banner is an AdaptiveIconDrawable (Android 8.0+ Google TV standard),
            // draw its background & foreground layers directly without AdaptiveIconDrawable's +25% zoom-in
            if (Build.VERSION.SDK_INT >= 26 && bannerDr instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable aid = (AdaptiveIconDrawable) bannerDr;
                Drawable bg = aid.getBackground();
                Drawable fg = aid.getForeground();
                int bleedX = Math.round(w * 0.08f);
                int bleedY = Math.round(h * 0.08f);
                if (bg != null) {
                    bg.setBounds(-bleedX, -bleedY, w + bleedX, h + bleedY);
                    bg.draw(canvas);
                } else {
                    canvas.drawColor(0xFF1E293B);
                }
                if (fg != null) {
                    int insetX = Math.round(w * 0.04f);
                    int insetY = Math.round(h * 0.04f);
                    fg.setBounds(insetX, insetY, w - insetX, h - insetY);
                    fg.draw(canvas);
                }
                return out;
            }

            int bw = bannerDr.getIntrinsicWidth();
            int bh = bannerDr.getIntrinsicHeight();
            if (bw > bh * 1.15f && !isHarshSplitLegacyBanner(bannerDr)) {
                drawGoogleTvBannerRefined(canvas, bannerDr, w, h);
                return out;
            }
        }

        // 2. Load application icon and render refined Google TV style banner (balanced logo size)
        Drawable iconDr = null;
        try {
            iconDr = pm.getApplicationIcon(pkg);
        } catch (Exception ignored) {
        }

        if (iconDr != null && Build.VERSION.SDK_INT >= 26 && iconDr instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) iconDr;
            Drawable bg = aid.getBackground();
            Drawable fg = aid.getForeground();

            if (bg != null) {
                bg.setBounds(0, 0, w, h);
                bg.draw(canvas);
            } else {
                canvas.drawColor(0xFF1E293B);
            }

            if (fg != null) {
                int fgSize = Math.round(h * 0.78f);
                int left = (w - fgSize) / 2;
                int top = (h - fgSize) / 2;
                fg.setBounds(left, top, left + fgSize, top + fgSize);
                fg.draw(canvas);
            }
            return out;
        }

        // 3. Fallback for legacy BitmapDrawable / squircle icons:
        if (iconDr != null) {
            int iw = Math.max(64, iconDr.getIntrinsicWidth());
            int ih = Math.max(64, iconDr.getIntrinsicHeight());
            Bitmap icBmp = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
            Canvas icCanvas = new Canvas(icBmp);
            iconDr.setBounds(0, 0, iw, ih);
            iconDr.draw(icCanvas);

            Integer solidBg = detectSquircleSolidBgColor(icBmp);
            Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

            if (solidBg != null) {
                canvas.drawColor(solidBg);
                int cropMarginX = Math.round(iw * 0.16f);
                int cropMarginY = Math.round(ih * 0.16f);
                android.graphics.Rect srcRect = new android.graphics.Rect(
                        cropMarginX, cropMarginY, iw - cropMarginX, ih - cropMarginY);
                int targetH = Math.round(h * 0.56f);
                RectF dst = new RectF((w - targetH) * 0.5f, (h - targetH) * 0.5f,
                        (w + targetH) * 0.5f, (h + targetH) * 0.5f);
                canvas.drawBitmap(icBmp, srcRect, dst, bmpPaint);
            } else {
                int brandColor = extractEdgeBrandColor(icBmp);
                Paint gradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                gradPaint.setShader(new LinearGradient(0, 0, w, h,
                         darkenColor(brandColor, 0.55f), darkenColor(brandColor, 0.30f), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, gradPaint);

                int targetSize = Math.round(h * 0.54f);
                RectF dst = new RectF((w - targetSize) * 0.5f, (h - targetSize) * 0.5f,
                        (w + targetSize) * 0.5f, (h + targetSize) * 0.5f);
                canvas.drawBitmap(icBmp, null, dst, bmpPaint);
            }
            return out;
        }

        canvas.drawColor(0xFF1E293B);
        return out;
    }

    private boolean isHarshSplitLegacyBanner(Drawable dr) {
        try {
            int dw = Math.max(1, dr.getIntrinsicWidth());
            int dh = Math.max(1, dr.getIntrinsicHeight());
            Bitmap bmp = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            dr.setBounds(0, 0, dw, dh);
            dr.draw(c);
            int topPx = bmp.getPixel(dw / 2, Math.max(1, dh / 12));
            int botPx = bmp.getPixel(dw / 2, Math.min(dh - 2, dh * 11 / 12));
            int diff = Math.abs(((topPx >> 16) & 0xFF) - ((botPx >> 16) & 0xFF))
                     + Math.abs(((topPx >> 8) & 0xFF) - ((botPx >> 8) & 0xFF))
                     + Math.abs((topPx & 0xFF) - (botPx & 0xFF));
            return diff > 220;
        } catch (Exception e) {
            return false;
        }
    }

    private void drawGoogleTvBannerRefined(Canvas canvas, Drawable dr, int viewW, int viewH) {
        int dw = Math.max(1, dr.getIntrinsicWidth());
        int dh = Math.max(1, dr.getIntrinsicHeight());
        Bitmap bmp = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        dr.setBounds(0, 0, dw, dh);
        dr.draw(c);

        // Check if the banner has a uniform solid edge background (e.g. YouTube, Netflix, Prime Video, Spotify, Play Store, VLC, Downloader)
        Integer edgeBg = detectBannerUniformEdgeColor(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (edgeBg != null) {
            canvas.drawColor(edgeBg);
            // Check if the foreground logo touches close to top/bottom edges (like VLC or Downloader)
            boolean tallLogo = hasForegroundNearTopBottomEdges(bmp, edgeBg);
            float contentScale = tallLogo ? 0.72f : 0.88f;
            float baseScale = Math.min((float) viewW / dw, (float) viewH / dh) * contentScale;
            float dx = (viewW - dw * baseScale) * 0.5f;
            float dy = (viewH - dh * baseScale) * 0.5f;
            Matrix m = new Matrix();
            m.setScale(baseScale, baseScale);
            m.postTranslate(dx, dy);
            canvas.drawBitmap(bmp, m, p);
        } else {
            drawDrawableCenterCrop(canvas, dr, viewW, viewH);
        }
    }

    private Integer detectBannerUniformEdgeColor(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int mx = Math.max(2, w / 20);
        int my = Math.max(2, h / 20);
        int[] pts = {
            bmp.getPixel(mx, my),
            bmp.getPixel(w - 1 - mx, my),
            bmp.getPixel(mx, h - 1 - my),
            bmp.getPixel(w - 1 - mx, h - 1 - my),
            bmp.getPixel(w / 2, my),
            bmp.getPixel(w / 2, h - 1 - my),
            bmp.getPixel(mx, h / 2),
            bmp.getPixel(w - 1 - mx, h / 2)
        };
        int ref = pts[0];
        if (((ref >>> 24) & 0xFF) < 230) return null;
        int r0 = (ref >> 16) & 0xFF;
        int g0 = (ref >> 8) & 0xFF;
        int b0 = ref & 0xFF;
        int matches = 0;
        for (int i = 1; i < pts.length; i++) {
            int px = pts[i];
            if (((px >>> 24) & 0xFF) < 230) continue;
            int diff = Math.abs(((px >> 16) & 0xFF) - r0)
                     + Math.abs(((px >> 8) & 0xFF) - g0)
                     + Math.abs((px & 0xFF) - b0);
            if (diff <= 36) matches++;
        }
        return (matches >= 6) ? (ref | 0xFF000000) : null;
    }

    private boolean hasForegroundNearTopBottomEdges(Bitmap bmp, int bgColor) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int r0 = (bgColor >> 16) & 0xFF;
        int g0 = (bgColor >> 8) & 0xFF;
        int b0 = bgColor & 0xFF;
        int scanY1 = Math.max(1, Math.round(h * 0.14f));
        int scanY2 = Math.min(h - 2, Math.round(h * 0.86f));
        int nonBgCount = 0;
        for (int x = w / 5; x < w * 4 / 5; x += Math.max(1, w / 30)) {
            int p1 = bmp.getPixel(x, scanY1);
            int p2 = bmp.getPixel(x, scanY2);
            int d1 = Math.abs(((p1 >> 16) & 0xFF) - r0) + Math.abs(((p1 >> 8) & 0xFF) - g0) + Math.abs((p1 & 0xFF) - b0);
            int d2 = Math.abs(((p2 >> 16) & 0xFF) - r0) + Math.abs(((p2 >> 8) & 0xFF) - g0) + Math.abs((p2 & 0xFF) - b0);
            if (d1 > 50 || d2 > 50) nonBgCount++;
        }
        return nonBgCount >= 2;
    }

    private Integer detectSquircleSolidBgColor(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int insetX = Math.round(w * 0.18f);
        int insetY = Math.round(h * 0.18f);
        int[] samples = {
            bmp.getPixel(w / 2, insetY),
            bmp.getPixel(w / 2, h - 1 - insetY),
            bmp.getPixel(insetX, h / 2),
            bmp.getPixel(w - 1 - insetX, h / 2)
        };
        int ref = samples[0];
        if (((ref >>> 24) & 0xFF) < 220) {
            return null;
        }
        int r0 = (ref >> 16) & 0xFF;
        int g0 = (ref >> 8) & 0xFF;
        int b0 = ref & 0xFF;
        int matchCount = 0;
        for (int i = 1; i < samples.length; i++) {
            int px = samples[i];
            if (((px >>> 24) & 0xFF) < 220) continue;
            int dr = Math.abs(((px >> 16) & 0xFF) - r0);
            int dg = Math.abs(((px >> 8) & 0xFF) - g0);
            int db = Math.abs((px & 0xFF) - b0);
            if (dr + dg + db < 45) {
                matchCount++;
            }
        }
        if (matchCount >= 2) {
            return ref | 0xFF000000;
        }
        return null;
    }

    private void drawDrawableCenterCrop(Canvas canvas, Drawable dr, int viewW, int viewH) {
        int dw = Math.max(1, dr.getIntrinsicWidth());
        int dh = Math.max(1, dr.getIntrinsicHeight());
        Bitmap bmp = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        dr.setBounds(0, 0, dw, dh);
        dr.draw(c);

        float scale = Math.max((float) viewW / dw, (float) viewH / dh);
        float dx = (viewW - dw * scale) * 0.5f;
        float dy = (viewH - dh * scale) * 0.5f;
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate(dx, dy);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bmp, m, p);
    }

    private Bitmap clipBitmapToRoundedRect(Bitmap src, int w, int h, float radiusPx) {
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0, 0, w, h), radiusPx, radiusPx, paint);
        return output;
    }

    private int extractEdgeBrandColor(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[][] coords = {
            {w / 2, h / 6}, {w / 2, h * 5 / 6}, {w / 6, h / 2}, {w * 5 / 6, h / 2},
            {w / 4, h / 4}, {w * 3 / 4, h / 4}, {w / 4, h * 3 / 4}, {w * 3 / 4, h * 3 / 4}
        };
        for (int[] pt : coords) {
            int x = Math.max(0, Math.min(w - 1, pt[0]));
            int y = Math.max(0, Math.min(h - 1, pt[1]));
            int px = bmp.getPixel(x, y);
            int alpha = (px >>> 24) & 0xFF;
            if (alpha > 180) {
                return px | 0xFF000000;
            }
        }
        return 0xFF1E293B;
    }

    private int lightenColor(int color, float factor) {
        int r = Math.min(255, Math.round(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((color & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int darkenColor(int color, float factor) {
        int r = Math.max(0, Math.round(((color >> 16) & 0xFF) * factor));
        int g = Math.max(0, Math.round(((color >> 8) & 0xFF) * factor));
        int b = Math.max(0, Math.round((color & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // =========================================================================
    // UPDATES: CLOCK, DATES, NETWORK & NOTIFICATIONS
    // =========================================================================

    private void updateClockAndDates() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        clockBigText.setText(clockFmt.format(now.getTime()));

        int dd = now.get(Calendar.DAY_OF_MONTH);
        int mm = now.get(Calendar.MONTH) + 1;
        int yy = now.get(Calendar.YEAR);

        String weekday = LunarCalendar.getWeekdayName(now);
        solarDateWidgetText.setText(weekday + ", " + dd + " tháng " + mm + ", " + yy);

        int[] lunar = LunarCalendar.solarToLunar(dd, mm, yy);
        String canChiYear = LunarCalendar.getYearCanChi(lunar[2]);

        lunarDateWidgetText.setText("Ngày " + lunar[0] + " tháng " + lunar[1]
                + (lunar[3] == 1 ? " nhuận" : "") + " Âm lịch");
        lunarCanChiSubText.setText("Năm " + canChiYear);
    }

    private void updateNetworkAndNotificationStatus() {
        boolean wifiConnected = false;
        boolean lanConnected = false;
        String wifiSsid = null;

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network[] networks = cm.getAllNetworks();
                if (networks != null) {
                    for (Network net : networks) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                        if (caps == null) continue;
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            wifiConnected = true;
                        }
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            lanConnected = true;
                        }
                    }
                }
                NetworkInfo active = cm.getActiveNetworkInfo();
                if (active != null && active.isConnected()) {
                    if (active.getType() == ConnectivityManager.TYPE_WIFI) wifiConnected = true;
                    if (active.getType() == ConnectivityManager.TYPE_ETHERNET) lanConnected = true;
                }
            }
            if (wifiConnected) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String rawSsid = info != null ? info.getSSID() : null;
                if (rawSsid != null && !rawSsid.isEmpty() && !"<unknown ssid>".equalsIgnoreCase(rawSsid)) {
                    wifiSsid = rawSsid.replace("\"", "");
                }
            }
        } catch (Exception ignored) {
        }

        lastWifiConnected = wifiConnected;
        lastLanConnected = lanConnected;
        lastWifiSsid = wifiSsid;

        wifiIconView.setConnected(wifiConnected);
        lanIconView.setConnected(lanConnected);

        systemNotifications.clear();
        List<String> activeNotifs = TvNotificationListener.getNotificationSummaries();
        if (!activeNotifs.isEmpty()) {
            systemNotifications.addAll(activeNotifs);
        } else {
            if (!prefs.getBoolean("notif_read_ack", false)) {
                systemNotifications.add("Hệ thống Mecool KM2: Đã tối ưu hóa và giải phóng RAM chạy ngầm.");
                systemNotifications.add("Mạng: " + (wifiConnected ? ("Đang kết nối Wi-Fi " + (wifiSsid != null ? wifiSsid : "")) : (lanConnected ? "Đang kết nối mạng LAN" : "Chưa kết nối mạng")));
            }
        }

        int count = systemNotifications.size();
        notifIconView.setBadgeCount(count);
    }

    private void showNotificationsDialog() {
        List<String> displayList = new ArrayList<>(systemNotifications);
        if (displayList.isEmpty()) {
            displayList.add("Không có thông báo mới nào.");
        }
        String[] items = displayList.toArray(new String[0]);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Thông báo hệ thống (" + systemNotifications.size() + ")")
                .setItems(items, null)
                .setPositiveButton("Đánh dấu đã đọc", (d, w) -> {
                    prefs.edit().putBoolean("notif_read_ack", true).apply();
                    updateNetworkAndNotificationStatus();
                })
                .setNeutralButton("Làm mới", (d, w) -> {
                    prefs.edit().putBoolean("notif_read_ack", false).apply();
                    updateNetworkAndNotificationStatus();
                    fetchLiveWeatherAndMarket();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    // =========================================================================
    // WALLPAPER ENGINE (AUTO INTERNET WALLPAPERS + MANUAL SELECTION)
    // =========================================================================

    private File getWallpaperCacheFile() {
        return new File(getFilesDir(), "wallpaper_current.jpg");
    }

    private void loadWallpaperOnStartup() {
        String wpMode = prefs.getString("wp_mode", "auto_bing");
        if ("none".equals(wpMode)) {
            wallpaperImageView.setImageDrawable(null);
            wallpaperScrimView.setBackgroundColor(0x00000000);
            return;
        }

        // Load cached wallpaper bitmap immediately if present
        File cacheFile = getWallpaperCacheFile();
        if (cacheFile.exists() && cacheFile.length() > 1024) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                if (bmp != null) {
                    wallpaperImageView.setImageBitmap(bmp);
                    applyWidgetOpacityTheme();
                }
            } catch (Exception ignored) {
            }
        } else {
            // First time launch: fetch a free internet wallpaper right away!
            fetchInternetWallpaperAsync(wpMode, false);
        }
    }

    private void checkAutoWallpaperRotation() {
        String wpMode = prefs.getString("wp_mode", "auto_bing");
        if (!wpMode.startsWith("auto_")) return;

        long intervalMs = prefs.getLong("wp_interval_ms", 30 * 60 * 1000L); // default 30 mins
        long lastUpdate = prefs.getLong("wp_last_update_ms", 0L);
        long now = System.currentTimeMillis();
        if (now - lastUpdate >= intervalMs) {
            fetchInternetWallpaperAsync(wpMode, false);
        }
    }

    private void fetchInternetWallpaperAsync(final String modeOrUrl, final boolean showToast) {
        if (showToast) {
            Toast.makeText(this, "Đang tải hình nền Full HD...", Toast.LENGTH_SHORT).show();
        }
        networkPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String targetUrl = modeOrUrl;
                    if ("auto_bing".equals(modeOrUrl) || "BING_DAILY_0".equals(modeOrUrl)) {
                        int idx = "BING_DAILY_0".equals(modeOrUrl) ? 0 : new Random().nextInt(7);
                        String bingJson = httpGet("https://www.bing.com/HPImageArchive.aspx?format=js&idx=" + idx + "&n=1&mkt=vi-VN");
                        if (bingJson != null) {
                            JSONArray arr = new JSONObject(bingJson).optJSONArray("images");
                            if (arr != null && arr.length() > 0) {
                                String relUrl = arr.getJSONObject(0).optString("url", "");
                                if (!relUrl.isEmpty()) {
                                    targetUrl = "https://www.bing.com" + relUrl;
                                }
                            }
                        }
                        if (targetUrl.startsWith("auto_") || targetUrl.startsWith("BING_")) {
                            targetUrl = PRESET_WALLPAPER_URLS[new Random().nextInt(PRESET_WALLPAPER_URLS.length - 1)];
                        }
                    } else if ("auto_random".equals(modeOrUrl)) {
                        // Rotate through curated 1080p Unsplash + Picsum photos
                        if (new Random().nextBoolean()) {
                            targetUrl = "https://picsum.photos/1920/1080?random=" + System.currentTimeMillis();
                        } else {
                            targetUrl = PRESET_WALLPAPER_URLS[new Random().nextInt(PRESET_WALLPAPER_URLS.length - 1)];
                        }
                    }

                    byte[] bytes = downloadBytes(targetUrl);
                    if (bytes != null && bytes.length > 2048) {
                        final Bitmap bmp = decodeSampledBitmapFromBytes(bytes, 1920, 1080);
                        if (bmp != null) {
                            FileOutputStream fos = new FileOutputStream(getWallpaperCacheFile());
                            fos.write(bytes);
                            fos.close();

                            prefs.edit().putLong("wp_last_update_ms", System.currentTimeMillis()).apply();

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    wallpaperImageView.setImageBitmap(bmp);
                                    applyWidgetOpacityTheme();
                                    if (showToast) {
                                        Toast.makeText(MainActivity.this, "Đã cập nhật hình nền mới!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (showToast) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Không tải được ảnh nền, vui lòng thử nguồn khác.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void showWallpaperSettingsDialog() {
        String currentMode = prefs.getString("wp_mode", "auto_bing");
        long intervalMin = prefs.getLong("wp_interval_ms", 30 * 60 * 1000L) / 60000L;
        int opacityPct = prefs.getInt("widget_opacity_pct", 42);
        int dimPct = prefs.getInt("wallpaper_dim_pct", 16);

        String[] options = {
            "💎 Chỉnh độ mờ đục (Trong suốt) nền Widget (Hiện tại: " + opacityPct + "%)",
            "☀️ Chỉnh độ sáng của Hình nền (Độ phủ tối: " + dimPct + "%)",
            "⚡ Đổi sang ảnh nền ngẫu nhiên khác ngay lập tức",
            "🔄 Tự động đổi ảnh từ Microsoft Bing Daily (" + ("auto_bing".equals(currentMode) ? "Đang bật" : "Bật") + ")",
            "🌄 Tự động đổi ảnh phong cảnh ngẫu nhiên (" + ("auto_random".equals(currentMode) ? "Đang bật" : "Bật") + ")",
            "⏱️ Cài đặt thời gian tự động đổi ảnh (Hiện tại: " + intervalMin + " phút)",
            "🖼️ Tự chọn thủ công từ Bộ sưu tập 10 Chủ đề Full HD",
            "📁 Tự chọn ảnh thủ công từ bộ nhớ TV / USB",
            "🔗 Nhập đường dẫn URL hình nền từ Internet",
            "🌑 Dùng nền tối mặc định (Tắt hình nền)"
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Cài đặt Giao diện, Độ trong suốt & Hình nền")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showWidgetOpacityDialog();
                    } else if (which == 1) {
                        showWallpaperBrightnessDialog();
                    } else if (which == 2) {
                        String m = prefs.getString("wp_mode", "auto_random");
                        if (!m.startsWith("auto_")) m = "auto_random";
                        fetchInternetWallpaperAsync(m, true);
                    } else if (which == 3) {
                        prefs.edit().putString("wp_mode", "auto_bing").apply();
                        fetchInternetWallpaperAsync("auto_bing", true);
                    } else if (which == 4) {
                        prefs.edit().putString("wp_mode", "auto_random").apply();
                        fetchInternetWallpaperAsync("auto_random", true);
                    } else if (which == 5) {
                        showWallpaperIntervalDialog();
                    } else if (which == 6) {
                        showPresetWallpapersDialog();
                    } else if (which == 7) {
                        showLocalFilesWallpaperDialog();
                    } else if (which == 8) {
                        showCustomUrlWallpaperDialog();
                    } else if (which == 9) {
                        prefs.edit().putString("wp_mode", "none").apply();
                        getWallpaperCacheFile().delete();
                        wallpaperImageView.setImageDrawable(null);
                        applyWidgetOpacityTheme();
                        Toast.makeText(this, "Đã chuyển về nền tối mặc định", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showWidgetOpacityDialog() {
        final String[] labels = {
            "0% - Trong suốt hoàn toàn (Không nền, chỉ hiện chữ nổi)",
            "20% - Kính trong suốt siêu mỏng",
            "32% - Kính mờ sáng nhẹ",
            "42% - Kính mờ cân bằng (Mặc định khuyên dùng)",
            "55% - Kính mờ rõ nét",
            "70% - Nền bán trong suốt đậm",
            "85% - Nền đặc tối đa"
        };
        final int[] pcts = {0, 20, 32, 42, 55, 70, 85};

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chọn độ mờ đục (Opacity) của nền Widget")
                .setItems(labels, (d, w) -> {
                    prefs.edit().putInt("widget_opacity_pct", pcts[w]).apply();
                    applyWidgetOpacityTheme();
                    styleAppSlot(ALL_APPS_SLOT, slotCards[ALL_APPS_SLOT] != null && slotCards[ALL_APPS_SLOT].hasFocus());
                    Toast.makeText(this, "Đã đặt độ mờ đục Widget: " + pcts[w] + "%", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Quay lại", (d, w) -> showWallpaperSettingsDialog())
                .show();
    }

    private void showWallpaperBrightnessDialog() {
        final String[] labels = {
            "Sáng rực rỡ 100% (0% lớp phủ tối)",
            "Sáng trong trẻo (16% lớp phủ tối - Mặc định khuyên dùng)",
            "Dịu mắt cân bằng (30% lớp phủ tối)",
            "Tối sâu tương phản cao (48% lớp phủ tối)"
        };
        final int[] dimPcts = {0, 16, 30, 48};

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chỉnh độ sáng của Hình nền")
                .setItems(labels, (d, w) -> {
                    prefs.edit().putInt("wallpaper_dim_pct", dimPcts[w]).apply();
                    applyWidgetOpacityTheme();
                    Toast.makeText(this, "Đã cập nhật độ sáng hình nền!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Quay lại", (d, w) -> showWallpaperSettingsDialog())
                .show();
    }

    private void showWallpaperIntervalDialog() {
        final String[] labels = {
            "Mỗi 5 phút",
            "Mỗi 15 phút",
            "Mỗi 30 phút (Khuyên dùng)",
            "Mỗi 1 giờ",
            "Mỗi 6 giờ",
            "Mỗi 24 giờ (Hàng ngày)"
        };
        final long[] valuesMs = {
            5 * 60_000L,
            15 * 60_000L,
            30 * 60_000L,
            60 * 60_000L,
            360 * 60_000L,
            1440 * 60_000L
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chọn chu kỳ tự động đổi hình nền")
                .setItems(labels, (d, w) -> {
                    prefs.edit().putLong("wp_interval_ms", valuesMs[w]).apply();
                    Toast.makeText(this, "Đã đặt chu kỳ: " + labels[w], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showPresetWallpapersDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chọn Hình nền Chủ đề Full HD (Miễn phí)")
                .setItems(PRESET_WALLPAPER_NAMES, (d, w) -> {
                    prefs.edit().putString("wp_mode", "manual_preset_" + w).apply();
                    fetchInternetWallpaperAsync(PRESET_WALLPAPER_URLS[w], true);
                })
                .setNegativeButton("Quay lại", (d, w) -> showWallpaperSettingsDialog())
                .show();
    }

    private void showCustomUrlWallpaperDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(14));
        box.setBackground(roundedBox(0xFF161B26, 12, dp(1), 0xFF2D3748));

        String currentUrl = prefs.getString("wp_custom_url", "https://picsum.photos/1920/1080");
        final EditText input = createDarkTvEditText(currentUrl, "https://...");
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Nhập đường dẫn URL ảnh nền")
                .setView(box)
                .setPositiveButton("Tải & Áp dụng", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        prefs.edit()
                                .putString("wp_mode", "manual_url")
                                .putString("wp_custom_url", url)
                                .apply();
                        fetchInternetWallpaperAsync(url, true);
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
        dialog.show();
    }

    private void showLocalFilesWallpaperDialog() {
        final List<File> imageFiles = new ArrayList<>();
        File[] roots = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStorageDirectory(),
            new File("/storage")
        };
        Set<String> visited = new HashSet<>();
        for (File r : roots) {
            scanForImages(r, imageFiles, visited, 0);
        }

        List<String> itemNames = new ArrayList<>();
        itemNames.add("📂 Mở trình chọn tệp hệ thống Android...");
        for (File f : imageFiles) {
            itemNames.add("🖼️ " + f.getName() + " (" + f.getParentFile().getName() + ")");
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chọn ảnh từ bộ nhớ TV / USB (" + imageFiles.size() + " ảnh)")
                .setItems(itemNames.toArray(new String[0]), (d, w) -> {
                    if (w == 0) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(Intent.createChooser(intent, "Chọn hình nền"), PICK_WALLPAPER_REQUEST);
                        } catch (Exception e) {
                            Toast.makeText(this, "Copy ảnh vào thư mục Download trên TV để chọn trực tiếp", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        File chosen = imageFiles.get(w - 1);
                        applyLocalWallpaperFile(chosen);
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void scanForImages(File dir, List<File> out, Set<String> visited, int depth) {
        if (dir == null || !dir.exists() || depth > 3 || out.size() >= 40) return;
        String path = dir.getAbsolutePath();
        if (visited.contains(path) || path.contains("/Android/data")) return;
        visited.add(path);

        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanForImages(f, out, visited, depth + 1);
            } else {
                String name = f.getName().toLowerCase(Locale.US);
                if ((name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
                        && f.length() > 10 * 1024) {
                    out.add(f);
                    if (out.size() >= 40) return;
                }
            }
        }
    }

    private void applyLocalWallpaperFile(final File file) {
        networkPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        FileOutputStream fos = new FileOutputStream(getWallpaperCacheFile());
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                        fos.close();
                        prefs.edit().putString("wp_mode", "manual_local").apply();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                wallpaperImageView.setImageBitmap(bmp);
                                wallpaperScrimView.setBackgroundColor(0x5A000000);
                                Toast.makeText(MainActivity.this, "Đã đặt hình nền: " + file.getName(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Không đọc được file ảnh", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_WALLPAPER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            networkPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        if (is != null) {
                            final Bitmap bmp = BitmapFactory.decodeStream(is);
                            is.close();
                            if (bmp != null) {
                                FileOutputStream fos = new FileOutputStream(getWallpaperCacheFile());
                                bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                                fos.close();
                                prefs.edit().putString("wp_mode", "manual_uri").apply();
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        wallpaperImageView.setImageBitmap(bmp);
                                        wallpaperScrimView.setBackgroundColor(0x5A000000);
                                        Toast.makeText(MainActivity.this, "Đã áp dụng hình nền!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    private Bitmap decodeSampledBitmapFromBytes(byte[] data, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        int inSampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            int halfHeight = options.outHeight / 2;
            int halfWidth = options.outWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    // =========================================================================
    // APP GRID (2x6) MANAGEMENT
    // =========================================================================

    private static final class AppEntry {
        final String label;
        final String pkg;

        AppEntry(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    private List<AppEntry> queryAllInstalledApps() {
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        List<AppEntry> list = new ArrayList<>();
        String myPkg = getPackageName();

        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : categories) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            List<ResolveInfo> res = pm.queryIntentActivities(q, 0);
            if (res == null) continue;
            for (ResolveInfo ri : res) {
                if (ri.activityInfo == null) continue;
                String p = ri.activityInfo.packageName;
                if (p == null || p.equals(myPkg) || seen.contains(p)) continue;
                seen.add(p);
                CharSequence lbl = ri.loadLabel(pm);
                String name = (lbl != null && lbl.length() > 0) ? lbl.toString() : p;
                list.add(new AppEntry(name, p));
            }
        }
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return list;
    }

    private String getAppLabel(String pkg) {
        if ("__ALL_APPS__".equals(pkg)) return "Tất cả ứng dụng";
        if ("__SETTINGS__".equals(pkg)) return "Cài đặt hệ thống";
        if ("__ADD_APP__".equals(pkg)) return "Thêm ứng dụng";
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence cs = pm.getApplicationLabel(ai);
            if (cs != null && cs.length() > 0) return cs.toString();
        } catch (Exception ignored) {
        }
        return pkg;
    }

    private boolean isPackageEnabledAndInstalled(String pkg) {
        if (pkg == null) return false;
        if ("__ALL_APPS__".equals(pkg) || "__SETTINGS__".equals(pkg)) return true;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            return ai != null && ai.enabled;
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshAppGridSlots() {
        List<AppEntry> allInstalled = queryAllInstalledApps();
        Set<String> usedPkgs = new HashSet<>();

        List<String> autoCandidates = new ArrayList<>();
        for (String pri : PRIORITY_PKGS) {
            if (isPackageEnabledAndInstalled(pri) && !usedPkgs.contains(pri)) {
                autoCandidates.add(pri);
                usedPkgs.add(pri);
            }
        }
        for (AppEntry ae : allInstalled) {
            if (!usedPkgs.contains(ae.pkg)) {
                autoCandidates.add(ae.pkg);
                usedPkgs.add(ae.pkg);
            }
        }

        int autoCursor = 0;
        for (int i = 0; i < ALL_APPS_SLOT; i++) {
            String saved = prefs.getString("app_slot_" + i, null);
            if (saved != null && isPackageEnabledAndInstalled(saved)) {
                activeSlotPkgs[i] = saved;
            } else {
                while (autoCursor < autoCandidates.size() && containsPkgUpTo(activeSlotPkgs, i, autoCandidates.get(autoCursor))) {
                    autoCursor++;
                }
                if (autoCursor < autoCandidates.size()) {
                    activeSlotPkgs[i] = autoCandidates.get(autoCursor++);
                } else if (!containsPkgUpTo(activeSlotPkgs, i, "__SETTINGS__")) {
                    activeSlotPkgs[i] = "__SETTINGS__";
                } else {
                    activeSlotPkgs[i] = "__ADD_APP__";
                }
            }
            if (slotBannerViews[i] != null) {
                slotBannerViews[i].setImageBitmap(getOrCreateFullBleedBanner(activeSlotPkgs[i]));
            }
            boolean focused = slotCards[i] != null && slotCards[i].hasFocus();
            styleAppSlot(i, focused);
        }

        // Slot 11 (bottom-right corner) is always All Apps
        activeSlotPkgs[ALL_APPS_SLOT] = "__ALL_APPS__";
        boolean allFocused = slotCards[ALL_APPS_SLOT] != null && slotCards[ALL_APPS_SLOT].hasFocus();
        styleAppSlot(ALL_APPS_SLOT, allFocused);

        updateSelectedAppPrompt(currentFocusedSlot);
    }

    private boolean containsPkgUpTo(String[] arr, int limit, String target) {
        for (int i = 0; i < limit; i++) {
            if (target.equals(arr[i])) return true;
        }
        return false;
    }

    private void styleAppSlot(int index, boolean focused) {
        String pkg = activeSlotPkgs[index];
        String label = getAppLabel(pkg);

        slotBottomLabels[index].setText(label);
        slotBottomLabels[index].setTextColor(focused ? 0xFFFFFFFF : 0xFFCBD5E1);

        if (index == ALL_APPS_SLOT) {
            int bgColor = focused ? 0xFF3B3748 : 0xCC2A2636;
            slotCards[index].setBackground(roundedBox(bgColor, 6, 0, 0));
        } else {
            slotCards[index].setBackground(roundedBox(0x00000000, 6, 0, 0));
            if (slotBannerViews[index] != null) {
                if (focused) {
                    // 100% crisp full brightness when focused, just like Google TV
                    slotBannerViews[index].clearColorFilter();
                } else {
                    // Authentic Google TV unfocused soft dimming tint (RGB ~ 166, 162, 170 on white tiles)
                    slotBannerViews[index].setColorFilter(0xFFA8A3AC, android.graphics.PorterDuff.Mode.MULTIPLY);
                }
            }
        }

        if (slotBorderOverlays[index] != null) {
            // Native Google TV Favorite Apps style: no box border on app tiles; focus is shown by brightness + scale + shadow
            slotBorderOverlays[index].setBackground(roundedBox(0x00000000, 6, 0, 0x00000000));
        }

        slotCards[index].setElevation(focused ? dp(14) : dp(4));
        slotCards[index].setScaleX(focused ? 1.08f : 1.0f);
        slotCards[index].setScaleY(focused ? 1.08f : 1.0f);
    }

    private void updateSelectedAppPrompt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= TOTAL_SLOTS) return;
        String title = getAppLabel(activeSlotPkgs[slotIndex]);
        String hint = (slotIndex == ALL_APPS_SLOT) ? "  ·  Nhấn OK để xem toàn bộ ứng dụng" : "  ·  Nhấn OK để mở";
        SpannableString ss = new SpannableString(title + hint);
        ss.setSpan(new ForegroundColorSpan(0xFFFFFFFF), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(0xFFE2E8F0), title.length(), ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new RelativeSizeSpan(0.76f), title.length(), ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        selectedAppPrompt.setText(ss);
    }

    private void launchAppSlot(int slotIndex) {
        String pkg = activeSlotPkgs[slotIndex];
        if ("__ALL_APPS__".equals(pkg)) {
            showAllAppsDialog(-1);
            return;
        }
        if ("__SETTINGS__".equals(pkg)) {
            openSystemSettings();
            return;
        }
        if ("__ADD_APP__".equals(pkg)) {
            showAllAppsDialog(slotIndex);
            return;
        }
        launchPackage(pkg);
    }

    private void launchPackage(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(pkg);
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không mở được: " + getAppLabel(pkg), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi mở ứng dụng", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWifiSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            openSystemSettings();
        }
    }

    private void openSystemSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // VISUAL ALL APPS DRAWER DIALOG (FULL-BLEED BANNERS)
    // =========================================================================

    private void showAllAppsDialog(final int targetSlotToAssign) {
        final List<AppEntry> apps = queryAllInstalledApps();
        final Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(0xF20E1015);
        container.setPadding(dp(42), dp(28), dp(42), dp(28));

        TextView header = new TextView(this);
        if (targetSlotToAssign >= 0) {
            header.setText("Chọn ứng dụng gán vào Ô số " + (targetSlotToAssign + 1));
        } else {
            header.setText("Tất cả ứng dụng (" + apps.size() + ")");
        }
        header.setTextColor(0xFFFFFFFF);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.bottomMargin = dp(18);
        container.addView(header, hLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(gridContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int cols = 6;
        int total = apps.size();
        int rows = (total + cols - 1) / cols;

        for (int r = 0; r < rows; r++) {
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(14);
            gridContainer.addView(rowView, rLp);

            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (c < cols - 1) cLp.rightMargin = dp(12);

                if (idx < total) {
                    final AppEntry entry = apps.get(idx);
                    FrameLayout card = new FrameLayout(this);
                    card.setFocusable(true);

                    ImageView iv = new ImageView(this);
                    iv.setScaleType(ImageView.ScaleType.FIT_XY);
                    iv.setImageBitmap(getOrCreateFullBleedBanner(entry.pkg));
                    card.addView(iv, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    final View border = new View(this);
                    border.setBackground(roundedBox(0x00000000, 12, dp(1), 0x33FFFFFF));
                    card.addView(border, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    final TextView lbl = new TextView(this);
                    lbl.setText(entry.label);
                    lbl.setTextColor(0xFF94A3B8);
                    lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                    lbl.setGravity(Gravity.CENTER);
                    lbl.setSingleLine(true);
                    lbl.setEllipsize(TextUtils.TruncateAt.END);

                    card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            border.setBackground(roundedBox(0x00000000, 12,
                                    dp(hasFocus ? 2.6f : 1), hasFocus ? 0xFFFFFFFF : 0x33FFFFFF));
                            v.setScaleX(hasFocus ? 1.05f : 1.0f);
                            v.setScaleY(hasFocus ? 1.05f : 1.0f);
                            lbl.setTextColor(hasFocus ? 0xFFFFFFFF : 0xFF94A3B8);
                        }
                    });

                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            if (targetSlotToAssign >= 0 && targetSlotToAssign < ALL_APPS_SLOT) {
                                prefs.edit().putString("app_slot_" + targetSlotToAssign, entry.pkg).apply();
                                refreshAppGridSlots();
                            } else {
                                launchPackage(entry.pkg);
                            }
                        }
                    });

                    cell.addView(card, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
                    LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lLp.topMargin = dp(4);
                    cell.addView(lbl, lLp);
                }
                rowView.addView(cell, cLp);
            }
        }

        dialog.setContentView(container);
        dialog.show();
    }

    private void showCityPickerDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Chọn Khu vực Thời tiết")
                .setItems(CITY_NAMES, (dialog, which) -> {
                    prefs.edit().putInt("city_index", which).apply();
                    cityLabel.setText("⌖ " + CITY_NAMES[which]);
                    fetchLiveWeatherAndMarket();
                })
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showWallpaperSettingsDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // =========================================================================
    // LIVE WEATHER, VN-INDEX & MARKET NETWORKING
    // =========================================================================

    private void loadCachedData() {
        int cityIdx = prefs.getInt("city_index", 0);
        if (cityIdx < 0 || cityIdx >= CITY_NAMES.length) cityIdx = 0;
        cityLabel.setText("⌖ " + CITY_NAMES[cityIdx]);

        mainTempText.setText(prefs.getString("w_temp", "25°"));
        mainConditionText.setText(prefs.getString("w_cond", "Ít mây"));
        mainHighLowText.setText(prefs.getString("w_hl", "Cao 29° · Thấp 23°"));
        mainWeatherIcon.setIconType(prefs.getInt("w_icon", WeatherIconView.TYPE_SUN));

        rainProbVal.setText(prefs.getString("w_rain", "12%"));
        humidityVal.setText(prefs.getString("w_hum", "76%"));
        aqiVal.setText(prefs.getString("w_aqi", "75 · TB"));
        uvVal.setText(prefs.getString("w_uv", "5 · TB"));

        marketUpdateText.setText(prefs.getString("m_time", "Cập nhật 21:40"));
        sjcBuyText.setText(prefs.getString("m_sjc_b", "139,0"));
        sjcSellText.setText(prefs.getString("m_sjc_s", "142,0"));
        styleGoldTrendBadge(sjcTrendBadge, prefs.getInt("m_sjc_dir", 1));
        ringBuyText.setText(prefs.getString("m_ring_b", "138,0"));
        ringSellText.setText(prefs.getString("m_ring_s", "141,0"));
        styleGoldTrendBadge(ringTrendBadge, prefs.getInt("m_ring_dir", 1));

        String vniPt = prefs.getString("m_vni_pt", "1.699,62");
        String vniChg = prefs.getString("m_vni_chg", "▼ -11,10 (-0,65%)");
        int vniDir = prefs.getInt("m_vni_dir", -1);
        vnIndexPointText.setText(vniPt);
        styleStockChangeBadge(vnIndexChangeBadge, vnIndexPointText, vniChg, vniDir);

        String vn30Pt = prefs.getString("m_vn30_pt", "1.815,60");
        String vn30Chg = prefs.getString("m_vn30_chg", "▲ +2,19 (+0,12%)");
        int vn30Dir = prefs.getInt("m_vn30_dir", 1);
        vn30PointText.setText(vn30Pt);
        styleStockChangeBadge(vn30ChangeBadge, vn30PointText, vn30Chg, vn30Dir);

        usdVcbText.setText(prefs.getString("m_usd", "25.744"));
        styleGoldTrendBadge(usdTrendBadge, prefs.getInt("m_usd_dir", 1));
    }

    private void fetchLiveWeatherAndMarket() {
        final int cityIdx = Math.max(0, Math.min(CITY_NAMES.length - 1, prefs.getInt("city_index", 0)));
        final double lat = CITY_LATS[cityIdx];
        final double lon = CITY_LONS[cityIdx];

        networkPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String weatherUrl = String.format(Locale.US,
                            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,relative_humidity_2m,weather_code"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max"
                            + "&timezone=Asia%%2FBangkok&forecast_days=6", lat, lon);

                    String wJsonStr = httpGet(weatherUrl);
                    if (wJsonStr != null) {
                        JSONObject root = new JSONObject(wJsonStr);
                        JSONObject current = root.getJSONObject("current");
                        int temp = (int) Math.round(current.getDouble("temperature_2m"));
                        int hum = current.getInt("relative_humidity_2m");
                        int wmo = current.getInt("weather_code");

                        JSONObject daily = root.getJSONObject("daily");
                        JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
                        JSONArray minArr = daily.getJSONArray("temperature_2m_min");
                        JSONArray codeArr = daily.getJSONArray("weather_code");
                        JSONArray uvArr = daily.getJSONArray("uv_index_max");
                        JSONArray rainArr = daily.getJSONArray("precipitation_probability_max");

                        int todayHigh = (int) Math.round(maxArr.getDouble(0));
                        int todayLow = (int) Math.round(minArr.getDouble(0));
                        int rainProb = rainArr.isNull(0) ? 10 : rainArr.getInt(0);
                        int uvMax = uvArr.isNull(0) ? 6 : (int) Math.round(uvArr.getDouble(0));

                        String uvLevel = uvMax <= 2 ? "Thấp" : (uvMax <= 5 ? "TB" : (uvMax <= 7 ? "Cao" : "Rất cao"));
                        final String tempStr = temp + "°";
                        final String condStr = WeatherIconView.conditionTextFromWmo(wmo);
                        final String hlStr = "Cao " + todayHigh + "° · Thấp " + todayLow + "°";
                        final int iconType = WeatherIconView.fromWmoCode(wmo);
                        final String rainStr = rainProb + "%";
                        final String humStr = hum + "%";
                        final String uvStr = uvMax + " · " + uvLevel;

                        final String[] fDays = new String[5];
                        final int[] fIcons = new int[5];
                        final String[] fTemps = new String[5];
                        Calendar cal = Calendar.getInstance();
                        for (int i = 0; i < 5; i++) {
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                            fDays[i] = LunarCalendar.getShortWeekday(cal);
                            int idx = Math.min(i + 1, codeArr.length() - 1);
                            fIcons[i] = WeatherIconView.fromWmoCode(codeArr.getInt(idx));
                            int hi = (int) Math.round(maxArr.getDouble(idx));
                            int lo = (int) Math.round(minArr.getDouble(idx));
                            fTemps[i] = hi + "° / " + lo + "°";
                        }

                        String aqiTextFinal = prefs.getString("w_aqi", "75 · TB");
                        try {
                            String aqiUrl = String.format(Locale.US,
                                    "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%.4f&longitude=%.4f&current=us_aqi",
                                    lat, lon);
                            String aqiJson = httpGet(aqiUrl);
                            if (aqiJson != null) {
                                int aqi = new JSONObject(aqiJson).getJSONObject("current").getInt("us_aqi");
                                String aqiLvl = aqi <= 50 ? "Tốt" : (aqi <= 100 ? "TB" : (aqi <= 150 ? "Kém" : "Xấu"));
                                aqiTextFinal = aqi + " · " + aqiLvl;
                            }
                        } catch (Exception ignored) {
                        }
                        final String aqiStr = aqiTextFinal;

                        prefs.edit()
                                .putString("w_temp", tempStr)
                                .putString("w_cond", condStr)
                                .putString("w_hl", hlStr)
                                .putInt("w_icon", iconType)
                                .putString("w_rain", rainStr)
                                .putString("w_hum", humStr)
                                .putString("w_aqi", aqiStr)
                                .putString("w_uv", uvStr)
                                .apply();

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cityLabel.setText("⌖ " + CITY_NAMES[cityIdx]);
                                mainTempText.setText(tempStr);
                                mainConditionText.setText(condStr);
                                mainHighLowText.setText(hlStr);
                                mainWeatherIcon.setIconType(iconType);
                                rainProbVal.setText(rainStr);
                                humidityVal.setText(humStr);
                                aqiVal.setText(aqiStr);
                                uvVal.setText(uvStr);
                                for (int i = 0; i < 5; i++) {
                                    forecastDayText[i].setText(fDays[i]);
                                    forecastIconView[i].setIconType(fIcons[i]);
                                    setForecastTempSpan(forecastTempText[i], fTemps[i]);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                }

                fetchGoldTrend();
                fetchStockIndex("VNINDEX", "m_vni_pt", "m_vni_chg", "m_vni_dir", vnIndexPointText, vnIndexChangeBadge);
                fetchStockIndex("VN30", "m_vn30_pt", "m_vn30_chg", "m_vn30_dir", vn30PointText, vn30ChangeBadge);

                try {
                    String fxJson = httpGet("https://open.er-api.com/v6/latest/USD");
                    if (fxJson != null) {
                        JSONObject rates = new JSONObject(fxJson).getJSONObject("rates");
                        double vnd = rates.getDouble("VND");
                        float prevVnd = prefs.getFloat("m_usd_raw", 0f);
                        int calcDir = prefs.getInt("m_usd_dir", 1);
                        if (prevVnd > 1000f && Math.abs(vnd - prevVnd) >= 1.0) {
                            calcDir = (vnd > prevVnd) ? 1 : -1;
                        }
                        final int usdDir = calcDir;
                        final String usdFormatted = String.format(Locale.GERMAN, "%,.0f", vnd);
                        final String nowTime = "Cập nhật " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        prefs.edit()
                                .putString("m_usd", usdFormatted)
                                .putFloat("m_usd_raw", (float) vnd)
                                .putInt("m_usd_dir", usdDir)
                                .putString("m_time", nowTime)
                                .apply();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                usdVcbText.setText(usdFormatted);
                                styleGoldTrendBadge(usdTrendBadge, usdDir);
                                marketUpdateText.setText(nowTime);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void fetchGoldTrend() {
        try {
            String cgJson = httpGet("https://api.coingecko.com/api/v3/simple/price?ids=pax-gold&vs_currencies=usd&include_24hr_change=true");
            if (cgJson != null) {
                JSONObject paxg = new JSONObject(cgJson).optJSONObject("pax-gold");
                if (paxg != null && paxg.has("usd_24h_change")) {
                    double chg = paxg.getDouble("usd_24h_change");
                    final int dir = chg > 0.02 ? 1 : (chg < -0.02 ? -1 : 0);
                    prefs.edit()
                            .putInt("m_sjc_dir", dir)
                            .putInt("m_ring_dir", dir)
                            .apply();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            styleGoldTrendBadge(sjcTrendBadge, dir);
                            styleGoldTrendBadge(ringTrendBadge, dir);
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void fetchStockIndex(final String symbol, final String prefPt, final String prefChg, final String prefDir,
                                 final TextView pointView, final TextView badgeView) {
        try {
            long nowSec = System.currentTimeMillis() / 1000L;
            long fromSec = nowSec - 14L * 86400L;
            String url = "https://dchart-api.vndirect.com.vn/dchart/history?resolution=D&symbol="
                    + symbol + "&from=" + fromSec + "&to=" + nowSec;
            String json = httpGet(url);
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                JSONArray closes = obj.optJSONArray("c");
                if (closes != null && closes.length() >= 2) {
                    int n = closes.length();
                    double last = closes.getDouble(n - 1);
                    double prev = closes.getDouble(n - 2);
                    double diff = last - prev;
                    double pct = (prev != 0) ? (diff / prev) * 100.0 : 0.0;

                    final int dir = diff > 0.005 ? 1 : (diff < -0.005 ? -1 : 0);
                    String arrow = dir > 0 ? "▲ +" : (dir < 0 ? "▼ " : "▬ ");
                    String signPct = dir > 0 ? "+" : "";
                    final String ptStr = String.format(Locale.GERMAN, "%,.2f", last);
                    final String chgStr = String.format(Locale.GERMAN, "%s%.2f (%s%.2f%%)", arrow, diff, signPct, pct);

                    prefs.edit()
                            .putString(prefPt, ptStr)
                            .putString(prefChg, chgStr)
                            .putInt(prefDir, dir)
                            .apply();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            pointView.setText(ptStr);
                            styleStockChangeBadge(badgeView, pointView, chgStr, dir);
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV; Mecool KM2)");
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private byte[] downloadBytes(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV; Mecool KM2)");
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc != null) {
                    return downloadBytes(loc);
                }
                return null;
            }
            if (code == 200) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    bos.write(buf, 0, read);
                }
                is.close();
                return bos.toByteArray();
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }
}
