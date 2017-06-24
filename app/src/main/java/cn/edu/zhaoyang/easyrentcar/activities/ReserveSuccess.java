package cn.edu.zhaoyang.easyrentcar.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.BluetoothHelper;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class ReserveSuccess extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private static final String TAG = "ReserveSuccess";
    private static final UUID MY_UUID = UUID.fromString("b1d0699c-b38e-4b73-88ab-f2991e714354");
    private String address;
    private Button cancelButton;
    private TextView positionTextView;
    private LinearLayout useCar;
    private BluetoothReceiver bluetoothReceiver;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket socket;
    private Handler handler = new Handler();
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private CustomProgressDialog customProgressDialog;
    private GeocodeSearch geocodeSearch;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserve_success);
        Bundle bundle = getIntent().getExtras();
        latitude = bundle.getDouble("latitude");
        longitude = bundle.getDouble("longitude");
        initToolbar("请到汽车旁取车");
        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
        getAddressByLatlng();
        positionTextView = (TextView) findViewById(R.id.positionTextView);
        useCar = (LinearLayout) findViewById(R.id.useCar);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        useCar.setOnClickListener(new UseCarOnClickListener());
        cancelButton.setOnClickListener(new CancelButtonOnClickListener());
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(ReserveSuccess.this, "请等待蓝牙连接");
        MyApplication app = (MyApplication) getApplication();
        address = app.getCarBluetoothAddress();
        //得到默认的蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //得到目标蓝牙设备
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        bluetoothReceiver = new BluetoothReceiver();
        //动态注册蓝牙搜索广播接收者
        IntentFilter intentFilter = new IntentFilter();
        //蓝牙状态改变，例如从关闭到开启
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //找到新的蓝牙设备
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //蓝牙绑定状态改变
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(bluetoothReceiver, intentFilter);
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private void getAddressByLatlng() {
        LatLonPoint latLonPoint = new LatLonPoint(latitude, longitude);
        //逆地理编码查询条件：逆地理编码查询的地理坐标点、查询范围、坐标类型。
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
        //异步查询
        geocodeSearch.getFromLocationAsyn(query);
    }

    /**
     * 根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
        String formatAddress = regeocodeAddress.getFormatAddress();
        Log.d(TAG, formatAddress);
        positionTextView.setText("汽车位置：" + formatAddress);
    }

    /**
     * 根据给定的地理名称和查询城市，返回地理编码的结果列表。
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    //显示提示框
    private void showDialog(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (customProgressDialog.isShowing()) {
                    customProgressDialog.dismiss();
                }
                new AlertDialog.Builder(ReserveSuccess.this).setTitle("提示").setMessage(msg).setPositiveButton("确定", null).show();
            }
        });
    }

    //取车按钮监听器
    private class UseCarOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            customProgressDialog.show();
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothAdapter == null) {
                        Log.d(TAG, "此设备不支持蓝牙");
                        showDialog("此设备不支持蓝牙");
                        return;
                    }
                    Log.d(TAG, "此设备支持蓝牙");
                    switch (bluetoothAdapter.getState()) {
                        //如果蓝牙没有打开，打开蓝牙
                        case BluetoothAdapter.STATE_OFF:
                            boolean status = bluetoothAdapter.enable();
                            Log.d(TAG, "正在打开蓝牙");
                            if (!status) {
                                Log.d(TAG, "蓝牙无法打开");
                                showDialog("蓝牙无法打开");
                                return;
                            }
                            break;
                        //如果蓝牙已开启，设置可检测性为永远可见，先取消搜索进程，再开始搜索可用蓝牙设备
                        case BluetoothAdapter.STATE_ON:
                            //设置可检测性为永远可见
                            Log.d(TAG, "设置蓝牙可见性");
                            BluetoothHelper.setScanMode(bluetoothAdapter);
                            //防止其他程序开启了蓝牙搜索进程
                            bluetoothAdapter.cancelDiscovery();
                            //如果目标蓝牙设备没有被绑定，则开始搜索
                            if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                                //开始寻找蓝牙设备
                                Log.d(TAG, "开始寻找蓝牙设备");
                                bluetoothAdapter.startDiscovery();
                            }
                            //如果目标蓝牙设备已经绑定了，则连接到其创建的socket
                            else {
                                connectToBluetoothServer();
                            }
                            break;
                        //其他的两种情况，等待蓝牙状态改变，由广播状态改变BluetoothAdapter.ACTION_STATE_CHANGED来处理
                        default:
                            break;
                    }
                }
            });

        }
    }

    /**
     * 蓝牙广播监听器
     * 初始状态为 未打开和正在打开，则监听BluetoothAdapter.ACTION_STATE_CHANGED 准备搜索
     * 初始状态为 打开，则开始搜索
     * 搜索过程中，收到目标设备就绑定
     */
    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            switch (action) {
                //如果收到的是找到了一个新设备，则进行验证MAC地址的操作，如果正确，就进行绑定
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (address.equals(bluetoothDevice.getAddress())) {
                        Log.d(TAG, "找到目标设备");
                        if (!checkDeviceIsBonded()) {
                            bluetoothAdapter.cancelDiscovery();
                            BluetoothHelper.createBond(bluetoothDevice);
                            Log.d(TAG, "绑定目标蓝牙设备");
                        }
                    }
                    break;
                //如果绑定状态改变了，则查看是否是目标设备被绑定了。如果是，则连接到目标socket
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    if (checkDeviceIsBonded()) {
                        connectToBluetoothServer();
                    }
                    break;
                /*
                如果收到了蓝牙状态的改变信息，则检查蓝牙是否开启.
                如果蓝牙已经开启，则检测设备是否已经被绑定。如果绑定了，调用connectToBluetoothServer()；否则设置蓝牙可见，并开始搜索进程
                */
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        //检测设备是否已经被绑定。如果绑定了的话，调用connectToBluetoothServer()
                        if (checkDeviceIsBonded()) {
                            connectToBluetoothServer();
                        } else {
                            //设置可检测性为永远可见
                            Log.d(TAG, "设置蓝牙可见性");
                            BluetoothHelper.setScanMode(bluetoothAdapter);
                            //防止其他程序开启了蓝牙搜索进程
                            bluetoothAdapter.cancelDiscovery();
                            //开始寻找蓝牙设备
                            Log.d(TAG, "开始寻找蓝牙设备");
                            bluetoothAdapter.startDiscovery();
                        }
                    }
                    break;
            }
        }

        //检查目标蓝牙设备是否已经被绑定
        private boolean checkDeviceIsBonded() {
            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                return true;
            } else {
                return false;
            }
        }
    }

    //连接蓝牙服务器，连接成功就跳转到ConnectSuccessActivity
    private void connectToBluetoothServer() {
        if (socket == null) {
            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //socket连接不能和蓝牙搜索进程共存，为了确认蓝牙搜索进程没有运行，必须进行取消蓝牙搜索进程的动作
            bluetoothAdapter.cancelDiscovery();
            if (!socket.isConnected()) {
                Log.d(TAG, "建立socket连接...");
                socket.connect();
            }
        } catch (IOException e) {
            Log.d(TAG, "socket创建失败");
            showDialog("socket创建失败");
            return;
        }
        Log.d(TAG, "socket创建成功");
        MyApplication app = (MyApplication) getApplication();
        app.setSocket(socket);
        if (customProgressDialog.isShowing()) {
            customProgressDialog.dismiss();
        }
        Intent intent = new Intent(ReserveSuccess.this, ConnectSuccess.class);
        startActivity(intent);
        finish();
    }

    //取消按钮监听器
    class CancelButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    String servletName = "CancelOrder";
                    JSONObject sndJsonObj = new JSONObject();
                    MyApplication app = (MyApplication) getApplication();
                    try {
                        sndJsonObj.put("carId", app.getCarId());
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
                    if (status == false) {
                        Log.d(TAG, "取消订单失败！");
                        Toast.makeText(ReserveSuccess.this, "取消订单失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!rcvJsonObj.has("rent")) {
                        Log.d(TAG, "取消订单成功！");
                        Toast.makeText(ReserveSuccess.this, "取消订单成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ReserveSuccess.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "订单需要支付费用！");
                        Intent intent = new Intent(ReserveSuccess.this, Pay.class);
                        startActivity(intent);
                        finish();
                    }
                    Looper.loop();
                }
            });
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
        this.unregisterReceiver(bluetoothReceiver);
        threadPool.shutdown();
    }
}
