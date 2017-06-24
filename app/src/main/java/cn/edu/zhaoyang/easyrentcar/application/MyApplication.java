package cn.edu.zhaoyang.easyrentcar.application;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

/**
 * Created by ZhaoYang on 2017/2/26.
 */

public class MyApplication extends Application {
    private boolean isLogin;
    private String username;
    private String password;
    private String key;
    private String carId;
    private String carBluetoothAddress;
    private BluetoothSocket socket;
    private double longitude;
    private double latitude;

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarBluetoothAddress() {
        return carBluetoothAddress;
    }

    public void setCarBluetoothAddress(String carBluetoothAddress) {
        this.carBluetoothAddress = carBluetoothAddress;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isLogin = false;
        username = "未登录";
    }
}
