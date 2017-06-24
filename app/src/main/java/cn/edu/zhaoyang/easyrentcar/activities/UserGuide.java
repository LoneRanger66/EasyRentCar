package cn.edu.zhaoyang.easyrentcar.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.widget.TextView;

import cn.edu.zhaoyang.easyrentcar.R;

public class UserGuide extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        textView = (TextView) findViewById(R.id.textView);
        initToolbar("用户指南");
        setText();
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    //设置textView中的字体
    private void setText() {
        String string = "1.我怎样预定车辆？\n" +
                "如您是个人用户，您可以通过登陆盼达用车APP进行车辆预订。\n" +
                "2.如何得知我预定成功？\n" +
                "用户可以通过易租车APP中“我的订单”页面查阅。\n" +
                "3.如何取消车辆预定？\n" +
                "在预订成功界面点击“取消订单”\n" +
                "4.预定成功后，如何取车？\n" +
                "用户可以使用易租车APP，在租车界面点击“取车”按钮\n" +
                "5.成功下单后，如果没有取车还会收费吗？\n" +
                "用户需在订单生成后的30分钟内取车，若用户未取车，系统后台将在30分钟后计费\n" +
                "6.最短多少时间内可以取车？\n" +
                "用户下单成功后即可用车，无需等待。";
        SpannableString ss = new SpannableString(string);
        int start = 0;
        int end;
        boolean flag = true;
        while (start < string.length()) {
            //从string的start位置开始找第一个换行符出现的位置
            end = string.indexOf("\n", start);
            if (end == -1) {
                break;
            }
            if (flag) {
                ss.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new RelativeSizeSpan(1.2f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            flag = !flag;
            start = end + 1;
        }
        textView.setText(ss);
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
