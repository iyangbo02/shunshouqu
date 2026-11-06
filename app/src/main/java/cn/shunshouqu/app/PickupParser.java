package cn.shunshouqu.app;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Pure Java, conservative rules. All fixtures are synthetic. */
public final class PickupParser {
    public static final String UNKNOWN = "地点待补充";
    public record Candidate(String code, String station, String carrier, String excerpt) {}
    private static final String TOKEN = "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*";
    private static final Pattern CODES = Pattern.compile("(?:取件码|提货码|取货码)\\s*(?:为|是)?\\s*[:：]?\\s*("+TOKEN+"(?:\\s*[、,，/或]\\s*"+TOKEN+")*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION = Pattern.compile("(?:已到达?|已存入|已放入|送达|存放于|包裹到|快件到|请到|请至|到达|位于|存入|在)\\s*([^，。；;！!\\n：:]{2,48}?(?:菜鸟驿站|快递服务站|快递驿站|快递站|驿站|丰巢柜|快递柜|自提柜|自提点|妈妈驿站|服务站))");
    private static final Pattern NOT_PICKUP = Pattern.compile("上门取件|预约取件|寄件|退货|退件|揽收|已取件|已领取|取件成功|已签收|无需取件|(?:没有|暂无|无|取消)待取|待取件\\s*[:：]?\\s*0(?:件|个|\\b)");
    private static final Pattern WAITING = Pattern.compile("待取件|待取货|待自提|等待(?:您|你)?(?:取件|取货|自提|领取)|请(?:您|你)?(?:及时|尽快)?(?:取件|取货|自提|领取包裹)|(?:前往|到).{0,48}(?:驿站|快递柜|自提点).{0,8}(?:取件|取货|领取)");
    private static boolean pickupReminder(String s,String station) {
        if (NOT_PICKUP.matcher(s).find()) return false;
        if (s.matches("(?s).*(?:预计|即将|将于|尚未|还没).{0,12}(?:到达|到站|送达|存入).*")) return false;
        if (s.matches("(?s).*(?:没有|暂无|无)(?:包裹|快件|快递)?(?:需要|待)?(?:取件|取货|自提|领取).*")) return false;
        boolean parcel=s.matches("(?s).*(包裹|快件|快递|物流|订单).*");
        boolean arrived=s.matches("(?s).*(包裹|快件|快递).{0,32}(已到|已存入|已放入|已送达|送达).*");
        boolean waiting=WAITING.matcher(s).find()||s.matches("(?s).*(包裹|快件|快递).{0,16}(待取|待领取|需要(?:您|你)?(?:领取|取件)).*");
        boolean pickupPlace=!station.equals(UNKNOWN)||s.matches("(?s).*(包裹|快件|快递).{0,32}(已到|已存入|已放入|已送达|送达).{0,24}(驿站|快递柜|自提点|自提柜).*");
        return (waiting && (parcel || s.matches("(?s).*待(?:取件|取货|自提).*"))) || (arrived && pickupPlace);
    }
    public static List<Candidate> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        String s = Normalizer.normalize(raw.substring(0, Math.min(raw.length(), 12000)), Normalizer.Form.NFKC)
                .replaceAll("[‐‑‒–—−－]", "-");
        if (!s.matches("(?s).*(取件|取货|提货|自提|领取|包裹|快件|快递).*")) return Collections.emptyList();
        if (s.matches("(?s).*(优惠|促销|抽奖|领取红包|领红包|点击领券).*") && !s.matches("(?s).*(包裹|快件).*(已到|存入|送达).*")) return Collections.emptyList();
        LinkedHashMap<String,Candidate> out = new LinkedHashMap<>();
        Set<String> places=new LinkedHashSet<>();Matcher locationMatches=LOCATION.matcher(s);
        while(locationMatches.find())places.add(locationMatches.group(1).trim().replaceFirst("^(您的|你的)",""));
        // Separate complete notifications, but keep wrapped lines within one notification together.
        String[] blocks = s.split("(?<=[。；;！!])\\s*|\\n\\s*\\n|\\n(?=【|(?:您的|你的|包裹|快件|您有)[^\\n]*(?:取件码|提货码|取货码))");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            String station = location(block);
            if (station.equals(UNKNOWN) && places.size()==1) station=places.iterator().next();
            Matcher m = CODES.matcher(block);
            boolean foundCode=false;
            while (m.find()) {
                for (String code : m.group(1).split("\\s*[、,，/或]\\s*")) {
                    if (!validCode(code)) continue;
                    foundCode=true;
                    Candidate c = new Candidate(code, station, carrier(block), excerpt(block));
                    out.putIfAbsent(station+"|"+code.toUpperCase(Locale.ROOT), c);
                }
            }
            if (!foundCode && pickupReminder(block,station)) {
                out.putIfAbsent(station+"|", new Candidate("",station,carrier(block),excerpt(block)));
            }
        }
        // A generic title/arrival sentence must not add a second reminder to a coded notification.
        Set<String> codedStations=new HashSet<>();
        for(Candidate c:out.values())if(!c.code().isEmpty())codedStations.add(c.station());
        if(!codedStations.isEmpty())out.values().removeIf(c->c.code().isEmpty()
                && (c.station().equals(UNKNOWN)||codedStations.contains(c.station())));
        return new ArrayList<>(out.values());
    }
    public static boolean validCode(String code) {
        return code.matches("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*") && code.length() >= 3 && code.length() <= 24
                && code.matches(".*[0-9].*") && !code.matches("[0-9]{9,}");
    }
    static String location(String s) {
        Matcher m = LOCATION.matcher(s);
        return m.find() ? m.group(1).trim().replaceFirst("^(您的|你的)", "") : UNKNOWN;
    }
    static String carrier(String s) { for (String c : new String[]{"菜鸟","丰巢","中通","圆通","申通","韵达","顺丰","京东","极兔","邮政"}) if(s.contains(c)) return c; return ""; }
    static String excerpt(String s) { return s.replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)","[手机号已隐藏]").replaceAll("验证码\\s*[:：]?\\s*[A-Za-z0-9-]+", "验证码[已隐藏]").substring(0, Math.min(600,s.replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)","[手机号已隐藏]").replaceAll("验证码\\s*[:：]?\\s*[A-Za-z0-9-]+", "验证码[已隐藏]").length())); }
}
