package cn.edu.zhaoyang.easyrentcar.util;

/**
 * Created by ZhaoYang on 2017/3/6.
 * 常量类，不允许被继承
 */

public final class Constants {
    // 时戳允许误差时间（单位：毫秒）
    public static final long TIME_OUT = 100 * 1000;
    // 服务器地址
    public static String SERVER_ADDRESS = "http://192.168.1.110:8080/EasyRentCarServer/servlet/";
    // 字符编码
    public static final String CHARSET_NAME = "UTF-8";
    // AES key用字符串保存时的编码
    public static final String CHARSET_NAME_AESKEY = "ISO-8859-1";
    // 订单状态
    public static final int TRANSACTION_IN_PROGRESS = 0;
    public static final int TRANSACTION_PAID = 1;
    public static final int TRANSACTION_ORDER_CANCELED = 2;
    // 订单状态字符数组
    public static final String[] orderStatus = {"行程中", "已完成", "已取消"};
}
