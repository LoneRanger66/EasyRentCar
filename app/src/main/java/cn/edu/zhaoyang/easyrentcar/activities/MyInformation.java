package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class MyInformation extends AppCompatActivity {

    private TextView textView;
    private TextView usernameTextView;
    private TextView registerTimeTextView;
    private TextView moneyTextView;
    private Button logoutButton;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private final String TAG = "MyInformation";
    private Handler handler = new Handler();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_information);
        initToolbar("我的信息");
        textView = (TextView) findViewById(R.id.textView);
        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        registerTimeTextView = (TextView) findViewById(R.id.registerTimeTextView);
        moneyTextView = (TextView) findViewById(R.id.moneyTextView);
        logoutButton = (Button) findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new LogoutButtonOnClickListener());
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private class LogoutButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MyApplication app = (MyApplication) getApplication();
            app.setLogin(false);
            app.setUsername(null);
            app.setPassword(null);
            Intent intent = new Intent(MyInformation.this, Login.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(MyInformation.this, "请稍候...");
        customProgressDialog.show();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "UserInformation";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                String username = null;
                String registerTime = null;
                double money = 0;
                try {
                    username = rcvJsonObj.getString("username");
                    registerTime = rcvJsonObj.getString("registerTime");
                    money = rcvJsonObj.getDouble("money");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                final String finalUsername = username;
                final String finalRegisterTime = registerTime;
                final String finalMoney = "¥" + money;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(finalUsername);
                        usernameTextView.setText(finalUsername);
                        registerTimeTextView.setText(finalRegisterTime);
                        moneyTextView.setText(finalMoney);
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
        threadPool = null;
    }
}
