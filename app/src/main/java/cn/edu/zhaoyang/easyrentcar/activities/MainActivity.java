package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.MyLocationStyle;

import java.util.ArrayList;
import java.util.HashMap;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;

/**
 * 租车主界面
 */
public class MainActivity extends AppCompatActivity implements LocationSource, AMapLocationListener {

    private AMap aMap;
    private MapView mapView;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private DrawerLayout drawerLayout;
    private ListView listView;
    private Button rentCarButton;
    private TextView usernameTextView;
    private LinearLayout userInformation;
    private LinearLayout setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar("易租车");
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        init();
    }

    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        listView = (ListView) findViewById(R.id.left_drawer);
        rentCarButton = (Button) findViewById(R.id.rentCarButton);
        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        userInformation = (LinearLayout) findViewById(R.id.userInformation);
        setting = (LinearLayout) findViewById(R.id.setting);

        drawerLayout.addDrawerListener(new MyDrawerListener());
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, getArrayList(), R.layout.item_left_drawer, new String[]{"imageView", "textView"}, new int[]{R.id.imageView, R.id.textView});
        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(new ListViewOnItemClickListener());
        rentCarButton.setOnClickListener(new RentCarButtonOnClickListener());
        userInformation.setOnClickListener(new UserInformationOnClickListener());
        setting.setOnClickListener(new SettingOnClickListener());
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.user_icon);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private ArrayList getArrayList() {
        ArrayList<HashMap> arrayList = new ArrayList();
        String[] textList = {"我的订单", "我的信息", "我的钱包", "用户指南", "关于"};
        int[] imageList = {R.drawable.order, R.drawable.information, R.drawable.wallet, R.drawable.guide, R.drawable.about};
        for (int i = 0; i < textList.length; i++) {
            HashMap hashMap = new HashMap();
            hashMap.put("imageView", imageList[i]);
            hashMap.put("textView", textList[i]);
            arrayList.add(hashMap);
        }
        return arrayList;
    }

    /**
     * 设置一些aMap的属性
     */
    private void setUpMap() {
        aMap.moveCamera(CameraUpdateFactory.zoomTo(16));//设置缩放级别
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setScaleControlsEnabled(true);//设置比例尺功能可用
        aMap.getUiSettings().setCompassEnabled(true);//设置指南针可用
        aMap.getUiSettings().setZoomControlsEnabled(false);//禁用缩放按钮
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        setupLocationStyle();
    }

    private void setupLocationStyle() {
        // 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(STROKE_COLOR);
        //自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(5);
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(FILL_COLOR);
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
    }

    //ListView监听器
    private class ListViewOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MyApplication app = (MyApplication) getApplication();
            Intent intent = new Intent();
            if (!app.isLogin()) {
                intent.setClass(MainActivity.this, Login.class);
                startActivity(intent);
                return;
            }
            switch (position) {
                case 0:
                    intent.setClass(MainActivity.this, MyOrder.class);
                    startActivity(intent);
                    break;
                case 1:
                    intent.setClass(MainActivity.this, MyInformation.class);
                    startActivity(intent);
                    break;
                case 2:
                    intent.setClass(MainActivity.this, MyWallet.class);
                    startActivity(intent);
                    break;
                case 3:
                    intent.setClass(MainActivity.this, UserGuide.class);
                    startActivity(intent);
                    break;
                case 4:
                    intent.setClass(MainActivity.this, About.class);
                    startActivity(intent);
                    break;
                default:
                    break;
            }
        }
    }

    //租车按钮监听器
    private class RentCarButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            MyApplication app = (MyApplication) getApplication();
            //如果没有登录，用车按钮指向登录界面
            //如果登录了，用车按钮指向车辆预定界面
            if (!app.isLogin()) {
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, RentCar.class);
                startActivity(intent);
            }
        }
    }

    //DrawerLayout监听器
    private class MyDrawerListener implements DrawerLayout.DrawerListener {

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            MyApplication app = (MyApplication) getApplication();
            if (app.isLogin()) {
                usernameTextView.setText(app.getUsername());
            } else {
                usernameTextView.setText("未登录");
            }
        }

        @Override
        public void onDrawerOpened(View drawerView) {

        }

        @Override
        public void onDrawerClosed(View drawerView) {

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    }

    class UserInformationOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MyApplication app = (MyApplication) getApplication();
            if (!app.isLogin()) {
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, MyInformation.class);
                startActivity(intent);
            }
        }
    }

    class SettingOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, Setting.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            MyApplication app = (MyApplication) getApplication();
            if (app.isLogin()) {
                usernameTextView.setText(app.getUsername());
            } else {
                usernameTextView.setText("未登录");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                MyApplication app = (MyApplication) getApplication();
                app.setLongitude(amapLocation.getLongitude());
                app.setLatitude(amapLocation.getLatitude());
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("ERR", errText);
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            //开始定位
            mLocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * 当点击toolbar的图标的时候，打开drawerLayout
     *
     * @param item 点击的项目
     * @return 点击的是toolbar的图标则返回true，否则交由上级处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(Gravity.LEFT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 在drawerLayout打开的时候，将返回按钮的作用改为关闭drawerLayout
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                return true;
            } else {
                new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage("确定退出易租车？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                }).setNegativeButton("取消", null).show();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
