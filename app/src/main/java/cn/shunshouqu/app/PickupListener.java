package cn.shunshouqu.app;

import android.app.Notification;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import java.util.*;
import java.util.concurrent.*;

public final class PickupListener extends NotificationListenerService {
    public static volatile boolean connected=false;
    public static final String CHANGED="cn.shunshouqu.app.CHANGED";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    public void onListenerConnected() { connected=true;changed();try {StatusBarNotification[] all=getActiveNotifications();if(all!=null)for(StatusBarNotification n:all)onNotificationPosted(n);}catch(SecurityException ignored){connected=false;changed();} }
    public void onListenerDisconnected(){connected=false;changed();}
    public void onDestroy(){connected=false;worker.shutdown();changed();super.onDestroy();}
    private void changed(){sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));}
    public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null||sbn.getPackageName().equals(getPackageName()))return;
        Notification n=sbn.getNotification();
        if((n.flags & Notification.FLAG_GROUP_SUMMARY)!=0)return;
        long received=n.when>0 && n.when<=System.currentTimeMillis()+60000?n.when:sbn.getPostTime();
        if(received<=getSharedPreferences("settings",0).getLong("ignoreBefore",0))return;
        String content=extract(n);String source="通知 · "+sbn.getPackageName();
        if(worker.isShutdown())return;
        worker.execute(()-> {try(PickupStore store=new PickupStore(this)){boolean inserted=false;for(PickupParser.Candidate c:PickupParser.parse(content))if(store.add(c,source,received,true)>0)inserted=true;if(inserted)changed();}catch(RuntimeException ignored){getSharedPreferences("settings",0).edit().putBoolean("receiveError",true).apply();changed();}});
    }
    static String extract(Notification n) {
        Bundle e=n.extras; if(e==null)return "";
        LinkedHashSet<String> parts=new LinkedHashSet<>();
        for(String key:new String[]{Notification.EXTRA_TITLE,Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT}){CharSequence t=e.getCharSequence(key);if(t!=null)parts.add(t.toString());}
        CharSequence[] lines=e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);if(lines!=null)for(CharSequence t:lines)if(t!=null)parts.add(t.toString());
        Parcelable[] messages=e.getParcelableArray(Notification.EXTRA_MESSAGES);if(messages!=null)for(Parcelable p:messages)if(p instanceof Bundle){CharSequence t=((Bundle)p).getCharSequence("text");if(t!=null)parts.add(t.toString());}
        return String.join("\n",parts);
    }
}
