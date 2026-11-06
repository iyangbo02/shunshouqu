package cn.shunshouqu.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private final int GREEN=Color.rgb(23,104,73), INK=Color.rgb(28,44,36), MUTED=Color.rgb(94,110,100), BG=Color.rgb(246,247,242);
    private PickupStore store; private LinearLayout root,content;private TextView state;private boolean history=false;private boolean registered=false;
    private final BroadcastReceiver changes=new BroadcastReceiver(){public void onReceive(Context c,Intent i){render();}};
    private final Handler handler=new Handler();
    private final Runnable poll=new Runnable(){public void run(){if(state!=null)state.setText(connectionText());handler.postDelayed(this,2000);}};
    public void onCreate(Bundle b){super.onCreate(b);store=new PickupStore(this);render();if(!getSharedPreferences("settings",0).getBoolean("welcomed",false))welcome();}
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag") // API 26-32 use the legacy overload; these broadcasts carry no input data.
    protected void onResume(){super.onResume();if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,new IntentFilter(PickupListener.CHANGED),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changes,new IntentFilter(PickupListener.CHANGED));registered=true;if(authorized()&&!PickupListener.connected)android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,PickupListener.class));render();handler.post(poll);}
    protected void onPause(){handler.removeCallbacks(poll);if(registered){unregisterReceiver(changes);registered=false;}super.onPause();}
    protected void onDestroy(){store.close();super.onDestroy();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private void bold(TextView t){t.setTypeface(null,Typeface.BOLD);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String label,View.OnClickListener action){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setTextColor(GREEN);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(action);return b;}
    private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private boolean authorized(){String v=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");if(v==null)return false;for(String part:v.split(":"))if(new ComponentName(this,PickupListener.class).flattenToString().equals(part))return true;return false;}
    private String connectionText(){if(!authorized())return "○ 未开启自动收集 · 手动功能可用";if(!PickupListener.connected)return "◌ 已授权，等待服务连接";if(getSharedPreferences("settings",0).getBoolean("receiveError",false))return "! 收集遇到问题，请重新打开应用或手动添加";return "● 通知服务已连接 · 等待取件通知";}
    private void render(){
        root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(8),dp(20),dp(8));
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(dp(20),i.top+dp(8),dp(20),i.bottom+dp(8));return insets;});
        // getInsets is API 30; older supported devices use the legacy system insets.
        if(Build.VERSION.SDK_INT<30)root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(20),i.getSystemWindowInsetTop()+dp(8),dp(20),i.getSystemWindowInsetBottom()+dp(8));return i;});
        setContentView(root);
        LinearLayout top=row();Button menu=button("☰",v->sidebar());menu.setContentDescription("打开侧边栏");menu.setMinWidth(0);menu.setMinimumWidth(0);top.addView(menu,new LinearLayout.LayoutParams(dp(48),-2));TextView title=text("顺手取",29,INK);bold(title);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));top.addView(button("设置",v->settings()));root.addView(top);
        state=text(connectionText(),12,MUTED);state.setContentDescription(connectionText());root.addView(state);
        LinearLayout tabs=row();tabs.addView(button(history?"待取":"待取 ✓",v->{history=false;render();}),new LinearLayout.LayoutParams(0,-2,1));tabs.addView(button(history?"已取记录 ✓":"已取记录",v->{history=true;render();}),new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);
        List<PickupStore.Item> items=store.list(history);long places=items.stream().map(PickupStore.Item::station).distinct().count();
        TextView count=text((history?"已处理 ":"待取 ")+itemCounts(items)+" · "+places+" 个地点",19,INK);bold(count);root.addView(count);gap(root,8);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=column();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(items.isEmpty()){
            gap(content,40);TextView empty=text(history?"还没有已取记录":"出门取件，一眼看全",25,INK);bold(empty);content.addView(empty);
            content.addView(text(history?"取到包裹后，点「已取」就会留在这里。":"粘贴取件通知，或手动添加。\n开启通知使用权后，新取件信息会自动收集。",16,MUTED));gap(content,16);
            if(!history)content.addView(button("开启通知自动收集",v->permission()));
        }
        String last="";for(PickupStore.Item item:items){if(!last.equals(item.station())){gap(content,16);List<PickupStore.Item> atPlace=new ArrayList<>();for(PickupStore.Item x:items)if(x.station().equals(item.station()))atPlace.add(x);TextView station=text(item.station()+"  ·  "+itemCounts(atPlace),18,INK);bold(station);content.addView(station);last=item.station();}card(item);}
        gap(root,8);LinearLayout actions=row();actions.addView(button("粘贴取件信息",v->importText()),new LinearLayout.LayoutParams(0,-2,1));Button add=button("＋ 手动添加",v->edit(null));add.setTextColor(Color.WHITE);add.setBackground(shape(GREEN,12));actions.addView(add,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
    }
    private String itemCounts(List<PickupStore.Item> items){long reminders=items.stream().filter(x->x.code().trim().isEmpty()).count();return (items.size()-reminders)+" 件"+(reminders>0?" + "+reminders+" 条提醒":"");}
    private String reminderTitle(PickupStore.Item item){String label=sourceLabel(item.source());String name=item.source().startsWith("通知 · ")?label.replaceFirst("^通知 · ",""):"";return name+(item.picked()?"取件提醒已处理":"有快递待取");}
    private String sourceLabel(String source){
        if(!source.startsWith("通知 · "))return source;
        String pkg=source.substring("通知 · ".length());
        if(pkg.equals("com.android.shell"))return "合成测试通知";
        try{return "通知 · "+getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0));}catch(android.content.pm.PackageManager.NameNotFoundException e){
            switch(pkg){case "com.jingdong.app.mall":return "通知 · 京东";case "com.taobao.taobao":return "通知 · 淘宝";case "com.xunmeng.pinduoduo":return "通知 · 拼多多";case "com.cainiao.wireless":return "通知 · 菜鸟";default:return "通知 · 来源 App";}
        }
    }
    private void card(PickupStore.Item item){
        LinearLayout c=column();c.setPadding(dp(16),dp(10),dp(16),dp(10));c.setBackground(shape(Color.WHITE,16));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);content.addView(c,cp);
        boolean reminder=item.code().trim().isEmpty();
        TextView code=text(reminder?reminderTitle(item):item.code(),reminder?22:34,INK);bold(code);code.setTextIsSelectable(true);c.addView(code);
        if(reminder)c.addView(text("通知未提供取件码，请打开来源应用核对。\n这是一条提醒，不代表包裹件数；可编辑补充取件码。",13,MUTED));
        else if(item.review())c.addView(text("请核对并补全取件信息",13,Color.rgb(139,87,17)));
        c.addView(text(sourceLabel(item.source())+" · "+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(item.received())),12,MUTED));
        LinearLayout buttons=row();buttons.addView(button(item.picked()?"恢复待取":reminder?"✓ 已处理":"✓ 已取",v->{store.picked(item.id(),!item.picked());boolean finished=!item.picked()&&store.list(false).isEmpty();render();toast(item.picked()?"已恢复到待取":"已移入已取记录，可随时恢复");if(finished)SupportPrompt.showOnce(this);}),new LinearLayout.LayoutParams(0,-2,1));buttons.addView(button("编辑",v->edit(item)),new LinearLayout.LayoutParams(0,-2,1));buttons.addView(button("详情",v->detail(item)),new LinearLayout.LayoutParams(0,-2,1));c.addView(buttons);
    }
    private LinearLayout form(){LinearLayout l=column();l.setPadding(dp(20),dp(8),dp(20),dp(8));return l;}
    private EditText input(LinearLayout l,String label,String value,boolean multiline){l.addView(text(label,14,MUTED));EditText e=new EditText(this);e.setText(value);e.setTextSize(17);e.setSingleLine(!multiline);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|(multiline?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0)|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setMinHeight(dp(48));l.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
    private void edit(PickupStore.Item item){LinearLayout l=form();EditText station=input(l,"驿站或柜机地点",item==null?"":item.station(),true);EditText code=input(l,"取件码（按原样保留字母和前导零）",item==null?"":item.code(),true);AlertDialog d=new AlertDialog.Builder(this).setTitle(item==null?"添加取件":"编辑取件").setView(l).setNegativeButton("取消",null).setPositiveButton("保存",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String s=station.getText().toString().trim(),c=code.getText().toString().trim();if(c.isEmpty()||c.length()>64||s.length()>120){toast("请填写取件码；地点最多120字，取件码最多64字");return;}if(s.isEmpty())s=PickupParser.UNKNOWN;if(item==null)store.add(new PickupParser.Candidate(c,s,"",""),"手动添加",System.currentTimeMillis(),false);else store.edit(item.id(),c,s);d.dismiss();render();toast("已保存");}));d.show();}
    private void importText(){LinearLayout l=form();l.addView(text("仅在你主动粘贴后处理；可一次输入多条。",14,MUTED));EditText e=input(l,"取件通知文字","",true);e.setMinLines(5);e.setMaxLines(9);l.addView(button("从剪贴板粘贴",v->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm.hasPrimaryClip()&&cm.getPrimaryClip()!=null)e.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));}));AlertDialog d=new AlertDialog.Builder(this).setTitle("粘贴取件信息").setView(l).setNegativeButton("取消",null).setPositiveButton("识别并预览",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{List<PickupParser.Candidate> cs=PickupParser.parse(e.getText().toString());if(cs.isEmpty()){toast("没有找到明确取件码，可检查文字或手动添加");return;}d.dismiss();preview(cs);}));d.show();}
    private void preview(List<PickupParser.Candidate> candidates){LinearLayout l=form();l.addView(text("请逐条核对，可修改后保存。重复信息会跳过。",14,MUTED));ArrayList<EditText[]> fields=new ArrayList<>();for(PickupParser.Candidate c:candidates){gap(l,12);EditText s=input(l,"地点",c.station(),true),code=input(l,"取件码（隐藏时可留空，稍后补充）",c.code(),true);fields.add(new EditText[]{s,code});}ScrollView scroll=new ScrollView(this);scroll.addView(l);AlertDialog d=new AlertDialog.Builder(this).setTitle("核对 "+candidates.size()+" 条取件信息").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{for(EditText[] f:fields)if(f[0].length()>120||f[1].length()>64){toast("地点最多120字，取件码最多64字");return;}int n=0;long now=System.currentTimeMillis();for(int i=0;i<fields.size();i++){EditText[] f=fields.get(i);String s=f[0].getText().toString().trim();PickupParser.Candidate old=candidates.get(i);if(store.add(new PickupParser.Candidate(f[1].getText().toString().trim(),s.isEmpty()?PickupParser.UNKNOWN:s,old.carrier(),old.excerpt()),"文字导入",now,true)>0)n++;}d.dismiss();render();toast("已保存 "+n+" 条，其余为近期重复信息");}));d.show();}
    private void detail(PickupStore.Item item){String msg="地点："+item.station()+"\n品牌："+(item.carrier().isEmpty()?"未知":item.carrier())+"\n来源："+sourceLabel(item.source())+"\n\n核对原文（最多600字）：\n"+(item.excerpt().trim().isEmpty()?"手动添加，无原文":item.excerpt());new AlertDialog.Builder(this).setTitle("取件详情").setMessage(msg).setPositiveButton("关闭",null).setNeutralButton("删除",(d,w)->new AlertDialog.Builder(this).setTitle("删除这条取件记录？").setMessage("删除后无法恢复；取件状态可通过「已取／恢复待取」调整。").setNegativeButton("取消",null).setPositiveButton("删除",(a,b)->{store.delete(item.id());render();toast("已删除");}).show()).show();}
    private void welcome(){new AlertDialog.Builder(this).setTitle("让取件信息，聚在一起").setMessage("顺手取从通知中自动收集取件信息，按地点整理成大字清单。\n\n通知使用权的系统范围较广，本应用只保留识别到的取件信息，在本机处理，无账号、不联网。不会读取历史短信。\n\n你可以跳过授权，随时粘贴文字或手动添加。").setCancelable(false).setNegativeButton("先手动使用",(d,w)->welcomed()).setPositiveButton("了解并去授权",(d,w)->{welcomed();permission();}).show();}
    private void welcomed(){getSharedPreferences("settings",0).edit().putBoolean("welcomed",true).apply();}
    private void permission(){new AlertDialog.Builder(this).setTitle("开启通知自动收集").setMessage("下一步在系统中找到「顺手取」并允许通知使用权。系统允许访问较广范围的通知；应用只在本地提取取件相关内容，不保存无关通知。\n\n部分手机需要在应用信息右上角允许「受限制的设置」。通知内容被隐藏时，请手动补全。授权后将检查系统仍保留的活动通知。").setNegativeButton("暂不开启",null).setPositiveButton("打开系统设置",(d,w)->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException e){toast("请在系统设置中搜索「通知使用权」");}}).show();}
    private void sidebar(){
        Dialog drawer=new Dialog(this);drawer.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout panel=column();panel.setBackgroundColor(BG);panel.setPadding(dp(20),dp(28),dp(20),dp(24));
        LinearLayout header=row();TextView name=text("顺手取",27,INK);bold(name);header.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        Button close=button("×",v->drawer.dismiss());close.setContentDescription("关闭侧边栏");header.addView(close,new LinearLayout.LayoutParams(dp(48),-2));panel.addView(header);
        panel.addView(text("取件信息，顺手看全",14,MUTED));gap(panel,28);
        panel.addView(button("待取清单",v->{drawer.dismiss();history=false;render();}));
        panel.addView(button("已取记录",v->{drawer.dismiss();history=true;render();}));gap(panel,16);
        panel.addView(button("赞赏作者",v->{drawer.dismiss();SupportPrompt.open(this);}));
        panel.addView(button("设置与数据",v->{drawer.dismiss();settings();}));
        scroll.addView(panel);drawer.setContentView(scroll);drawer.setCanceledOnTouchOutside(true);drawer.show();
        Window window=drawer.getWindow();if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));window.setGravity(Gravity.START|Gravity.TOP);window.setLayout(Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(48)),WindowManager.LayoutParams.MATCH_PARENT);window.setDimAmount(0.35f);}
    }
    private void clearAllRecords(){
        boolean hadRecords=!store.list(false).isEmpty()||!store.list(true).isEmpty();
        getSharedPreferences("settings",0).edit().putLong("ignoreBefore",System.currentTimeMillis()).apply();
        store.clear();render();toast("已清空");
        if(hadRecords)SupportPrompt.showOnce(this);
    }
    private void settings(){
        new AlertDialog.Builder(this).setTitle("设置与数据").setItems(new String[]{"通知使用权与连接状态","重新连接通知服务","隐私说明","清空全部记录"},(dialog,which)->{
            if(which==0)permission();
            if(which==1){if(authorized()){android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,PickupListener.class));toast("已请求连接，系统会决定连接时间");}else permission();}
            if(which==2)new AlertDialog.Builder(this).setTitle("数据只留在本机").setMessage("无账号、无联网权限、无广告和统计。只处理系统提供的可见通知；不读取历史短信、聊天数据库，不使用无障碍。\n\n取件码、地点、来源、时间、有限原文及状态存于应用私有数据库。手机号和明确验证码会遮盖。删除单条后保留摘要用于防止重复导入；清空全部记录会同时删除这些摘要，并忽略清空之前的通知回放。系统备份已关闭。卸载也会移除数据。\n\n赞赏码图片保存在应用内。首次清空后的赞赏提示记录仅保存在本机，清空快递不会重置；卸载或清除应用数据后重置。应用不验证付款、不保存付款信息。\n\n仅在你点击粘贴或主动粘贴时读取剪贴板。可在系统设置撤销通知使用权，手动功能仍可用。").setPositiveButton("知道了",null).show();
            if(which==3)new AlertDialog.Builder(this).setTitle("清空全部记录？").setMessage("待取、已取和去重记录均会删除，无法恢复。清空之前的通知不会重新导入。").setNegativeButton("取消",null).setPositiveButton("全部清空",(a,b)->clearAllRecords()).show();
        }).show();
    }
}
