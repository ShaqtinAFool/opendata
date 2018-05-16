import app.db.DBSettingEnum;
import app.filetree.FV;
import app.filetree.FVEnum;
import app.station.StationData;
import osi.presentation.SSL;

/**
 * 十分鐘雨量
 */
public class NF_RainData {
    public static void main(String[] args) {
        // 開起解析方法
        StationData r = new StationData(DBSettingEnum.by10Rain);  
        // 啟動各功能
        FV fv = new FV(FVEnum.station);// 走訪目錄
        // 解決無法 SSL 連線問題
        SSL ps = new SSL();
        ps.enableSSLSocket();
        // 解析讀進來的資料
        r.parseOpendataRain();
    }
}


