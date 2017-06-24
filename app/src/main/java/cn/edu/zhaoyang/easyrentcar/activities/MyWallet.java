package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class MyWallet extends AppCompatActivity implements View.OnClickListener {
    private TextView moneyTextView;
    private EditText chargeMoneyEditText;
    private Button chargeMoneyButton;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final String TAG = "MyWallet";
    private Handler handler = new Handler();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_wallet);
        initToolbar("我的钱包");
        moneyTextView = (TextView) findViewById(R.id.moneyTextView);
        chargeMoneyEditText = (EditText) findViewById(R.id.chargeMoneyEditText);
        chargeMoneyButton = (Button) findViewById(R.id.chargeMoneyButton);
        chargeMoneyButton.setOnClickListener(this);
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onClick(View v) {
        if (chargeMoneyEditText.getText().length() == 0) {
            return;
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "Recharge";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                double chargeMoney = Double.parseDouble(chargeMoneyEditText.getText().toString());
                try {
                    sndJsonObj.put("chargeMoney", chargeMoney);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                final JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                boolean status = false;
                try {
                    status = rcvJsonObj.getBoolean("status");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (status) {
                    Log.d(TAG, "充值成功");
                    Intent intent = new Intent(MyWallet.this, RechargeSuccess.class);
                    intent.putExtra("chargeMoney", chargeMoney);
                    startActivity(intent);
                    finish();
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MyWallet.this).setTitle("提示").setMessage("充值失败，请重试").setPositiveButton("确定", null).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(MyWallet.this, "请稍候...");
        customProgressDialog.show();
        //设置当前钱包金额
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "UserWallet";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                final JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                double money = 0;
                try {
                    money = rcvJsonObj.getDouble("money");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final double finalMoney = money;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        moneyTextView.setText(String.valueOf(finalMoney));
                        customProgressDialog.dismiss();
                    }
                });
            }
        });
    }

    /**
     * 点击返回按钮，回到上一级
     *
     * @param item 点击的项目
     * @return 点击的是toolbar的图标则返回true，否则交由上级处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPool.shutdown();
    }
}
