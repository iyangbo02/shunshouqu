package cn.shunshouqu.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

/** Production SQLite helper on Robolectric; each test has its own isolated database. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
public class ReminderStoreTest {
    private Context context;
    private PickupStore store;
    private long time;
    private static final String JD="通知 · com.jingdong.app.mall", TB="通知 · com.taobao.taobao";
    private PickupParser.Candidate candidate(String code,String station,String text){return new PickupParser.Candidate(code,station,"",text);}
    private PickupParser.Candidate reminder(){return candidate("",PickupParser.UNKNOWN,"合成包裹待取件");}
    private PickupParser.Candidate coded(){return candidate("008321","合成东门菜鸟驿站","合成包裹已到站，取件码008321");}
    @Before public void setUp(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("settings",0).edit().clear().commit();context.deleteDatabase("reminder_test.db");store=new PickupStore(context,"reminder_test.db");time=System.currentTimeMillis();}
    @After public void tearDown(){store.close();context.deleteDatabase("reminder_test.db");}

    @Test public void repeatedReminderAndDifferentApps(){
        assertTrue(store.add(reminder(),JD,time,true,"jd-1")>0);
        assertEquals(-1,store.add(reminder(),JD,time+1,true,"jd-1"));
        assertTrue(store.add(reminder(),TB,time,true,"tb-1")>0);
        assertEquals(2,store.list(false).size());
    }
    @Test public void singleNotificationFillsCodeAndKeepsId(){
        long id=store.add(reminder(),JD,time,true,"same");
        assertEquals(id,store.add(coded(),JD,time,true,"same"));
        assertEquals(1,store.list(false).size());assertEquals("008321",store.list(false).get(0).code());
        assertEquals(-1,store.add(reminder(),JD,time,true,"same"));
        assertEquals(-1,store.add(coded(),JD,time,true,"same"));
    }
    @Test public void completedReminderStaysCompletedAfterCodeArrives(){
        long id=store.add(reminder(),JD,time,true,"same");store.picked(id,true);
        assertEquals(id,store.add(coded(),JD,time,true,"same"));
        assertTrue(store.list(false).isEmpty());assertEquals("008321",store.list(true).get(0).code());
    }
    @Test public void laterMissingCodeViewDoesNotCreateSecondReminder(){
        store.add(coded(),JD,time,true,"same");
        assertEquals(-1,store.add(reminder(),JD,time,true,"same"));
        assertEquals(1,store.list(false).size());assertEquals("008321",store.list(false).get(0).code());
    }
    @Test public void deletedReminderDoesNotReturnOnUpdate(){
        long id=store.add(reminder(),JD,time,true,"same");store.delete(id);
        assertEquals(-1,store.add(coded(),JD,time,true,"same"));assertTrue(store.list(false).isEmpty());
    }
    @Test public void unrelatedNotificationDoesNotConsumeReminder(){
        store.add(reminder(),JD,time,true,"first");store.add(coded(),JD,time,true,"other");
        assertEquals(2,store.list(false).size());
    }
    @Test public void noIdentityDoesNotGuessMatching(){
        store.add(reminder(),JD,time,true,"");store.add(coded(),JD,time,true,"");
        assertEquals(2,store.list(false).size());
    }
    @Test public void changedReminderTextStillOneSingleNotification(){
        store.add(reminder(),JD,time,true,"same");
        assertEquals(-1,store.add(candidate("",PickupParser.UNKNOWN,"合成包裹待取件，请尽快领取"),JD,time,true,"same"));
        assertEquals(1,store.list(false).size());
    }
    @Test public void manualCodeIsNotOverwritten(){
        long id=store.add(reminder(),JD,time,true,"same");store.edit(id,"A-08-006","合成手动地点");
        store.add(coded(),JD,time,true,"same");assertEquals(2,store.list(false).size());
        assertTrue(store.list(false).stream().anyMatch(x->x.code().equals("A-08-006")));
    }
    @Test public void manualPlaceSurvivesMissingLocation(){
        long id=store.add(reminder(),JD,time,true,"same");store.edit(id,"","合成手动地点");
        store.add(candidate("008321",PickupParser.UNKNOWN,"合成取件码008321"),JD,time,true,"same");
        assertEquals("合成手动地点",store.list(false).get(0).station());assertFalse(store.list(false).get(0).review());
    }
    @Test public void knownCodeDedupStillWorksAcrossAppsAndAfterPick(){
        long id=store.add(coded(),JD,time,true);store.picked(id,true);
        assertEquals(-1,store.add(coded(),TB,time+1,true));assertTrue(store.list(false).isEmpty());
        assertTrue(store.add(coded(),JD,time+PickupStore.WINDOW+1,true)>0);
    }
    @Test public void oldIdentityExpiresAndClearRemovesLinks(){
        long id=store.add(reminder(),JD,time,true,"same");store.delete(id);
        assertTrue(store.add(coded(),JD,time+PickupStore.WINDOW+1,true,"same")>0);
        store.clear();assertTrue(store.add(coded(),JD,time,true,"same")>0);
    }
    @Test public void clearingIgnoresOldNotifications(){
        context.getSharedPreferences("settings",0).edit().putLong("ignoreBefore",time).commit();
        assertEquals(-1,store.add(reminder(),JD,time,true,"same"));
        assertTrue(store.add(reminder(),JD,time+1,true,"new")>0);
        context.getSharedPreferences("settings",0).edit().clear().commit();
    }
    @Test public void migratesVersionOneWithoutLosingHistory(){
        store.close();context.deleteDatabase("reminder_test.db");
        try(SQLiteDatabase d=context.openOrCreateDatabase("reminder_test.db",0,null)){
            d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL,station TEXT NOT NULL,carrier TEXT NOT NULL,source TEXT NOT NULL,excerpt TEXT NOT NULL,received INTEGER NOT NULL,created INTEGER NOT NULL,updated INTEGER NOT NULL,picked INTEGER NOT NULL DEFAULT 0,pickedAt INTEGER NOT NULL DEFAULT 0,review INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE seen(fingerprint TEXT NOT NULL,received INTEGER NOT NULL,itemId INTEGER NOT NULL)");
            d.execSQL("CREATE INDEX seen_lookup ON seen(fingerprint,received)");
            d.execSQL("INSERT INTO items VALUES(1,'000123','合成地点','','文字导入','',1,1,1,1,1,0)");d.setVersion(1);
        }
        store=new PickupStore(context,"reminder_test.db");
        assertEquals("000123",store.list(true).get(0).code());
        assertTrue(store.add(reminder(),JD,time,true,"migrated")>0);
        assertEquals(2,store.getReadableDatabase().getVersion());
    }
}
