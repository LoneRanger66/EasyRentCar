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

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.Constants;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class MyOrderDetails extends AppCompatActivity {
    private static final String TAG = "MyOrderDetails";
    private TextView orderIdTextView;
    private TextView carIdTextView;
    private TextView carNameTextView;
    private TextView orderTimeTextView;
    private TextView startTimeTextView;
    private TextView endTimeTextView;
    private TextView rentTextView;
    private TextView orderStatusTextView;
    private Button payButton;
    private Handler handler = new Handler();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_order_details);
        initToolbar("我的订单详情");
        orderIdTextView = (TextView) findViewById(R.id.orderIdTextView);
        carIdTextView = (TextView) findViewById(R.id.carIdTextView);
        carNameTextView = (TextView) findViewById(R.id.carNameTextView);
        orderTimeTextView = (TextView) findViewById(R.id.orderTimeTextView);
        startTimeTextView = (TextView) findViewById(R.id.startTimeTextView);
        endTimeTextView = (TextView) findViewById(R.id.endTimeTextView);
        rentTextView = (TextView) findViewById(R.id.rentTextView);
        orderStatusTextView = (TextView) findViewById(R.id.orderStatusTextView);
        payButton = (Button) findViewById(R.id.payButton);
        payButton.setOnClickListener(new PayButtonOnClickListener());
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    class PayButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String servletName = "Pay";
                    MyApplication app = (MyApplication) getApplication();
                    JSONObject sndJsonObj = new JSONObject();
                    try {
                        sndJsonObj.put("carId", carIdTextView.getText().toString());
                        sndJsonObj.put("rent", Double.valueOf(rentTextView.getText().toString()));
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
                        Log.d(TAG, "成功付款");
                        Intent intent = new Intent(MyOrderDetails.this, PaySuccess.class);
                        startActivity(intent);
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MyOrderDetails.this).setTitle("提示").setMessage("余额不足，请充值！").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(MyOrderDetails.this, MyWallet.class);
                                        startActivity(intent);
                                    }
                                }).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void requestOrderDetails(final String orderId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String servletName = "UserOrderDetails";
                MyApplication app = (MyApplication) getApplication();
                JSONObject sndJsonObj = new JSONObject();
                try {
                    sndJsonObj.put("orderId", orderId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                String rcvOrderId = null;
                String rcvCarId = null;
                String rcvCarName = null;
                String rcvOrderTime = null;
                String rcvStartTime = null;
                String rcvEndTime = null;
                double rcvRent = 0;
                int rcvOrderStatus = 0;
                try {
                    rcvOrderId = rcvJsonObj.getString("orderId");
                    rcvCarId = rcvJsonObj.getString("carId");
                    rcvCarName = rcvJsonObj.getString("carName");
                    rcvOrderTime = rcvJsonObj.getString("orderTime");
                    rcvStartTime = rcvJsonObj.optString("startTime", "无");
                    rcvEndTime = rcvJsonObj.optString("endTime", "无");
                    rcvRent = rcvJsonObj.getDouble("rent");
                    rcvOrderStatus = rcvJsonObj.getInt("orderStatus");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (rcvOrderStatus == Constants.TRANSACTION_IN_PROGRESS) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            payButton.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            payButton.setVisibility(View.GONE);
                        }
                    });
                }
                final String finalRcvOrderId = "订单号：" + rcvOrderId;
                final String finalRcvCarId = rcvCarId;
                final String finalRcvCarName = rcvCarName;
                final String finalRcvOrderTime = rcvOrderTime;
                final String finalRcvStartTime = rcvStartTime;
                final String finalRcvEndTime = rcvEndTime;
                final String finalRcvRent = String.valueOf(rcvRent);
                final String finalRcvOrderStatus = Constants.orderStatus[rcvOrderStatus];
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        orderIdTextView.setText(finalRcvOrderId);
                        carIdTextView.setText(finalRcvCarId);
                        carNameTextView.setText(finalRcvCarName);
                        orderTimeTextView.setText(finalRcvOrderTime);
                        startTimeTextView.setText(finalRcvStartTime);
                        endTimeTextView.setText(finalRcvEndTime);
                        rentTextView.setText(finalRcvRent);
                        orderStatusTextView.setText(finalRcvOrderStatus);
                        if (customProgressDialog.isShowing()) {
                            customProgressDialog.dismiss();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(MyOrderDetails.this, "请稍等...");
        customProgressDialog.show();
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String orderId = (String) bundle.get("orderId");
        requestOrderDetails(orderId);
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
