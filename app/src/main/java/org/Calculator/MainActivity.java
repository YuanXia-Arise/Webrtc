package org.Calculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.squareup.leakcanary.LeakCanary;
import com.webrtc.R;

import org.Util.DisplayUtil;
import org.Util.MyApp;
import org.activity.LauncherActivity;
import org.webrtc.PrefSingleton;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.activity.App.BUS;

public class MainActivity extends AppCompatActivity {

    //public static MainActivity activity;
    private Context context;
    private Toolbar toolbar;
    private EditText inText;
    //private TextView stateText;
    private TextView outText;
    private ViewPager drawerPager;
    private DrawerLayout drawer;
    private ArrayList<View> drawerPageList;
    public FrameLayout delete;

    public int[] XX = {1, 3, 1, 3};
    public int[] YY = {6, 5, 5, 5};

    //private static final String[] OPERATOR = {"??", "??", "-", "+","???","^","(",")"};
    private final String[] OPERATOR = {"??", "??", "-", "+"};

    private final String[][] BUTTON = {
            {"???","^","sqrt", "cbrt", "root", "rand", "randInt", "abs", "lg", "ln", "log",
                    "min", "max", "fact", "sin", "cos", "tan", "asin", "acos",
                    "atan", "re", "im", "arg", "norm", "reg", "conj", "diff",
                    "sum", "lim", "eval", "fzero", "integ", "exp", "gcd", "lcm",
                    "perm", "comb", "gamma", "round", "floor", "ceil", "sign",
                    "remn", "prime", "isPrime", "prec", "base"},
            {"ans", "reg", "??", "e", "F", "h", "??", "??", "??", "c",
                    "N", "R", "K", "k", "G", "??", "true", "false", "me", "mn", "mp"}};


    /*private String[][] BUTTON_VICE = {
            {"?????????", "?????????", "??????", "????????????", "????????????", "?????????", "????????????", "????????????", "??????",
                    "??????", "??????", "??????", "??????", "??????", "??????", "?????????", "?????????", "?????????", "??????",
                    "??????", "??????", "??????", "??????", "????????????", "?????????", "????????????", "??????", "??????",
                    "????????????", "?????????", "e?????????", "????????????", "????????????", "??????", "??????", "????????????",
                    "????????????", "????????????", "????????????", "????????????", "??????", "??????", "????????????", "????????????", "????????????"},
            {"????????????", "?????????", "?????????", "????????????", "?????????", "?????????", "???????????????",
                    "??????", "????????????", "??????", "???????????????", "????????????", "??????", "????????????",
                    "????????????", "????????????", "???", "???", "????????????", "????????????", "????????????"}};*/

    private final Pattern FUNCTIONS_KEYWORDS = Pattern.compile(
            "\\b(" + "sqrt|cbrt|root|rand|randInt|lg|ln|log|abs|min|max|fact|" +
                    "sin|cos|tan|asin|acos|atan|re|im|arg|norm|reg|conj|diff|" +
                    "sum|lim|eval|fzero|integ|exp|gcd|lcm|perm|comb|round|floor|" +
                    "ceil|sign|gamma|remn|prime|isPrime|prec|base|??" + ")\\b");

    private final Pattern CONSTANS_KEYWORDS2 = Pattern.compile(
            "\\b(" + "ans|reg|true|false|me|mn|mp" + ")\\b");

    private final Pattern CONSTANS_KEYWORDS1 = Pattern.compile("[???i??%??eFh??????cNRkG??]");


    private final String[] NUMERIC = {
            "7", "8", "9",
            "4", "5", "6",
            "1", "2", "3",
            "??", "0", "=",
            "%", "(", ")"};

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //activity = this;
        context = getApplicationContext();
        super.onCreate(savedInstanceState);
        /*if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(getApplication()); // ??????????????????*/
        DisplayUtil.setDensity(this, getApplication());  // ????????????UI??????
        setContentView(R.layout.activity_main);
        PrefSingleton.getInstance().Initialize(getApplicationContext());
        if (PrefSingleton.getInstance().getString("psk").equals("")) PrefSingleton.getInstance().putString("psk","111111");

