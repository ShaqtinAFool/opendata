import app.db.DBSettingEnum;
import app.filetree.FV;
import app.filetree.FVEnum;
import app.station.StationData;

/**
 * 歷史氣象資料
 * @author tony
 */
public class NF_HistoryData {
    public static void main(String[] args) {
        // 開起解析方法
        StationData r = new StationData(DBSettingEnum.byStation);  
        // 啟動各功能
        FV fv = new FV(FVEnum.station);// 走訪目錄
        for (Object url : fv.getPath()) {
            String urlToString = url.toString();
            if(urlToString.contains("CAA")){
                System.out.println(urlToString);
//                r.parseCWB(urlToString);
                r.parseCAA(urlToString);
            }

        }
    }
}
