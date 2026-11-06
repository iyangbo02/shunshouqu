package cn.shunshouqu.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class PickupStore extends SQLiteOpenHelper {
    public static final long WINDOW = 72L * 60 * 60 * 1000;
    public record Item(long id,String code,String station,String carrier,String source,String excerpt,long received,long created,long updated,boolean picked,long pickedAt,boolean review) {}
    private final Context context;
    public PickupStore(Context c) { this(c,"pickups.db"); }
    PickupStore(Context c,String database) { super(c.getApplicationContext(),database,null,1);context=c.getApplicationContext(); setWriteAheadLoggingEnabled(true); }
    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL,station TEXT NOT NULL,carrier TEXT NOT NULL,source TEXT NOT NULL,excerpt TEXT NOT NULL,received INTEGER NOT NULL,created INTEGER NOT NULL,updated INTEGER NOT NULL,picked INTEGER NOT NULL DEFAULT 0,pickedAt INTEGER NOT NULL DEFAULT 0,review INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE seen(fingerprint TEXT NOT NULL,received INTEGER NOT NULL,itemId INTEGER NOT NULL)");
        d.execSQL("CREATE INDEX seen_lookup ON seen(fingerprint,received)");
    }
    public void onUpgrade(SQLiteDatabase d,int old,int n) { throw new IllegalStateException("Database migration required"); }
    private String fingerprint(PickupParser.Candidate c) { String s=c.station().trim().replaceAll("\\s+","")+"|"+c.code().toUpperCase(Locale.ROOT)+(c.code().isEmpty()?"|"+c.excerpt():"");try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(Locale.ROOT,"%02x",b & 255));return hex.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    public synchronized long add(PickupParser.Candidate c,String source,long received,boolean dedup) {
        SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
        try {
            if(source.startsWith("通知 · ") && received<=context.getSharedPreferences("settings",0).getLong("ignoreBefore",0)){d.setTransactionSuccessful();return -1;}
            String fp=fingerprint(c);
            if(dedup) try(Cursor x=d.rawQuery("SELECT itemId FROM seen WHERE fingerprint=? AND received BETWEEN ? AND ? LIMIT 1",new String[]{fp,""+(received-WINDOW),""+(received+WINDOW)})) {
                if(x.moveToFirst()) { d.setTransactionSuccessful(); return -1; }
            }
            long now=System.currentTimeMillis(); ContentValues v=new ContentValues();
            v.put("code",c.code());v.put("station",c.station().trim().isEmpty()?PickupParser.UNKNOWN:c.station());v.put("carrier",c.carrier());v.put("source",source);v.put("excerpt",c.excerpt());v.put("received",received);v.put("created",now);v.put("updated",now);v.put("review",c.code().trim().isEmpty()||c.station().equals(PickupParser.UNKNOWN)?1:0);
            long id=d.insertOrThrow("items",null,v);
            ContentValues seen=new ContentValues();seen.put("fingerprint",fp);seen.put("received",received);seen.put("itemId",id);d.insertOrThrow("seen",null,seen);
            d.setTransactionSuccessful(); return id;
        } finally { d.endTransaction(); }
    }
    public List<Item> list(boolean picked) {
        ArrayList<Item> items=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("items",null,"picked=?",new String[]{picked?"1":"0"},null,null,"station COLLATE LOCALIZED, created DESC")) {
            while(c.moveToNext()) items.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),c.getLong(7),c.getLong(8),c.getInt(9)==1,c.getLong(10),c.getInt(11)==1));
        } return items;
    }
    public void edit(long id,String code,String station) {
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
        try {
            try(Cursor c=d.query("items",new String[]{"received","excerpt"},"id=?",new String[]{""+id},null,null,null)){
                if(c.moveToFirst()){ContentValues seen=new ContentValues();seen.put("fingerprint",fingerprint(new PickupParser.Candidate(code,station,"",c.getString(1))));seen.put("received",c.getLong(0));seen.put("itemId",id);d.insertOrThrow("seen",null,seen);}
            }
            ContentValues v=new ContentValues();v.put("code",code);v.put("station",station);v.put("review",code.trim().isEmpty()||station.equals(PickupParser.UNKNOWN)?1:0);v.put("updated",System.currentTimeMillis());d.update("items",v,"id=?",new String[]{""+id});d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    public void picked(long id,boolean picked) { ContentValues v=new ContentValues();v.put("picked",picked?1:0);v.put("pickedAt",picked?System.currentTimeMillis():0);v.put("updated",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{""+id}); }
    // Keep an anonymous dedup tombstone, so replay cannot recreate a deleted parcel.
    public void delete(long id) { getWritableDatabase().delete("items","id=?",new String[]{""+id}); }
    public void clear() { SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try {d.delete("items",null,null);d.delete("seen",null,null);d.setTransactionSuccessful();}finally{d.endTransaction();} }
}
