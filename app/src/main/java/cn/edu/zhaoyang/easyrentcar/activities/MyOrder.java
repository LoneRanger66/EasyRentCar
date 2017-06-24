package cn.edu.zhaoyang.easyrentcar.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import cn.edu.zhaoyang.easyrentcar.R;
import cn.edu.zhaoyang.easyrentcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcar.util.Constants;
import cn.edu.zhaoyang.easyrentcar.util.NetworkHelper;

public class MyOrder extends AppCompatActivity {

    private static final String TAG = "MyOrder";
    private ListView listView;
    private ArrayList<HashMap<String, Object>> listItem = new ArrayList();
    private Handler handler = new Handler();
    private MyAdapter adapter;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_order);
        initToolbar("我的订单");
        listView = (ListView) findViewById(R.id.listView);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        //根据listView点击的选项设定carId
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap hashMap = (HashMap) parent.getItemAtPosition(position);
                orderId = (String) hashMap.get("orderId");
                Log.d(TAG, "点击的orderId为：" + orderId);
                Intent intent = new Intent(MyOrder.this, MyOrderDetails.class);
                Bundle bundle = new Bundle();
                bundle.putString("orderId", orderId);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        initOrder();
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
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
            /*
            如果屏幕在相应位置没有绘制listView，加载相应布局文件到convertView，寻找id存入ViewHolder，将这个ViewHolder放入convertView中
            否则，从convertView中取出ViewHolder，直接利用里面存储的id值
             */
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.item_user_order, null);
                viewHolder.carName = (TextView) convertView.findViewById(R.id.carNameTextView);
                viewHolder.orderTime = (TextView) convertView.findViewById(R.id.orderTimeTextView);
                viewHolder.carRent = (TextView) convertView.findViewById(R.id.carRentTextView);
                viewHolder.orderStatus = (TextView) convertView.findViewById(R.id.orderStatusTextView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            HashMap hashMap = listItem.get(position);
            viewHolder.carName.setText((String) hashMap.get("carName"));
            viewHolder.orderTime.setText((String) hashMap.get("orderTime"));
            viewHolder.carRent.setText(String.valueOf((double) hashMap.get("carRent")));
            int orderStatus = (int) hashMap.get("orderStatus");
            viewHolder.orderStatus.setText(Constants.orderStatus[orderStatus]);
            if (orderStatus == 0) {
                convertView.setBackgroundResource(R.color.yellow);
            } else {
                convertView.setBackgroundResource(0);
            }
            return convertView;
        }
    }

    //静态类，用来存储相应的id，加快响应速度
    static class ViewHolder {
        public TextView carName;
        public TextView orderTime;
        public TextView carRent;
        public TextView orderStatus;
    }

    /**
     * 用来请求用户所有的订单信息，存储到Activity中定义的listItem中
     */
    private void initOrder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String servletName = "UserOrder";
                JSONObject sndJsonObj = new JSONObject();
                MyApplication app = (MyApplication) getApplication();
                NetworkHelper.addUsernameAndPassword(sndJsonObj, app.getUsername(), app.getPassword());
                JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                if (rcvJsonObj == null) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                JSONArray order;
                try {
                    order = rcvJsonObj.getJSONArray("order");
                } catch (JSONException e) {
                    Log.d(TAG, "接收到的JSON格式错误！");
                    return;
                }
                if (order == null || order.length() == 0) {
                    Log.d(TAG, "用户没有订单信息！");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MyOrder.this).setTitle("提示").setMessage("用户没有订单信息！").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).show();
                        }
                    });
                    return;
                }
                JSONObject temp;
                listItem.clear();
                for (int i = 0; i < order.length(); i++) {
                    try {
                        temp = order.getJSONObject(i);
                        HashMap hashMap = new HashMap();
                        hashMap.put("orderId", temp.getString("orderId"));
                        hashMap.put("carName", temp.getString("carName"));
                        hashMap.put("orderTime", temp.getString("orderTime"));
                        hashMap.put("carRent", temp.getDouble("rent"));
                        hashMap.put("orderStatus", temp.getInt("orderStatus"));
                        listItem.add(hashMap);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //将未完成的订单排到前面
                for (int i = listItem.size() - 1; i >= 0; i--) {
                    HashMap<String, Object> hashMap = listItem.get(i);
                    if ((int) hashMap.get("orderStatus") == 0) {
                        listItem.add(0, hashMap);
                        listItem.remove(i + 1);
                        break;
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
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
