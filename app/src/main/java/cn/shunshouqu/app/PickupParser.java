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
    public static List<Candidate> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        String s = Normalizer.normalize(raw.substring(0, Math.min(raw.length(), 12000)), Normalizer.Form.NFKC)
                .replaceAll("[‐‑‒–—−－]", "-");
        if (!s.matches("(?s).*(取件|取货|提货|领取|包裹|快件).*")) return Collections.emptyList();
        if (s.matches("(?s).*(优惠|促销|抽奖|领取红包|领红包|点击领券).*") && !s.matches("(?s).*(包裹|快件).*(已到|存入|送达).*")) return Collections.emptyList();
        LinkedHashMap<String,Candidate> out = new LinkedHashMap<>();
        // Separate complete notifications, but keep wrapped lines within one notification together.
        String[] blocks = s.split("(?<=[。；;！!])\\s*|\\n\\s*\\n|\\n(?=【|(?:您的|你的|包裹|快件|您有)[^\\n]*(?:取件码|提货码|取货码))");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            String station = location(block);
            if (station.equals(UNKNOWN) && blocks.length == 1) station = location(s);
            Matcher m = CODES.matcher(block);
            while (m.find()) {
                for (String code : m.group(1).split("\\s*[、,，/或]\\s*")) {
                    if (!validCode(code)) continue;
                    Candidate c = new Candidate(code, station, carrier(block), excerpt(block));
                    out.putIfAbsent(station+"|"+code.toUpperCase(Locale.ROOT), c);
                }
            }
            if (!m.reset().find() && block.matches("(?s).*(包裹|快件).*(已到|存入|送达).*")) {
                out.putIfAbsent(station+"|", new Candidate("",station,carrier(block),excerpt(block)));
            }
        }
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
