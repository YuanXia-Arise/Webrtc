package org.java.voip;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.easydarwin.easypusher.R;


/**
 * 多人通话界面
 */
public class CallMultiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_call);
    }
}
