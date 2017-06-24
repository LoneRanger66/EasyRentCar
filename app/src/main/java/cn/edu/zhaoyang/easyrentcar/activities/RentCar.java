package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.Constants;
import cn.edu.zhaoyang.easyrentcar.util.CustomProgressDialog;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class RentCar extends AppCompatActivity {

    private Button confirmRentCarButton;
    private ImageView refreshImageView;
    private ListView listView;
    private static final String TAG = "RentCar";
    private String carId = null;
    private double latitude;
    private double longitude;
    private ArrayList<HashMap<String, Object>> listItem = new ArrayList();
    private Handler handler = new Handler();
    private MyAdapter adapter;
    private View oldView;
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private CustomProgressDialog customProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rent_car);
        init();
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    private void init() {
        initToolbar("当前可用车辆");
        customProgressDialog = CustomProgressDialog.getCustomProgressDialog(RentCar.this, "请稍候...");
        confirmRentCarButton = (Button) findViewById(R.id.confirmRentCarButton);
        refreshImageView = (ImageView) findViewById(R.id.refreshImageView);
        listView = (ListView) findViewById(R.id.listView);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        //设置刷新按钮监听器
        refreshImageView.setOnClickListener(new RefreshOnClickListener());
        //设置确认租车按钮监听器
        confirmRentCarButton.setOnClickListener(new ConfirmRentCarOnClickListener());
        //根据listView点击的选项设定carId
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (oldView != null) {
                    oldView.setBackgroundResource(0);
                }
                oldView = view;
                view.setBackgroundResource(android.R.color.holo_blue_dark);
                HashMap hashMap = (HashMap) parent.getItemAtPosition(position);
                carId = (String) hashMap.get("carId");
                Log.d(TAG, "点击的carId为：" + carId);
                latitude = (double) hashMap.get("latitude");
                longitude = (double) hashMap.get("longitude");
            }
        });
        refreshImageView.callOnClick();
    }

    /**
     * 自定义适配器类，用来处理listView中的数据
     */
    private class MyAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;

        public MyAdapter() {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return listItem.size();
        }

        @Override
        public Object getItem(int position) {
            return listItem.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //使用ViewHolder静态内部类存储id，在数据量大的情况下能大大提高listView的效率
            ViewHolder viewHolder;
            /*如果屏幕在相应位置没有绘制listView，加载相应布局文件到convertView，寻找id存入ViewHolder，
            并将这个ViewHolder放入convertView中；否则，从convertView中取出ViewHolder，直接利用里面存储的id值*/
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.item_available_car, null);
                viewHolder.carPicture = (ImageView) convertView.findViewById(R.id.carPictureImageView);
                viewHolder.carId = (TextView) convertView.findViewById(R.id.carIdTextView);
                viewHolder.carRent = (TextView) convertView.findViewById(R.id.carRentTextView);
                viewHolder.carName = (TextView) convertView.findViewById(R.id.carNameTextView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            HashMap<String, Object> hashMap = listItem.get(position);
            viewHolder.carId.setText("车牌号：" + hashMap.get("carId"));
            viewHolder.carPicture.setImageBitmap((Bitmap) hashMap.get("carPicture"));
            viewHolder.carName.setText("汽车名：" + hashMap.get("carName"));
            viewHolder.carRent.setText("租金/小时：¥" + String.valueOf((double) hashMap.get("carRent")));
            return convertView;
        }
    }

    //静态类，用来存储相应的id，加快响应速度
    static class ViewHolder {
        ImageView carPicture;
        TextView carId;
        TextView carRent;
        TextView carName;
    }

    private void dismissCustomProgressDialog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                customProgressDialog.dismiss();
            }
        });
    }

    /*
    函数用来得到汽车的图片
     */
    private Bitmap getCarPicture(String carPicture) {
        Future<Bitmap> future = threadPool.submit(new GetCarPictureCallable(carPicture));
        Bitmap bitmap = null;
        try {
            bitmap = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 自定义Callable类，用来得到相应汽车的图片，返回要使用的bitmap。
     */
    class GetCarPictureCallable implements Callable<Bitmap> {
        private String carPicture;

        public GetCarPictureCallable(String carPicture) {
            this.carPicture = carPicture;
        }

        @Override
        public Bitmap call() throws Exception {
            URL url;
            try {
                String servletName = "RequestCarPicture";
                url = new URL(Constants.SERVER_ADDRESS + servletName);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            conn.setDoInput(true);
            conn.setDoOutput(true);
            try {
                conn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            conn.setRequestProperty("Charset", Constants.CHARSET_NAME);
            conn.setConnectTimeout(10000);
            OutputStream out;
            try {
                out = conn.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            try {
                out.write(carPicture.getBytes(Constants.CHARSET_NAME));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream in;
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
    }

    /**
     * 刷新按钮监听器，用来请求可用汽车信息，存储到Activity中定义的listItem中
     */
    class RefreshOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            customProgressDialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String servletName = "AvailableCar";
                    JSONObject sndJsonObj = new JSONObject();
                    MyApplication app = (MyApplication) getApplication();
                    try {
                        sndJsonObj.put("longitude", app.getLongitude());
                        sndJsonObj.put("latitude", app.getLatitude());
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
                    JSONArray availableCar;
                    try {
                        availableCar = rcvJsonObj.getJSONArray("availableCar");
                    } catch (JSONException e) {
                        Log.d(TAG, "接收到的JSON格式错误！");
                        dismissCustomProgressDialog();
                        return;
                    }
                    if (availableCar == null) {
                        Log.d(TAG, "当前没有可用汽车！");
                        dismissCustomProgressDialog();
                        return;
                    }
                    JSONObject temp;
                    listItem.clear();
                    for (int i = 0; i < availableCar.length(); i++) {
                        try {
                            temp = availableCar.getJSONObject(i);
                            HashMap hashMap = new HashMap();
                            hashMap.put("carId", temp.getString("carId"));
                            hashMap.put("carName", temp.getString("carName"));
                            hashMap.put("carPicture", getCarPicture(temp.getString("carPicture")));
                            hashMap.put("carRent", temp.getDouble("carRent"));
                            hashMap.put("latitude", temp.getDouble("carLatitude"));
                            hashMap.put("longitude", temp.getDouble("carLongitude"));
                            listItem.add(hashMap);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            customProgressDialog.dismiss();
                        }
                    });
                }
            }).start();
        }
    }

    /**
     * 确定租车按钮的监听器
     */
    class ConfirmRentCarOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (carId == null) {
                new AlertDialog.Builder(RentCar.this).setTitle("提示").setMessage("请选择要租的车辆").setPositiveButton("确定", null).show();
            } else {
                customProgressDialog.show();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        String servletName = "DistributeKeyForUser";
                        MyApplication app = (MyApplication) getApplication();
                        JSONObject sndJsonObj = new JSONObject();
                        try {
                            sndJsonObj.put("carId", carId);
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
                        int status = 0;
                        try {
                            status = rcvJsonObj.getInt("status");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        /*
                        status含义:1代表预订成功，2代表用户有未结账的订单，3代表用户选择的汽车被占用
                         */
                        switch (status) {
                            case 1:
                                String key = null;
                                String rcvCarBluetoothAddress = null;
                                try {
                                    rcvCarBluetoothAddress = rcvJsonObj.getString("carBluetoothAddress");
                                    key = rcvJsonObj.getString("key");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Log.d(TAG, "成功获得虚拟钥匙");
                                app.setKey(key);
                                app.setCarId(carId);
                                app.setCarBluetoothAddress(rcvCarBluetoothAddress);
                                dismissCustomProgressDialog();
                                Intent intent = new Intent(RentCar.this, ReserveSuccess.class);
                                Bundle bundle = new Bundle();
                                bundle.putDouble("latitude", latitude);
                                bundle.putDouble("longitude", longitude);
                                intent.putExtras(bundle);
                                startActivity(intent);
                                finish();
                                break;
                            case 2:
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        customProgressDialog.dismiss();
                                        new AlertDialog.Builder(RentCar.this).setTitle("提示").setMessage("您有未结账的订单！").setPositiveButton("结账", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(RentCar.this, MyOrder.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }).show();
                                    }
                                });
                                break;
                            case 3:
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        customProgressDialog.dismiss();
                                        new AlertDialog.Builder(RentCar.this).setTitle("提示").setMessage("您选的汽车已被其他用户占用！").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                refreshImageView.callOnClick();
                                            }
                                        }).show();
                                    }
                                });
                                break;
                            default:
                                break;
                        }

                    }
                });
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
        threadPool = null;
    }
}
