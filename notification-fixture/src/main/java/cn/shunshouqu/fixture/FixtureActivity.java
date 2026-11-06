package cn.shunshouqu.fixture;
import android.app.*;
import android.os.*;
public class FixtureActivity extends Activity {
    public void onCreate(Bundle b){super.onCreate(b);NotificationManager m=getSystemService(NotificationManager.class);m.createNotificationChannel(new NotificationChannel("synthetic","合成取件通知",NotificationManager.IMPORTANCE_DEFAULT));
        String mode=getIntent().getStringExtra("mode");int id=getIntent().getIntExtra("id",100);long when=getIntent().getLongExtra("when",System.currentTimeMillis());
        String body=getIntent().getStringExtra("text");if(body==null)body="【菜鸟驿站】您的包裹已到东门菜鸟驿站，取件码3-2-190，请及时领取。";
        Notification.Builder n=new Notification.Builder(this,"synthetic").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("合成测试 · 取件通知").setContentText(body).setWhen(when);
        if("big".equals(mode))n.setStyle(new Notification.BigTextStyle().bigText(body));
        if("lines".equals(mode))n.setStyle(new Notification.InboxStyle().addLine(body).addLine(body));
        if("messages".equals(mode))n.setStyle(new Notification.MessagingStyle(new Person.Builder().setName("合成发送者").build()).addMessage(body,when,new Person.Builder().setName("测试").build()));
        m.notify(id,n.build());finish();}
}
