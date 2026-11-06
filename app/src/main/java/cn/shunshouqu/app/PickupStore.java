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
    PickupStore(Context c,String database) { super(c.getApplicationContext(),database,null,2);context=c.getApplicationContext(); setWriteAheadLoggingEnabled(true); }
    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL,station TEXT NOT NULL,carrier TEXT NOT NULL,source TEXT NOT NULL,excerpt TEXT NOT NULL,received INTEGER NOT NULL,created INTEGER NOT NULL,updated INTEGER NOT NULL,picked INTEGER NOT NULL DEFAULT 0,pickedAt INTEGER NOT NULL DEFAULT 0,review INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE seen(fingerprint TEXT NOT NULL,received INTEGER NOT NULL,itemId INTEGER NOT NULL,source TEXT NOT NULL DEFAULT '')");
        d.execSQL("CREATE INDEX seen_lookup ON seen(fingerprint,received)");
        createNotificationLinks(d);
    }
    private void createNotificationLinks(SQLiteDatabase d){d.execSQL("CREATE TABLE notification_links(identity TEXT PRIMARY KEY,itemId INTEGER NOT NULL,received INTEGER NOT NULL)");}
    public void onUpgrade(SQLiteDatabase d,int old,int n) {
        if(old<2){d.execSQL("ALTER TABLE seen ADD COLUMN source TEXT NOT NULL DEFAULT ''");
            d.execSQL("UPDATE seen SET source=COALESCE((SELECT source FROM items WHERE items.id=seen.itemId),'')");createNotificationLinks(d);}
    }
    private String fingerprint(PickupParser.Candidate c) { String s=c.station().trim().replaceAll("\\s+","")+"|"+c.code().toUpperCase(Locale.ROOT)+(c.code().isEmpty()?"|"+c.excerpt():"");try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(Locale.ROOT,"%02x",b & 255));return hex.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    public synchronized long add(PickupParser.Candidate c,String source,long received,boolean dedup) {
        return add(c,source,received,dedup,"");
    }
    /** Identity is supplied only for a single-candidate system notification: key + original time. */
    public synchronized long add(PickupParser.Candidate c,String source,long received,boolean dedup,String identity) {
        SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
        try {
            if(source.startsWith("通知 · ") && received<=context.getSharedPreferences("settings",0).getLong("ignoreBefore",0)){d.setTransactionSuccessful();return -1;}
            String fp=fingerprint(c);
            if(dedup) try(Cursor x=d.rawQuery("SELECT itemId FROM seen WHERE fingerprint=? AND received BETWEEN ? AND ?"+(c.code().isEmpty()?" AND (source=? OR source='')":"")+" LIMIT 1",c.code().isEmpty()?new String[]{fp,""+(received-WINDOW),""+(received+WINDOW),source}:new String[]{fp,""+(received-WINDOW),""+(received+WINDOW)})) {
                if(x.moveToFirst()) {link(d,identity,x.getLong(0),received);d.setTransactionSuccessful(); return -1; }
            }
            long now=System.currentTimeMillis(); ContentValues v=new ContentValues();
            v.put("code",c.code());v.put("station",c.station().trim().isEmpty()?PickupParser.UNKNOWN:c.station());v.put("carrier",c.carrier());v.put("source",source);v.put("excerpt",c.excerpt());v.put("received",received);v.put("created",now);v.put("updated",now);v.put("review",c.code().trim().isEmpty()||c.station().equals(PickupParser.UNKNOWN)?1:0);
            // Only the very same single notification may fill a missing code. Never guess by app/station.
            if(dedup&&!identity.isEmpty())try(Cursor x=d.rawQuery("SELECT items.id,items.code,items.station FROM notification_links LEFT JOIN items ON items.id=notification_links.itemId WHERE identity=? AND notification_links.received BETWEEN ? AND ? AND (items.source=? OR items.id IS NULL)",new String[]{identity,""+(received-WINDOW),""+(received+WINDOW),source})){
                if(x.moveToFirst()&&x.isNull(0)){d.setTransactionSuccessful();return -1;}
                if(x.moveToFirst()&&c.code().isEmpty()){remember(d,fp,source,received,x.getLong(0));d.setTransactionSuccessful();return -1;}
                if(x.moveToFirst()&&x.getString(1).isEmpty()){
                    long id=x.getLong(0);
                    v.remove("created");
                    // Preserve an existing location, which may have been supplied manually.
                    if(!x.getString(2).equals(PickupParser.UNKNOWN)){v.put("station",x.getString(2));v.put("review",0);remember(d,fingerprint(new PickupParser.Candidate(c.code(),x.getString(2),c.carrier(),c.excerpt())),source,received,id);}
                    d.update("items",v,"id=?",new String[]{""+id});remember(d,fp,source,received,id);
                    d.setTransactionSuccessful();return id; // picked/pickedAt remain untouched.
                }
            }
            long id=d.insertOrThrow("items",null,v);
            remember(d,fp,source,received,id);link(d,identity,id,received);
            d.setTransactionSuccessful(); return id;
        } finally { d.endTransaction(); }
    }
    private void remember(SQLiteDatabase d,String fp,String source,long received,long id){ContentValues v=new ContentValues();v.put("fingerprint",fp);v.put("source",source);v.put("received",received);v.put("itemId",id);d.insertOrThrow("seen",null,v);}
    private void link(SQLiteDatabase d,String identity,long id,long received){if(identity.isEmpty())return;ContentValues v=new ContentValues();v.put("identity",identity);v.put("itemId",id);v.put("received",received);d.insertWithOnConflict("notification_links",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public List<Item> list(boolean picked) {
        ArrayList<Item> items=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("items",null,"picked=?",new String[]{picked?"1":"0"},null,null,"station COLLATE LOCALIZED, created DESC")) {
            while(c.moveToNext()) items.add(new Item(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),c.getLong(7),c.getLong(8),c.getInt(9)==1,c.getLong(10),c.getInt(11)==1));
        } return items;
    }
    public void edit(long id,String code,String station) {
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
        try {
            try(Cursor c=d.query("items",new String[]{"received","excerpt","source"},"id=?",new String[]{""+id},null,null,null)){
                if(c.moveToFirst())remember(d,fingerprint(new PickupParser.Candidate(code,station,"",c.getString(1))),c.getString(2),c.getLong(0),id);
            }
            ContentValues v=new ContentValues();v.put("code",code);v.put("station",station);v.put("review",code.trim().isEmpty()||station.equals(PickupParser.UNKNOWN)?1:0);v.put("updated",System.currentTimeMillis());d.update("items",v,"id=?",new String[]{""+id});d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }
    public void picked(long id,boolean picked) { ContentValues v=new ContentValues();v.put("picked",picked?1:0);v.put("pickedAt",picked?System.currentTimeMillis():0);v.put("updated",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{""+id}); }
    // Keep an anonymous dedup tombstone, so replay cannot recreate a deleted parcel.
    public void delete(long id) { getWritableDatabase().delete("items","id=?",new String[]{""+id}); }
    public void clear() { SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try {d.delete("items",null,null);d.delete("seen",null,null);d.delete("notification_links",null,null);d.setTransactionSuccessful();}finally{d.endTransaction();} }
}
