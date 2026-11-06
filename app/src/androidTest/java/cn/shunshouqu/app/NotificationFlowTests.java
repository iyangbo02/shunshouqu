package cn.shunshouqu.app;
import android.test.AndroidTestCase;
import android.content.Intent;
import java.util.*;
public class NotificationFlowTests extends AndroidTestCase {
    private PickupStore s;
    protected void setUp(){s=new PickupStore(getContext());}
    protected void tearDown(){s.close();}
    private void send(String mode,String code,int id,long when){
        Intent i=new Intent().setClassName("cn.shunshouqu.fixture","cn.shunshouqu.fixture.FixtureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("mode",mode).putExtra("id",id).putExtra("when",when).putExtra("text","【合成测试】包裹已到合成测试东门菜鸟驿站，取件码"+code+"。");getContext().startActivity(i);
    }
    private List<PickupStore.Item> matching(String code){ArrayList<PickupStore.Item> out=new ArrayList<>();for(boolean picked:new boolean[]{false,true})for(var p:s.list(picked))if(p.code().equals(code)&&p.source().equals("通知 · cn.shunshouqu.fixture"))out.add(p);return out;}
    private void await(String code) throws Exception {long end=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<end){if(!matching(code).isEmpty())return;Thread.sleep(250);}fail("Notification did not reach the application database");}
    private void flow(String mode,int id) throws Exception {
        String code="TEST-"+id+"-"+(System.currentTimeMillis()%1000000);long when=System.currentTimeMillis();send(mode,code,id,when);await(code);assertEquals(1,matching(code).size());assertEquals("合成测试东门菜鸟驿站",matching(code).get(0).station());long record=matching(code).get(0).id();s.picked(record,true);send(mode,code,id,when);Thread.sleep(1200);assertEquals(1,matching(code).size());assertTrue(matching(code).get(0).picked());s.delete(record);
    }
    public void testPlainNotificationThroughListener() throws Exception{flow("plain",801);}
    public void testExpandedNotificationThroughListener() throws Exception{flow("big",802);}
    public void testInboxNotificationThroughListener() throws Exception{flow("lines",803);}
    public void testMessagingNotificationThroughListener() throws Exception{flow("messages",804);}
}
