import app.rain.RainData;
import osi.presentation.SSL;

/**
 * 十分鐘雨量
 */
public class NF_RainData {
    public static void main(String[] args) {
        // 開起解析方法
        RainData r = new RainData();        
        // 解決無法 SSL 連線問題
        SSL ps = new SSL();
        ps.enableSSLSocket();
        // 將測站地址放到 List 裡面
        r.setStnAddress();
        // 解析讀進來的資料
        r.parseData();
    }
}


