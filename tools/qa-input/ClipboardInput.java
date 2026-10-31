package com.secure.textpad.qa.input;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

/** Test-only clipboard input, callable only by a DUMP-privileged sender such as adb. */
public final class ClipboardInput extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String value = intent.getStringExtra("value");
        if (value == null) value = "";
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("TXT synthetic QA input", value);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        clipboard.setPrimaryClip(clip);
        setResultCode(Activity.RESULT_OK);
    }
}
