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
        r.parseCODis();
        // 啟動各功能
//        FV fv = new FV(FVEnum.station);// 走訪目錄
//        for (Object url : fv.getPath()) {
//            String urlToString = url.toString();
//            if(urlToString.contains(".xlsx")){
//                continue;
//            }else if(urlToString.contains("_cwb_dy.txt")){
//                continue;
//            }else if(urlToString.contains("_cwb_m.txt")){
//                continue;
//            }else if(urlToString.contains("_upair.txt")){
//                continue;
//            }
//            r.parseCWB(urlToString);
////            r.parseCAA(urlToString);
//        }
    }
}
