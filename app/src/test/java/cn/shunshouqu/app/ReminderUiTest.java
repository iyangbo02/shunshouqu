package cn.shunshouqu.app;

import android.app.Notification;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
public class ReminderUiTest {
    @Before public void setUp(){RuntimeEnvironment.getApplication().deleteDatabase("pickups.db");RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putBoolean("welcomed",true).commit();}
    @After public void tearDown(){RuntimeEnvironment.getApplication().deleteDatabase("pickups.db");}
    private String text(View view){StringBuilder out=new StringBuilder();if(view instanceof TextView)out.append(((TextView)view).getText()).append('\n');if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)out.append(text(((ViewGroup)view).getChildAt(i)));return out.toString();}
    @Test public void noCodeShowsAppNameAndSeparateReminderCount(){
        try(PickupStore store=new PickupStore(RuntimeEnvironment.getApplication())){
            store.add(new PickupParser.Candidate("",PickupParser.UNKNOWN,"","合成待取件"),"通知 · com.jingdong.app.mall",System.currentTimeMillis(),true,"ui");
        }
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){
            String screen=text(controller.get().getWindow().getDecorView());
            assertTrue(screen.contains("京东有快递待取"));assertTrue(screen.contains("0 件 + 1 条提醒"));
            assertTrue(screen.contains("不代表包裹件数"));assertTrue(screen.contains("已处理"));
        }
    }
    @Test public void realNotificationExtrasYieldOneReminderOrOneCode(){
        Notification n=new Notification();n.extras=new Bundle();
        n.extras.putCharSequence(Notification.EXTRA_TITLE,"待取件");
        n.extras.putCharSequence(Notification.EXTRA_TEXT,"合成包裹已到东门菜鸟驿站，请打开应用查看");
        var reminders=PickupParser.parse(PickupListener.extract(n));assertEquals(1,reminders.size());assertEquals("",reminders.get(0).code());
        n.extras.putCharSequence(Notification.EXTRA_BIG_TEXT,"合成包裹已到东门菜鸟驿站，取件码008321");
        var coded=PickupParser.parse(PickupListener.extract(n));assertEquals(1,coded.size());assertEquals("008321",coded.get(0).code());
    }
}
