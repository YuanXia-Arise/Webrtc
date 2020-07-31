package com.dds.webrtclib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class AppUtil {

    //public String url = "http://192.168.3.144:5000";
    public String url = "http://vvv.fupdate.cc:5000";

    public String PATH = "/storage/emulated/0/Download/backup/";

    public String[] data = {"com.freevpnintouch: Betternet", "io.hideme.android:Hideme-Vpn",
            "com.autually.advpn:EasyVpn", "com.autually.vipvpn:Speed Vpn", "com.yongdaoyun.vpn:极速Vpn",
            "com.speed.faster:极速Vpn", "com.fvcorp.flyclient:flyVpn", "com.goldenfrog.vyprvpn.app:Vypr Vpn",
            "github.kr328.clash:clash", "com.v2ray.ang:v2ray", "com.expressvpn.vpn:ExpressVPN",
            "com.vpnexpress.turbo.vpn:Express Vpn", "free.express.vpn:free.express.vpn",
            "com.nvt.expressvpn:Express VPN", "com.nordvpn.android: NordVPN",
            "com.pvpn.privatevpn:PrivateVPN", "free.vpn.private:VPN Private",
            "com.vpn.privatevpn: PrivateVPN", "com.privateinternetaccess.android: VPN by Private Internet Access",
            "com.findtheway:老王VPN", "com.surfshark.vpnclient.android:Surfshark",
            "com.astrill.astrillvpn: com.astrill.astrillvpn",
            "hotspotshield.android.vpn: 极速&免费翻墙神器",
            "hssb.android.free.app:免费 VPN – Hotspot Shield 基本版",
            "free.vpn.shield.hotspot: Hotspot Free VPN Shield",
            "com.supervpn.freevpn:Hotspot VPN", "free.vpn.unblock.proxy.securevpn:Secure VPN",
            "com.megafreeapps.freevpn.proxy.secure.shield.fasthotspot: Free VPN Proxy",
            "mam.free.vpn.proxy: Super VPN Hotspot Unblock Proxy Master Speed VPN",
            "com.windscribe.vpn: Windscribe VPN", "ch.protonvpn.android: ProtonVPN",
            "com.tunnelbear.android:TunnelBear VPN", "com.gaditek.purevpnics:PureVPN",
            "free.androidtv.vpn.proxy.purevpn: PureVPN", "com.arsoftltd.arpurevpn:AR Pure VPN",
            "com.galaxylab.ss:银河VPN", "com.google.android.gm:Gmail", "com.dropbox.android:Dropbox",
            "com.google.android.googlequicksearchbox:Google",
            "com.google.android.apps.enterprise.dmagent:Google Apps Device Policy",
            "com.google.android.apps.googleassistant:Google 助理",
            "com.microsoft.skydrive:Microsoft OneDrive", "com.Slack:Slack",
            "com.android.vending: Google Play", "com.hootsuite.droid.full:Hootsuite",
            "com.facebook.katana: facebook", "com.facebook.lite: fackbook-lite",
            "com.instagram.android:instagram", "com.twitter.android:twitter", "com.snapchat.android:Snapchat",
            "com.pinterest:Pinterest", "com.quora.android:Quora", "com.tumblr:Tumblr" ,
            "com.reddit.frontpage: Reddit", "com.google.android.youtube:Youtube",
            "com.dailymotion.dailymotion:DailyMotion", "com.vimeo.android.videoapp:Vimeo",
            "tv.twitch.android.app:Twitch", "tv.periscope.android:Periscope", "com.pandora.jewelry.store.app:Pandora",
            "com.spotify.music:Spotify", "mp3.player.freemusic:Free Music for SoundCloud",
            "com.nytimes.android:The New York Times", "bbc.mobile.news.ww:BBC News",
            "com.ft.news:Financial Times", "com.ft.ftchinese:FT中文网", "wsj.reader_sp:The Wall Street Journal",
            "com.thomsonreuters.reuters:路透 新闻", "com.cnn.mobile.android.phone:CNN Breaking US,",
            "com.yahoo.mobile.client.android.search:Yahoo搜尋", "com.yahoo.infohub:Yahoo 新聞",
            "com.duckduckgo.mobile.android: DuckDuckGo", "com.whatsapp: WhatsApp",
            "org.telegram.messenger: telegram", "com.facebook.orca:Facebook Messenger",
            "jp.naver.line.android:LINE", "org.thoughtcrime.securesms:Signal",
            "com.kakao.talk:KaKao Talk", "com.medium.reader:Medium",
            "com.google.android.apps.blogger:Blogspot", "org.wordpress.android:WordPress.com",
            "com.skype.raider:Skype", "com.google.android.talk: 环聊,", "app.getvibe: Vibe",
            "me.xhss.xiaoha:小哈VPN",
            "com.Thunder.vpn.fast.unlimited.vpn.proxy:Thunder VPN",
            "com.thundervpn.free.proxy:Thunder VPN Lite",
            "com.Vpn.highspeed:Thunder VPN",
            "com.lemonapp.super_snap_vpn_pro:	Super Snap VPN-Unlimited Free Super Fast VPN Proxy",
            "free.vpn.unblock.proxy.securevpn:Secure VPN",
            "com.free.vpn.super.hotspot.open:VPN Super",
            "free.vpn.unblock.proxy.turbovpn:Turbo VPN",
            "free.vpn.unblock.proxy.turbovpn.lite: Turbo VPN Lite",
            "free.vpn.unblock.proxy.turbovpnUSA:Turbo VPN - FREE",
            "free.unblock.prh.turbo.vpn:Turbo VPN - USA",
            "me.turbovpn.vpn:Turbo VPN Pro"};

    public String[] Data = {"Betternet", "Hideme-Vpn", "EasyVpn", "Speed Vpn", "极速Vpn", "极速Vpn", "flyVpn",
            "Vypr Vpn", "clash", "v2ray", "ExpressVPN", "Express Vpn", "free.express.vpn", "Express VPN",
            "NordVPN", "PrivateVPN", "VPN Private", "PrivateVPN", "VPN by Private Internet Access",
            "老王VPN", "Surfshark", "com.astrill.astrillvpn", "极速&免费翻墙神器", "免费 VPN – Hotspot Shield 基本版",
            "Hotspot Free VPN Shield", "Hotspot VPN", "Secure VPN", "Free VPN Proxy" ,
            "Super VPN Hotspot Unblock Proxy Master Speed VPN" , "Windscribe VPN" , "ProtonVPN" ,
            "TunnelBear VPN" , "PureVPN" , "PureVPN" , "AR Pure VPN" , "银河VPN" , "Gmail" ,
            "Dropbox" , "Google" , "Google Apps Device Policy" , "Google 助理" , "Microsoft OneDrive" ,
            "Slack" , "Google Play" , "Hootsuite" , "facebook" , "fackbook-lite" , "instagram" ,
            "twitter" , "Snapchat" , "Pinterest" , "Quora" , "Tumblr" , "Reddit" , "Youtube" ,
            "DailyMotion" , "Vimeo" , "Twitch" , "Periscope" , "Pandora" , "Spotify" , "Free Music for SoundCloud" ,
            "The New York Times" , "BBC News" , "Financial Times" , "FT中文网" , "The Wall Street Journal" ,
            "路透 新闻" , "CNN Breaking US," , "Yahoo搜尋" , "Yahoo 新聞" , "DuckDuckGo" , "WhatsApp" ,
            "telegram" , "Facebook Messenger" , "LINE" , "Signal" , "KaKao Talk" , "Medium" ,
            "Blogspot" , "WordPress.com" , "Skype" , "环聊" , "Vibe"};


    public void saveAsFileWriter(String content, String filename) {
        FileWriter fwriter = null;
        String strContent = content + "\r\n";
        try {
            if (!new File(PATH).exists()){
                new File(PATH).mkdirs();
            }
            File file = new File(PATH + filename);
            if (!file.exists()) {
                file.createNewFile();
                //Runtime.getRuntime().exec("chmod 777 " +  file );
            }
            fwriter = new FileWriter(file, true);
            fwriter.write(strContent);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public String readfile(String filePath) {
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder = new StringBuilder();
        File file = new File(filePath);
        if (file.exists()) {
            try {
                fileInputStream = new FileInputStream(file);
                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + " ");
                }
                bufferedReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return stringBuilder.toString();
    }

}
