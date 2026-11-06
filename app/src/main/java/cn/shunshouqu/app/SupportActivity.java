package cn.shunshouqu.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.*;

/** Displays the author's original, bundled image. No payment SDK or network requests. */
public final class SupportActivity extends Activity {
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        int green=Color.rgb(23,104,73), ink=Color.rgb(28,44,36), muted=Color.rgb(94,110,100);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246,247,242));
        root.setPadding(dp(16),dp(8),dp(16),dp(8));
        if(Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(dp(16),bars.top+dp(8),dp(16),bars.bottom+dp(8));return insets;
        });
        else root.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(dp(16),insets.getSystemWindowInsetTop()+dp(8),dp(16),insets.getSystemWindowInsetBottom()+dp(8));return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("返回");back.setTextColor(green);back.setMinHeight(dp(48));back.setOnClickListener(v->finish());
        header.addView(back);
        TextView title=new TextView(this);title.setText("赞赏作者");title.setTextSize(23);title.setTextColor(ink);title.setTypeface(null,Typeface.BOLD);
        title.setPadding(dp(12),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(4),dp(20),dp(4),dp(24));
        TextView message=new TextView(this);message.setText("如果顺手取帮到了你，\n欢迎自愿支持一下作者。");message.setTextColor(ink);message.setTextSize(21);message.setTypeface(null,Typeface.BOLD);message.setPadding(0,0,0,dp(12));body.addView(message);
        TextView voluntary=new TextView(this);voluntary.setText("赞赏完全自愿，不影响应用功能的使用。感谢你的支持！");voluntary.setTextColor(muted);voluntary.setTextSize(15);voluntary.setPadding(0,0,0,dp(20));body.addView(voluntary);
        ImageView image=new ImageView(this);image.setImageResource(R.drawable.support_code);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("作者的微信赞赏码，感谢支持");body.addView(image,new LinearLayout.LayoutParams(-1,-2));
        TextView instructions=new TextView(this);instructions.setText("可截图后在微信「扫一扫 → 相册」中识别，或使用另一台手机扫描。");instructions.setTextColor(muted);instructions.setTextSize(15);instructions.setPadding(0,dp(20),0,0);body.addView(instructions);
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
}
