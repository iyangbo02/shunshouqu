package cn.shunshouqu.app;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/** Runs production SQLite code under Android's app_process; uses only a dedicated shell test directory. */
public class DeviceStoreHarness {
    static int passed;
    static void check(boolean ok,String description){if(!ok)throw new AssertionError(description);passed++;System.out.println("PASS "+description);}
    static PickupParser.Candidate c(String code,String place){return new PickupParser.Candidate(code,place,"","合成测试");}
    public static void main(String[] args){
        File dir=new File("/data/local/tmp/ssq-store-tests");dir.mkdirs();
        Context context=new ContextWrapper(null){
            public Context getApplicationContext(){return this;}
            public File getDatabasePath(String name){return new File(dir,name);}
            public String getPackageName(){return "cn.shunshouqu.harness";}
            public String getOpPackageName(){return getPackageName();}
            public ApplicationInfo getApplicationInfo(){ApplicationInfo a=new ApplicationInfo();a.targetSdkVersion=36;return a;}
            public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
            public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        };
        long t=System.currentTimeMillis();PickupStore s=new PickupStore(context,"isolated.db");s.clear();
        long id=s.add(c("008321","东门菜鸟驿站"),"合成测试",t,true);check(id>0,"insert");
        check(s.add(c("008321","东门菜鸟驿站"),"合成测试",t+1000,true)==-1,"repeat suppressed");
        s.picked(id,true);check(s.add(c("008321","东门菜鸟驿站"),"合成测试",t+1000,true)==-1&&s.list(false).isEmpty(),"picked not revived");
        check(s.add(c("008321","北门丰巢柜"),"合成测试",t,true)>0,"different station same code");
        check(s.add(c("008321","东门菜鸟驿站"),"合成测试",t+PickupStore.WINDOW+1,true)>0,"code reused after window");
        s.edit(id,"000123","合成新地点");s.close();s=new PickupStore(context,"isolated.db");
        check(s.list(true).get(0).code().equals("000123")&&s.list(true).get(0).station().equals("合成新地点"),"edit persisted after reopen");
        check(s.add(c("000123","合成新地点"),"合成测试",t,true)==-1,"edited alias deduplicated");
        s.picked(id,false);check(s.list(true).isEmpty(),"restore pending");
        s.delete(id);check(s.add(c("008321","东门菜鸟驿站"),"合成测试",t,true)==-1,"deleted not recreated by replay");
        s.clear();check(s.list(false).isEmpty()&&s.list(true).isEmpty(),"clear all statuses");
        for(var p:PickupParser.parse("包裹已到合成东门菜鸟驿站，取件码3-2-190。"))s.add(p,"合成测试",t,true);
        for(var p:PickupParser.parse("包裹已到合成东门菜鸟驿站，取件码3-2-190、6-1-025。"))s.add(p,"合成测试",t,true);
        check(s.list(false).size()==2,"updated multiple parcel notification");
        s.clear();s.close();System.out.println("PASS: "+passed+" production SQLite assertions on Android");
    }
}
