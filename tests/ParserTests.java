import cn.shunshouqu.app.PickupParser;
import java.util.*;
public class ParserTests {
    static int count=0;
    static void test(String s,String station,String...codes){var r=PickupParser.parse(s);if(r.size()!=codes.length)throw new AssertionError("case "+(count+1)+": expected "+codes.length+", got "+r);for(int i=0;i<codes.length;i++){if(!r.get(i).code().equals(codes[i]))throw new AssertionError("Code mismatch "+r);if(station!=null&&!r.get(i).station().equals(station))throw new AssertionError("Station mismatch "+r);}count++;}
    public static void main(String[] args){
        test("【菜鸟驿站】您的包裹已到东门菜鸟驿站，取件码3-2-190，请及时领取。","东门菜鸟驿站","3-2-190");
        test("【丰巢】您的快件已存入北门丰巢柜，取件码：008321。","北门丰巢柜","008321");
        test("取货码A-08-006，包裹已到南门快递服务站。","南门快递服务站","A-08-006");
        test("您有2件包裹到东门菜鸟驿站，取件码：3-2-190、6-1-025。","东门菜鸟驿站","3-2-190","6-1-025");
        test("【购物平台】验证码832745，五分钟内有效。",null);
        test("您的订单已发货，运单号123456789012，客服电话95543。",null);
        test("包裹已到东门菜鸟驿站，请打开应用查看取件码。","东门菜鸟驿站","");
        test("",null);test(null,null);test(" \n\t",null);
        test("取件码：００８３２１，包裹已存入北门丰巢柜。","北门丰巢柜","008321");
        test("取件码：A—08–006，包裹已到南门快递服务站。","南门快递服务站","A-08-006");
        test("包裹已到东门菜鸟驿站，\n取件码: 3-2-190","东门菜鸟驿站","3-2-190");
        test("包裹已到东门菜鸟驿站，取件码3-2-190，取件码3-2-190","东门菜鸟驿站","3-2-190");
        test("领取红包！输入取件码123456赢优惠",null);
        test("取件码13812345678，订单123456789012",null);
        test("取件码832745",PickupParser.UNKNOWN,"832745");
        test("包裹已到东门菜鸟驿站，取件码3-2-190。\n包裹已存入北门丰巢柜，取件码008321。",null,"3-2-190","008321");
        test("包裹已到东门菜鸟驿站，取件码3-2-190、3-2-190。","东门菜鸟驿站","3-2-190");
        test("无关文字".repeat(15000),null);
        test("验证码123456，取件码3-2-190",PickupParser.UNKNOWN,"3-2-190");
        test("包裹已到东门菜鸟驿站，取件码008321\n包裹已存入北门丰巢柜，取件码008321",null,"008321","008321");
        var p=PickupParser.parse("包裹已到东门菜鸟驿站，取件码3-2-190，手机号13812345678，验证码998877");if(p.get(0).excerpt().contains("13812345678")||p.get(0).excerpt().contains("998877"))throw new AssertionError("Privacy");count++;
        test("待取件\n您有一个包裹，请打开京东查看详情",PickupParser.UNKNOWN,"");
        test("淘宝\n您有快递待取件，请及时领取",PickupParser.UNKNOWN,"");
        test("拼多多\n您的订单待自提，打开应用查看取货信息",PickupParser.UNKNOWN,"");
        test("包裹已到驿站，请打开应用查看",PickupParser.UNKNOWN,"");
        test("您的快递已存入快递柜，码在应用中查看",PickupParser.UNKNOWN,"");
        test("您有2个包裹待取，点击查看",PickupParser.UNKNOWN,"");
        test("您的快件等待您领取",PickupParser.UNKNOWN,"");
        test("包裹已到东门菜鸟驿站。取件码008321。","东门菜鸟驿站","008321");
        test("待取件\n包裹已到东门菜鸟驿站，取件码008321。","东门菜鸟驿站","008321");
        test("待取件。包裹已到东门菜鸟驿站，取件码008321。","东门菜鸟驿站","008321");
        test("包裹已到东门菜鸟驿站，取件码****，请打开应用查看。","东门菜鸟驿站","");
        test("包裹已到分拨中心，下一站将派送",null);
        test("您的快递已送达",null);
        test("物流更新，点击查看",null);
        test("您的包裹正在派送，请注意查收",null);
        test("退货进度：快递员待取件，请准备好包裹",null);
        test("预约上门取件成功，快递员即将到达",null);
        test("您的包裹已领取，取件成功",null);
        test("包裹已到东门菜鸟驿站，现已签收，无需取件",null);
        test("暂无待取件",null);
        test("待取件：0件",null);
        test("领取红包，点击领券，待取件",null);
        test("订单已发货，待商家交给快递员",null);
        test("您的包裹预计明天送达东门菜鸟驿站",null);
        test("没有包裹待取件",null);
        test("暂无快递待取件",null);
        System.out.println("PASS: "+count+" synthetic parser cases");
    }
}
