//<editor-fold defaultstate="collapsed" desc="import...">
import app.db.DBSettingEnum;
import app.filesize.FileInfomation;
import app.filetree.FV;
import app.typhoon.DownloadTypeEnum;
import app.typhoon.TyphoonList;
import app.typhoon.TyphoonData;
//</editor-fold>

/**
 * 颱風資料
 */
public class NF_TyphoonList {

    public static void main(String[] args) {
        // 開起解析方法
        TyphoonData td = new TyphoonData(DBSettingEnum.byTyphoon);
        // 啟動各功能
        FV fv = new FV();// 走訪目錄
        TyphoonList tl = new TyphoonList();// 下載颱風清單
        FileInfomation fi = new FileInfomation();// 判斷檔案資訊
        /****************** 以下開始執行 ******************/
        // 更新颱風名單
//        tl.getCWBTyListToHtml();
        // 下載: 即時 or 歷史
//        System.out.println("下載");
//        td.getTigge(15000, DownloadTypeEnum.byRealtime);
//        td.getTigge(15000, DownloadTypeEnum.byHistory);
        for (Object url : fv.getTyphoonPath()) {
            // 檔案大小為 0，直接跳過
            if(fi.getFileSizeByByte((String) url) <= 10)continue;
            // 解析資料，先看 2016 年
//            if(url.toString().contains("UKMO")){
//            if(url.toString().contains("2015")){
            String dataType = fi.getDirectory((String) url, 2);
            switch (dataType) {
                case "tigge":
                    td.parseTigge((String) url);
                    break;
                case "cwb_track":
                    td.parseCWBTrack((String) url);
                    break;
                default:
                    break;
            }
            // 建立颱風、單位清單
//            td.setNameAndCentre();
            // 建立 info
//            td.setXXXInfo();
            // 匯入 content
//            td.setXXXContent();  
        }
    }
}
