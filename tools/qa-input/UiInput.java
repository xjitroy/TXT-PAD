package com.secure.textpad.qa.input;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

/** Shell-launched UI automation for synthetic native password input in the separate QA APK. */
public final class UiInput extends Instrumentation {
    private Bundle arguments;

    @Override public void onCreate(Bundle values) {
        super.onCreate(values);
        arguments = new Bundle(values);
        start();
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label, boolean input) {
        if (node == null) return null;
        CharSequence hint = node.getHintText();
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        boolean matches = input
                ? "android.widget.EditText".contentEquals(node.getClassName() == null ? "" : node.getClassName())
                    && hint != null && label.contentEquals(hint)
                : (text != null && label.contentEquals(text))
                    || (description != null && label.contentEquals(description));
        if (matches && node.isEnabled() && node.isVisibleToUser()) return node;
        for (int index = 0; index < node.getChildCount(); index++) {
            AccessibilityNodeInfo child = node.getChild(index);
            AccessibilityNodeInfo found = find(child, label, input);
            if (found != null) return found;
        }
        return null;
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            UiAutomation automation = getUiAutomation();
            boolean input = "input".equals(arguments.getString("mode"));
            String label = arguments.getString("label", "Password");
            long deadline = SystemClock.uptimeMillis() + 15000;
            boolean accepted = false;
            while (SystemClock.uptimeMillis() < deadline && !accepted) {
                AccessibilityNodeInfo node = find(automation.getRootInActiveWindow(), label, input);
                if (node != null) {
                    if (input) {
                        Bundle value = new Bundle();
                        value.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                arguments.getString("value", ""));
                        accepted = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, value);
                        value.clear();
                    } else {
                        for (int depth = 0; node != null && depth < 8; depth++, node = node.getParent()) {
                            if (node.isClickable() && node.isEnabled()) {
                                accepted = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                break;
                            }
                        }
                    }
                }
                if (!accepted) SystemClock.sleep(150);
            }
            if (!accepted) throw new IllegalStateException("Labelled native UI action was not accepted");
            result.putString("result", "PASS: native UI action accepted");
            finish(Activity.RESULT_OK, result);
        } catch (Exception error) {
            result.putString("result", "FAIL: " + error.getClass().getSimpleName());
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            arguments.clear();
        }
    }
}
