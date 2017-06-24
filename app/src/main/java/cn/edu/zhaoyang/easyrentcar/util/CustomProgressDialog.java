package cn.edu.zhaoyang.easyrentcar.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;

import cn.edu.zhaoyang.easyrentcar.R;

/**
 * Created by ZhaoYang on 2017/4/26.
 * 自定义ProgressDialog类，比原生界面美化了一些
 */

public class CustomProgressDialog extends Dialog {

    public CustomProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public static CustomProgressDialog getCustomProgressDialog(Context context, String msg) {
        CustomProgressDialog customProgressDialog = new CustomProgressDialog(context, R.style.CustomProgressDialog);
        customProgressDialog.setContentView(R.layout.custom_progress_dialog);
        customProgressDialog.getWindow().getAttributes().gravity = Gravity.CENTER;
        customProgressDialog.setCanceledOnTouchOutside(false);
        ((TextView) customProgressDialog.findViewById(R.id.msgTextView)).setText(msg);
        return customProgressDialog;
    }
}
