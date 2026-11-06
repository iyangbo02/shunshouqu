package cn.shunshouqu.app;
import android.test.AndroidTestCase;
import android.app.Notification;
import android.os.Bundle;
public class StoreTests extends AndroidTestCase {
    PickupStore s;long t=1800000000000L;
    protected void setUp(){getContext().deleteDatabase("test_pickups.db");s=new PickupStore(getContext(),"test_pickups.db");}
    protected void tearDown(){s.clear();s.close();}
    PickupParser.Candidate c(String code,String station){return new PickupParser.Candidate(code,station,"菜鸟","合成样本");}
    public void testRepeatAndPickedNoRevival(){long id=s.add(c("008321","东门菜鸟驿站"),"测试",t,true);assertTrue(id>0);s.picked(id,true);assertEquals(-1L,s.add(c("008321","东门菜鸟驿站"),"测试",t+1000,true));assertEquals(0,s.list(false).size());assertEquals(1,s.list(true).size());}
    public void testOtherStationAndReuse(){s.add(c("008321","东门菜鸟驿站"),"测试",t,true);assertTrue(s.add(c("008321","北门丰巢柜"),"测试",t,true)>0);assertTrue(s.add(c("008321","东门菜鸟驿站"),"测试",t+PickupStore.WINDOW+1,true)>0);assertEquals(3,s.list(false).size());}
    public void testEditPersistenceRestoreDelete(){long id=s.add(c("008321","东门菜鸟驿站"),"测试",t,true);s.edit(id,"000123","北门丰巢柜");s.picked(id,true);s.close();s=new PickupStore(getContext(),"test_pickups.db");assertEquals("000123",s.list(true).get(0).code());assertEquals("北门丰巢柜",s.list(true).get(0).station());s.picked(id,false);assertEquals(1,s.list(false).size());s.delete(id);assertEquals(0,s.list(false).size());assertEquals(-1L,s.add(c("008321","东门菜鸟驿站"),"测试",t,true));}
    public void testClear(){s.add(c("1-02-003","东门"),"测试",t,true);s.clear();assertEquals(0,s.list(false).size());assertTrue(s.add(c("1-02-003","东门"),"测试",t,true)>0);}
    public void testMultipleParcelsUpdatedNotification(){for(var c:PickupParser.parse("包裹已到东门菜鸟驿站，取件码3-2-190。"))s.add(c,"测试",t,true);for(var c:PickupParser.parse("包裹已到东门菜鸟驿站，取件码3-2-190、6-1-025。"))s.add(c,"测试",t,true);assertEquals(2,s.list(false).size());}
    public void testNotificationStructures(){Notification n=new Notification();n.extras=new Bundle();String body="包裹已到东门菜鸟驿站，取件码3-2-190。";n.extras.putCharSequence(Notification.EXTRA_TITLE,"合成取件通知");n.extras.putCharSequence(Notification.EXTRA_TEXT,body);n.extras.putCharSequence(Notification.EXTRA_BIG_TEXT,body);n.extras.putCharSequenceArray(Notification.EXTRA_TEXT_LINES,new String[]{body,body});Bundle msg=new Bundle();msg.putCharSequence("text",body);n.extras.putParcelableArray(Notification.EXTRA_MESSAGES,new Bundle[]{msg});assertEquals(1,PickupParser.parse(PickupListener.extract(n)).size());}
}
