//<editor-fold defaultstate="collapsed" desc="import...">
import app.db.DBSettingEnum;
import app.filesize.FileInfomation;
import app.filetree.FV;
import app.filetree.FVEnum;
import app.typhoon.DownloadTypeEnum;
import app.typhoon.TyphoonList;
import app.typhoon.TyphoonData;
import weather.Adjust;
//</editor-fold>

/**
 * 颱風資料
 */
public class NF_TyphoonList {

    public static void main(String[] args) {
        // 開啟時間
        Adjust adj = new Adjust("yyyyMMdd");
        String nowTime, theDayBefore;
        nowTime = adj.getNowTime();
        adj.inputValue(nowTime);
        theDayBefore = adj.adjustDay(-1);         
        // 開起解析方法
        TyphoonData td = new TyphoonData(DBSettingEnum.byTyphoon);
        // 啟動各功能
        FV fv = new FV(FVEnum.typhoon);// 走訪目錄
        TyphoonList tl = new TyphoonList();// 下載颱風清單
        FileInfomation fi = new FileInfomation();// 判斷檔案資訊
        /****************** 以下開始執行 ******************/
        // 更新颱風名單
        if(td.setRunOrNotRun("typhoon_list_from_cwb_web"))tl.getCWBTyListToHtml();
        if(td.setRunOrNotRun("tigge_parse_realtime")){
            // 下載: 即時 or 歷史
            td.downloadTigge(15000, DownloadTypeEnum.byRealtime);
//            td.downloadTigge(15000, DownloadTypeEnum.byHistory);            
        }
        for (Object url : fv.getPath()) {
            String urlToString = url.toString();
            int lev = fi.getDirectoryLevel(urlToString);
            String dataType = fi.getDirectoryName(urlToString, lev);
            switch (dataType) {
                case "tigge":
                    // 檔案大小為 0，直接跳過
//                    if(urlToString.contains("NCEP"))
                    if(fi.getFileSizeByBytes(urlToString) > 8192) {
                        if(td.setRunOrNotRun("tigge")){    
                            if(td.setRunOrNotRun("tigge_parse_realtime") && urlToString.contains(theDayBefore)){  
                                // 即時且前一日資料
                                td.parseTigge(urlToString);
                                // 刪除 tigge temp 資料夾
                                td.deleteTempDirectory(urlToString);                                  
                            }else if(td.setRunOrNotRun("tigge_parse_history") && !td.setRunOrNotRun("tigge_parse_realtime")){
                                // 歷史資料
                                td.parseTigge(urlToString);
                            }
                        }
                    }
                    break;
                case "cwb_track":
                    if(td.setRunOrNotRun("cwb_track"))td.parseCWBTrack(urlToString);
                    break;
                case "typhoon2000":
                    if(td.setRunOrNotRun("typhoon2000"))td.parseTy2000();
                    break;
                case "jtwc":
                    if(td.setRunOrNotRun("jtwc"))td.parseJTWC(urlToString);
                    break;
                case "cwb_weps":
                    if(td.setRunOrNotRun("cwb_weps"))td.parseCWBWEPS(urlToString);
                    break;
                default:
                    break;
            }
        }

    }
}
