package cn.edu.zhaoyang.easyrentcar.activities;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.BluetoothHelper;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class ConnectSuccess extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ConnectSuccess";
    private BluetoothSocket socket;
    private LinearLayout lock;
    private LinearLayout unlock;
    private LinearLayout openTrunk;
    private TextView carNameTextView;
    private TextView carStatusTextView;
    private boolean canUse = false;
    private boolean sndStartTime = false;
    private boolean sndEndTime = false;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private static Toast mToast = null;
    private Handler handler = new Handler();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_success);
        initToolbar("控制车辆");
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(ConnectSuccess.this, "请等待...");
        MyApplication app = (MyApplication) getApplication();
        socket = app.getSocket();
        lock = (LinearLayout) findViewById(R.id.lock);
        unlock = (LinearLayout) findViewById(R.id.unlock);
        openTrunk = (LinearLayout) findViewById(R.id.openTrunk);
        carNameTextView = (TextView) findViewById(R.id.carNameTextView);
        carStatusTextView = (TextView) findViewById(R.id.carStatusTextView);
        carNameTextView.setText(app.getCarId());
        lock.setOnClickListener(this);
        unlock.setOnClickListener(this);
        openTrunk.setOnClickListener(this);
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private void showToast(Context context, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            mToast.show();
        } else {
            mToast.cancel();
            mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lock:
                if (canUse) {
                    new AlertDialog.Builder(ConnectSuccess.this).setTitle("提示").setMessage("确定结束用车？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            carStatusTextView.setText("锁定");
                            showToast(ConnectSuccess.this, "锁车");
                            customProgressDialog.show();
                            endUseCar();
                        }
                    }).setNegativeButton("取消", null).show();
                }
                break;
            case R.id.unlock:
                if (!canUse) {
                    carStatusTextView.setText("正在使用");
                    showToast(ConnectSuccess.this, "开锁");
                    customProgressDialog.show();
                    startUseCar();
                } else {
                    new AlertDialog.Builder(ConnectSuccess.this).setTitle("提示").setMessage("汽车已经解锁！").setPositiveButton("确定", null).show();
                }
                break;
            case R.id.openTrunk:
                if (canUse) {
                    showToast(ConnectSuccess.this, "打开后备箱");
                    threadPool.execute(new BluetoothRunnable(3));
                } else {
                    new AlertDialog.Builder(ConnectSuccess.this).setTitle("提示").setMessage("请先解锁汽车！").setPositiveButton("确定", null).show();
                }
                break;
            default:
                break;
        }
    }

    //点击开锁时，向服务器发送开始使用汽车的时间
    private void startUseCar() {
        if (sndStartTime) {
            return;
        }
        sndStartTime = true;
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "StartUseCar";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                try {
                    sndJsonObj.put("carId", app.getCarId());
                    sndJsonObj.put("startTime", String.valueOf(System.currentTimeMillis()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
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
                    Log.d(TAG, "向服务器发送开始使用汽车时间成功");
                    canUse = true;
                    threadPool.execute(new BluetoothRunnable(2));
                }
            }
        });
    }

    //点击锁车时，向服务器发送结束使用汽车时间
    private void endUseCar() {
        if (sndEndTime) {
            return;
        }
        sndEndTime = true;
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "EndUseCar";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                try {
                    sndJsonObj.put("carId", app.getCarId());
                    sndJsonObj.put("endTime", String.valueOf(System.currentTimeMillis()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
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
                    Log.d(TAG, "向服务器发送结束使用汽车时间成功");
                    canUse = false;
                    threadPool.execute(new BluetoothRunnable(1));
                }
            }
        });
    }

    //向汽车端发送信息的Runnable
    class BluetoothRunnable implements Runnable {

        private int message;

        public BluetoothRunnable(int message) {
            this.message = message;
        }

        @Override
        public void run() {
            InputStream in;
            OutputStream out;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "得到输入输出流失败");
                return;
            }
            MyApplication app = (MyApplication) getApplication();
            boolean status = BluetoothHelper.sendAndReceiveMessage(message, app.getKey(), in, out);
            if (status) {
                Log.d(TAG, "响应成功");
            } else {
                Log.d(TAG, "响应失败");
            }
            if (mToast != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mToast.cancel();
                    }
                });
            }
            if (customProgressDialog != null && customProgressDialog.isShowing()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        customProgressDialog.dismiss();
                    }
                });
            }
            if (message == 1) {
                app.setKey(null);
                app.setCarBluetoothAddress(null);
                try {
                    app.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                app.setSocket(null);
                Intent intent = new Intent(ConnectSuccess.this, Pay.class);
                startActivity(intent);
                finish();
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPool.shutdown();
    }
}
