package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
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
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class Register extends AppCompatActivity {

    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText repeatEdit;
    private Button registerButton;
    private static final String TAG = "Register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initToolbar("用户注册");
        usernameEdit = (EditText) findViewById(R.id.usernameEdit);
        passwordEdit = (EditText) findViewById(R.id.passwordEdit);
        repeatEdit = (EditText) findViewById(R.id.repeatEdit);
        registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new registerButtonOnClickListener());
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private boolean isPhoneNumber(String input) {
        String regex = "^[1][3,4,5,8][0-9]{9}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    class registerButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String username = usernameEdit.getText().toString();
            if (!isPhoneNumber(username)) {
                Toast.makeText(Register.this, "手机号码格式错误！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!passwordEdit.getText().toString().equals(repeatEdit.getText().toString())) {
                Toast.makeText(Register.this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(new registerRunnable()).start();
        }
    }

    class registerRunnable implements Runnable {

        @Override
        public void run() {
            Looper.prepare();
            String username = usernameEdit.getText().toString();
            String password = passwordEdit.getText().toString();
            String servletName = "UserRegister";
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
            if (status) {
                Log.d(TAG, "验证通过");
                Toast.makeText(Register.this, "注册成功！", Toast.LENGTH_SHORT).show();
                MyApplication app = (MyApplication) getApplication();
                app.setLogin(true);
                app.setUsername(username);
                app.setPassword(password);
                Intent intent = new Intent(Register.this, MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(Register.this, "账号已被占用", Toast.LENGTH_SHORT).show();
            }
            Looper.loop();
        }
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
