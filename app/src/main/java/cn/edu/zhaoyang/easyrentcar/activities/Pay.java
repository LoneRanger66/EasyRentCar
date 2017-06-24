package cn.edu.zhaoyang.easyrentcar.activities;

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

public class Pay extends AppCompatActivity implements View.OnClickListener {

    private TextView orderIdTextView;
    private TextView usernameTextView;
    private TextView carIdTextView;
    private TextView orderTimeTextView;
    private TextView startTimeTextView;
    private TextView endTimeTextView;
    private TextView rentTextView;
    private Button payButton;
    private double rent;
    private CustomProgressDialog customProgressDialog;
    private static final String TAG = "Pay";
    private Handler handler = new Handler();
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        initToolbar("用户付款");
        orderIdTextView = (TextView) findViewById(R.id.orderIdTextView);
        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        carIdTextView = (TextView) findViewById(R.id.carIdTextView);
        orderTimeTextView = (TextView) findViewById(R.id.orderTimeTextView);
        startTimeTextView = (TextView) findViewById(R.id.startTimeTextView);
        endTimeTextView = (TextView) findViewById(R.id.endTimeTextView);
        rentTextView = (TextView) findViewById(R.id.rentTextView);
        payButton = (Button) findViewById(R.id.payButton);
        payButton.setOnClickListener(this);
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private void dismissCustomProgressDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (customProgressDialog.isShowing()) {
                    customProgressDialog.dismiss();
                }
            }
        });
    }

    //付款按钮监听器
    @Override
    public void onClick(View v) {
        customProgressDialog.show();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String servletName = "Pay";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                try {
                    sndJsonObj.put("carId", app.getCarId());
                    sndJsonObj.put("rent", rent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    dismissCustomProgressDialog();
                    return;
                }
                boolean status = false;
                try {
                    status = rcvJsonObj.getBoolean("status");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (status) {
                    Log.d(TAG, "成功付款");
                    dismissCustomProgressDialog();
                    Intent intent = new Intent(Pay.this, PaySuccess.class);
                    startActivity(intent);
                    finish();
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            customProgressDialog.dismiss();
                            new AlertDialog.Builder(Pay.this).setTitle("提示").setMessage("余额不足，请充值！").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Pay.this, MyWallet.class);
                                    startActivity(intent);
                                }
                            }).show();
                        }
                    });
                }
            }
        });
    }

    class RequestOrderDetailRunnable implements Runnable {

        @Override
        public void run() {
            String servletName = "PaymentInformation";
            MyApplication app = (MyApplication) getApplication();
            JSONObject sndJsonObj = new JSONObject();
            try {
                sndJsonObj.put("carId", app.getCarId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
            JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
            if (rcvJsonObj == null) {
                Log.d(TAG, "接收到的JSON格式错误！");
                dismissCustomProgressDialog();
                return;
            }
            boolean status = false;
            try {
                status = rcvJsonObj.getBoolean("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (!status) {
                dismissCustomProgressDialog();
                return;
            }
            String rcvOrderId = null;
            String rcvCarId = null;
            String rcvUsername = null;
            String rcvOrderTime = null;
            String rcvStartTime = null;
            String rcvEndTime = null;
            try {
                rcvOrderId = rcvJsonObj.getString("orderId");
                rcvCarId = rcvJsonObj.getString("carId");
                rcvUsername = rcvJsonObj.getString("username");
                rcvOrderTime = rcvJsonObj.getString("orderTime");
                rcvStartTime = rcvJsonObj.optString("startTime", "无");
                rcvEndTime = rcvJsonObj.optString("endTime", "无");
                rent = rcvJsonObj.getDouble("rent");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final String finalRcvOrderId = "订单号：" + rcvOrderId;
            final String finalRcvUsername = rcvUsername;
            final String finalRcvCarId = rcvCarId;
            final String finalRcvOrderTime = rcvOrderTime;
            final String finalRcvStartTime = rcvStartTime;
            final String finalRcvEndTime = rcvEndTime;
            final String finalRcvRent = String.valueOf(rent);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    orderIdTextView.setText(finalRcvOrderId);
                    usernameTextView.setText(finalRcvUsername);
                    carIdTextView.setText(finalRcvCarId);
                    orderTimeTextView.setText(finalRcvOrderTime);
                    startTimeTextView.setText(finalRcvStartTime);
                    endTimeTextView.setText(finalRcvEndTime);
                    rentTextView.setText(finalRcvRent);
                    customProgressDialog.dismiss();
                }
            });

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(Pay.this, "请稍候...");
        customProgressDialog.show();
        threadPool.execute(new RequestOrderDetailRunnable());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadPool.shutdown();
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