        PrefSingleton.getInstance().putBoolean("flow_mode", false);
        PrefSingleton.getInstance().putBoolean("key_mode", false);
        PrefSingleton.getInstance().putBoolean("voice_mode", false);
        PrefSingleton.getInstance().putBoolean("speak_mode", false);
        PrefSingleton.getInstance().putBoolean("focus_mode", false);
        PrefSingleton.getInstance().putBoolean("recorder_mode", false);
        PrefSingleton.getInstance().putBoolean("water_mode", false);

        // ????????????
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //AudioManager.STREAM_SYSTEM AudioManager.STREAM_RING
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE); // ????????????,????????????
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0); // ??????
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0); // ??????
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        initToolBar();
        initEditText();
        initTextView();
        initDrawer();
        initPages();
        initTabs();
        initDelete();
        initNumeric();
        initOperator();
        initFunction();
    }

    //private static final long CLICK_INTERVAL_TIME = 300;
    //private static long lastClickTime = 0;
    private void initDelete() {
        delete = (FrameLayout) findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*long currentTimeMillis = SystemClock.uptimeMillis();
                if (currentTimeMillis - lastClickTime < CLICK_INTERVAL_TIME) {
                    startActivity(new Intent(getApplicationContext(),LauncherActivity.class));
                    finish();
                    return;
                }
                lastClickTime = currentTimeMillis;*/

                Editable editable = inText.getText();
                int index = inText.getSelectionStart();
                int index2 = inText.getSelectionEnd();
                if (index == index2) {
                    if (index == 0) return;
                    editable.delete(index - 1, index);
                } else {
                    editable.delete(index, index2);
                }
            }
        });
        delete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ExpressionHandler.stop();
                inText.setText(null);
                return true;
            }
        });

    }

    private void initTextView() {
        outText = (TextView) findViewById(R.id.text_out);
        outText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(rootValue);
                Snackbar.make(v, "?????????????????????", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void initDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_main);
        findViewById(R.id.drawer_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    private void initTabs() {
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs_main);
        tabs.setupWithViewPager(drawerPager);
        tabs.getTabAt(0).setText("??????");
        tabs.getTabAt(1).setText("??????");
    }

    private void initPages() {
        drawerPageList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            GridView gridView = new GridView(this);
            drawerPageList.add(gridView);
        }

        drawerPager = (ViewPager) findViewById(R.id.viewPager_drawer);
        MainPagerAdapter drawerPagerAdapter = new MainPagerAdapter(drawerPageList);
        drawerPager.setAdapter(drawerPagerAdapter);
        drawerPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
                } else {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.END);
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void initNumeric() {
        GridView numericBar = (GridView) findViewById(R.id.bar_numeric);
        numericBar.setNumColumns(XX[1]);
        numericBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = position == 9 ? "." : NUMERIC[position];
                if (str.equals("=")) {
                    if (calcThread != null) {
                        Snackbar.make(view, "???????????????????????????", Snackbar.LENGTH_SHORT)
                                .setAction("????????????", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ExpressionHandler.stop();
                                    }
                                }).show();
                        return;
                    }
                    outText.setTextColor(0xffbdbdbd);
                    calcThread = new Calc(inText.getText().toString());
                    calcThread.start();
                    return;
                }
                modifyInText(str);
            }
        });
        GridViewAdapter numericAdapter = new GridViewAdapter(numericBar, Arrays.asList(NUMERIC),
                null, R.layout.button_numeric, YY[1], this);
        numericBar.setAdapter(numericAdapter);
    }

    private void initOperator() {
        GridView operatorBar = (GridView) findViewById(R.id.bar_operator);
        operatorBar.setNumColumns(XX[2]);
        operatorBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = position == 0 ? "??" : OPERATOR[position];
                modifyInText(position == 1 ? "??" : str);
            }
        });
        GridViewAdapter operatorAdapter = new GridViewAdapter(operatorBar, Arrays.asList(OPERATOR),
                null, R.layout.button_operator, YY[2],this);
        operatorBar.setAdapter(operatorAdapter);
    }

    private void initFunction() {
        int i = 0;
        for (View view : drawerPageList) {
            GridView operatorProBar = (GridView) view;
            operatorProBar.setNumColumns(XX[3]);

            if (i == 0) {
                operatorProBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        modifyInText((BUTTON[0][position].equals("gamma") ? "??" : BUTTON[0][position]) + "()");
                    }
                });

                operatorProBar.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        String text = BUTTON[0][position];
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle(text);
                        dialog.setMessage(HelpUtil.getFunctionHelp(text));
                        dialog.setPositiveButton("??????", null);
                        dialog.show();
                        return true;
                    }
                });
            } else {
                operatorProBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        modifyInText(BUTTON[1][position]);
                    }
                });
            }
            int id = i == 0 ? R.layout.button_function : R.layout.button_constant;
            /*GridViewAdapter operatorProAdapter = new GridViewAdapter(operatorProBar,
                    Arrays.asList(BUTTON[i++]), Arrays.asList(BUTTON_VICE[i - 1]), id, YY[3]);*/
            GridViewAdapter operatorProAdapter = new GridViewAdapter(operatorProBar,
                    Arrays.asList(BUTTON[i++]), null, id, YY[3], this);

            operatorProBar.setAdapter(operatorProAdapter);
        }
    }

    private void modifyInText(String str) {
        int index = inText.getSelectionStart();
        int index2 = inText.getSelectionEnd();
        if (index == index2) {
            inText.getText().insert(index, str);
        } else {
            inText.getText().replace(index, index2, str);
        }
    }

    class FastCalc extends Thread implements Runnable {
        private String exp;

        public FastCalc(String exp) {
            this.exp = exp;
        }

        @Override
        public void run() {
            final long t = System.currentTimeMillis();
            final String[] value = ExpressionHandler.calculation(exp);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    outText.setTextColor(0xffbdbdbd);
                    if (value[0].getBytes().length > 1000) {
                        outText.setText("??????????????????????????????????????????");
                    } else
                        outText.setText(value[0]);
                    rootValue = value[0];
                    calcThread = null;
                }
            });
        }
    }

    class Calc extends Thread implements Runnable {
        private String exp;

        public Calc(String exp) {
            this.exp = exp;
        }

        @Override
        public void run() {
            final long t = System.currentTimeMillis();
            final String[] value = ExpressionHandler.calculation(exp);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (PrefSingleton.getInstance().getString("psk").equals(outText.getText().toString())) {
                        startActivity(new Intent(getApplicationContext(),LauncherActivity.class));
                        finish();
                    }
                    if (value[1].equals("true")) {
                        outText.setTextColor(0xffff4081);
                        outText.setText(value[0]);
                    } else {
                        Constants.setAns(value[0]);
                        if (value[0].getBytes().length > 1000) {
                            outText.setText("??????????????????????????????????????????");
                        } else
                            outText.setText(value[0]);
                    }
                    rootValue = value[0];
                    calcThread = null;
                }
            });
        }

    }

    private boolean modified = true;
    private int selection = 0;
    private Thread calcThread;
    private String rootValue;

    private void initEditText() {
        inText = (EditText) findViewById(R.id.editText);
        AutofitHelper.create(inText).setMinTextSize(28).setMaxLines(1);
        inText.requestFocus();
        inText.requestFocusFromTouch();
        inText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    outText.setTextColor(0xffbdbdbd);
                    outText.setText(null);
                    rootValue = null;
                    return;
                }

                if (calcThread == null) {
                    calcThread = new FastCalc(s.toString());
                    calcThread.start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!modified) return;

                selection = inText.getSelectionStart();
                s.clearSpans();

                for (Matcher m = Pattern.compile("x").matcher(s); m.find(); )
                    s.setSpan(new ForegroundColorSpan(0xfff48fb1), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                for (Matcher m = CONSTANS_KEYWORDS1.matcher(s); m.find(); )
                    s.setSpan(new ForegroundColorSpan(0xfffff59d), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                for (Matcher m = CONSTANS_KEYWORDS2.matcher(s); m.find(); )
                    s.setSpan(new ForegroundColorSpan(0xfffff59d), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                for (Matcher m = FUNCTIONS_KEYWORDS.matcher(s); m.find(); )
                    s.setSpan(new ForegroundColorSpan(0xffa5d6a7), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                for (Matcher m = Pattern.compile("[()\\-??+.,??!^=???%]").matcher(s); m.find(); )
                    s.setSpan(new ForegroundColorSpan(0xff81d4fa), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                modified = false;
                inText.setText(s);
                modified = true;

                if (selection >= 2 && s.toString().substring(selection - 2, selection).equals("()"))
                    selection--;
                inText.setSelection(selection);
            }
        });
    }

    private void initToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(null);
        toolbar.setSubtitle("????????????");
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawerPager.setCurrentItem(0);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
            drawer.closeDrawer(GravityCompat.END);
            return;
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


}
