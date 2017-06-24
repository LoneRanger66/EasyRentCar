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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class Login extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private static final String TAG = "Login";
    private Handler handler = new Handler();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initToolbar("用户登录");
        loginButton = (Button) findViewById(R.id.loginButton);
        registerButton = (Button) findViewById(R.id.registerButton);
        usernameEdit = (EditText) findViewById(R.id.usernameEdit);
        passwordEdit = (EditText) findViewById(R.id.passwordEdit);
        loginButton.setOnClickListener(new LoginButtonOnClickListener());
        registerButton.setOnClickListener(new RegisterButtonOnClickListener());
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(Login.this, "登录中，请等待");
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    //登录按钮监听器
    class LoginButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String username = usernameEdit.getText().toString();
            if (!isPhoneNumber(username)) {
                Toast.makeText(Login.this, "手机号码格式错误！", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(new loginRunnable()).start();
        }
    }

    private boolean isPhoneNumber(String input) {
        String regex = "^[1][3,4,5,8][0-9]{9}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    //注册按钮监听器
    class RegisterButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        }
    }

    class loginRunnable implements Runnable {
        @Override
        public void run() {
            String username = usernameEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    customProgressDialog.show();
                }
            });
            String servletName = "UserLogin";
            JSONObject sndJsonObj = new JSONObject();
            NetworkHelper.addUsernameAndPassword(sndJsonObj, username, password);
            JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    customProgressDialog.dismiss();
                }
            });
            if (status) {
                Log.d(TAG, "验证通过");
                showToast("登录成功");
                MyApplication app = (MyApplication) getApplication();
                app.setLogin(true);
                app.setUsername(username);
                app.setPassword(password);
                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
            } else {
                showToast("账号或密码错误");
            }
        }
    }

    private void showToast(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Login.this, msg, Toast.LENGTH_SHORT).show();
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
}
