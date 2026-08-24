package com.onetowncity.app;

import android.net.Uri;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.LauncherActivity;

public final class MainActivity extends LauncherActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Uri getLaunchingUrl() {
        return Uri.parse("https://onetowncity.com/");
    }
}
