package cn.shunshouqu.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

final class SupportPrompt {
    // Separate from parcel data/settings. Clearing parcels must never reset this preference.
    static final String PREFERENCES="support";
    static final String SHOWN="first_clear_prompt_shown";

    static void open(Activity activity) { activity.startActivity(new Intent(activity,SupportActivity.class)); }

    static void showOnce(Activity activity) {
        SharedPreferences preferences=activity.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE);
        if(activity.isFinishing()||activity.isDestroyed()||preferences.getBoolean(SHOWN,false))return;
        int padding=Math.round(24*activity.getResources().getDisplayMetrics().density);
        TextView body=new TextView(activity);body.setTextSize(16);body.setTextColor(Color.rgb(28,44,36));body.setPadding(padding,padding/2,padding,padding/2);
        String link="去赞赏作者 →";
        SpannableString text=new SpannableString("如果顺手取帮到了你，可以自愿支持一下作者。赞赏与否都不影响使用。\n\n"+link+"\n\n这条提示只会出现一次，以后可在侧边栏「赞赏作者」中找到。\n卸载重装或清除应用数据后，提示记录会重置。");
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("谢谢使用顺手取").setPositiveButton("知道了",null).create();
        int start=text.toString().indexOf(link);
        text.setSpan(new ClickableSpan(){
            public void onClick(View widget){dialog.dismiss();open(activity);}
            public void updateDrawState(TextPaint paint){super.updateDrawState(paint);paint.setColor(Color.rgb(23,104,73));paint.setUnderlineText(true);}
        },start,start+link.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        body.setText(text);body.setMovementMethod(LinkMovementMethod.getInstance());
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);dialog.setView(scroll);
        // Persist as soon as it is displayed, including when dismissed with Back/outside touch.
        dialog.setOnShowListener(ignored->preferences.edit().putBoolean(SHOWN,true).commit());
        dialog.show();
    }
}
