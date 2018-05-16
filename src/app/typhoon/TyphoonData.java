package app.typhoon;

//<editor-fold defaultstate="collapsed" desc="import...">
import app.db.DBSetting;
import app.filetree.WalkFileTree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weather.Adjust;
//</editor-fold>

/**
 * 解析颱風資料
 * Tigge、CWB Best Track 
 */
public class TyphoonData extends DBSetting {
    
    private Properties prop;
    private Connection conn;
    private int setYear;
    private String db_type, whereToDownload, whereToDownload_temp, tiggeURL, ty2000URL, regex;
    private Adjust adjust;
    private TyphoonList ty_list;
    private WalkFileTree w_file;
    private Path abs_path;
    private PreparedStatement prepStmt;
    private ResultSet rs;
    private HashSet<String> hs_tyInfo;
    private HashSet<String> hs_cetreInfo;
    private HashSet<String> hs_anlyInfo, hs_foreInfo, hs_ensbInfo, hs_bstkInfo;
    private ArrayList<String> al_rawdata;
    
    /**
     * 
     * @param dbEnum
     */
    public TyphoonData(Enum dbEnum) {//<editor-fold defaultstate="collapsed" desc="...">
        // 必寫
        super(dbEnum);
        // 設定 property
        setProperty();
        // 分格符號
        regex = ",";
        // 資料庫
        db_type = getReturnValue("dbType");
        conn = getConn();
        hs_tyInfo = new HashSet<>();
        hs_cetreInfo = new HashSet<>();
        hs_anlyInfo = new HashSet<>();
        hs_foreInfo = new HashSet<>();
        hs_ensbInfo = new HashSet<>();
        hs_bstkInfo = new HashSet<>();
        // 原始資料
        al_rawdata = new ArrayList<>();
        // 其他設定
        adjust = new Adjust("yyyy-MM-dd HH:mm:ss");// 時間格式
        ty_list = new TyphoonList();
        w_file = new WalkFileTree();// 走訪目錄
    }//</editor-fold>
    
    /**
     * 設定 property
     */
    private void setProperty() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            prop = new Properties();
            prop.load(new FileReader(tywebProp));
            whereToDownload = prop.getProperty("download_dir_path");
            whereToDownload_temp = prop.getProperty("download_dir_path_temp");
            tiggeURL = prop.getProperty("tigge_web_url");
            ty2000URL = prop.getProperty("ty2000_url");
            setYear = Integer.parseInt(prop.get("ty2000_year").toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }     
    }//</editor-fold>
    
    /**
     * 下載 xml
     * @param xmlFile xml 檔名
     * @param xmlURL xml URL
     * @param createDirPath 檔案下載後的位置
     */
    public void downloadData(String xmlFile, String xmlURL, String createDirPath) {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            URL wantToDownloadURL = new URL(xmlURL);
            // 建立資料夾，不知道有沒有強過老方法 createDirectory.mkdirs();
            File makeDownloadDir = new File(createDirPath);
            FileUtils.forceMkdir(makeDownloadDir);
            File createFile = new File(createDirPath + xmlFile);
            // 下載網頁上檔案
            FileUtils.copyURLToFile(wantToDownloadURL, createFile);        
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // 測試看看....
            abs_path = null;
        }
    }//</editor-fold>
        
    /**
     * 下載 tigge XML (目前隱性建議，用 shell 下載或許較好)
     * @param timeout 緩衝時間
     * @param dt_enum 下載方式
     */
    public void downloadTigge(int timeout, DownloadTypeEnum dt_enum) {//<editor-fold defaultstate="collapsed" desc="...">
        String[] centre = {"CMA", "ECMWF", "JMA", "KMA", "MSC", "NCEP", /*"STI",*/ "UKMO"};
        try {
            // 以下區域是設定變數
            Adjust adj = new Adjust("yyyyMMdd");
            String yymmdd, createDirPath, nowTime, theDayBefore;
            Document doc_lev1, doc_lev2;
            // 以上區域是設定變數
            for (int i = 0; i < centre.length; i++) {
                // tiggeURL = http://tparc.mri-jma.go.jp/cxmldata/cxml/ECMWF
                // 解析網站資料
                doc_lev1 = Jsoup.connect(tiggeURL + "/" + centre[i]).timeout(timeout).get();
                Elements elms_a_lev1 = doc_lev1.select("a");
                int rows = 0;
                for (Element elm_a_lev1 : elms_a_lev1) {
                    if(rows >= 5) {
//                        System.out.println(elm_a_lev1.text());
                        // 建資料夾用
                        yymmdd = elm_a_lev1.text().replaceAll("/", "");
                        nowTime = adj.getNowTime();
                        adj.inputValue(nowTime);
                        theDayBefore = adj.adjustDay(-1);
                        if(dt_enum.equals(DownloadTypeEnum.byRealtime)){
                            // 下載即時資料
                            if(yymmdd.equals(theDayBefore)) {
                                // 取特定日期
                                doc_lev2 = Jsoup.connect(elm_a_lev1.absUrl("href")).timeout(timeout).get();
                                Elements elms_a_lev2 = doc_lev2.select("a");
                                for (Element elm_a_lev2 : elms_a_lev2) {
                                    String xmlURL = elm_a_lev2.absUrl("href");
                                    if(xmlURL.contains(".xml")){
                                        // 取出檔案名稱
                                        String xmlFile = elm_a_lev2.text();
                                        createDirPath = String.format("%s/tigge/%s/%s/", whereToDownload_temp, centre[i], yymmdd);
                                        downloadData(xmlFile, xmlURL, createDirPath);
                                    }
                                }
                            }
                        }else if(dt_enum.equals(DownloadTypeEnum.byHistory)){
                            // 下載歷史資料
                            doc_lev2 = Jsoup.connect(elm_a_lev1.absUrl("href")).timeout(timeout).get();
                            Elements elms_a_lev2 = doc_lev2.select("a");
                            for (Element elm_a_lev2 : elms_a_lev2) {
                                String xmlURL = elm_a_lev2.absUrl("href");
                                if(xmlURL.contains(".xml")){
                                    // 取出檔案名稱
                                    String xmlFile = elm_a_lev2.text();
                                    createDirPath = String.format("%s/tigge/%s/%s/", whereToDownload_temp, centre[i], yymmdd);
                                    downloadData(xmlFile, xmlURL, createDirPath);
                                }
                            }                         
                        }
                    }
                    rows++;
                }   
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            
        }
    }//</editor-fold>    

    /**
     * 搬移 xml
     * @param sourceURL
     * @param destinationURL 
     */
    public void moveFile(String sourceURL, String destinationURL) {//<editor-fold defaultstate="collapsed" desc="...">
//        System.out.println(sourceURL + " :::> " + destinationURL);
        File sourceFile = new File(sourceURL);
        File destinationFile = new File(destinationURL);
        Path destinationPath = Paths.get(destinationURL);
        File destinationDirectory = new File(destinationPath.getParent().toString());
        if(!destinationDirectory.isDirectory()){
            destinationDirectory.mkdirs();
        }        
        sourceFile.renameTo(destinationFile);
    }//</editor-fold>
    
    /**
     * 刪除 tigge 資料夾 by 遞回
     * @param url
     */
    public void deleteTempDirectory(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        File destinationPath = new File(url);
        // 目標是只剩下 .\temp\tigge\UKMO
        File destinationDirectory = destinationPath.getParentFile();
        if(!destinationPath.exists()){
//            System.out.println(url + " 不存在就不動作");
//            System.out.println("刪除 " + destinationDirectory.getPath() + " 並 return");
            destinationDirectory.delete();
            return;
        }
        if(destinationPath.isFile()){
//            System.out.println(url + " 是檔案就刪除");
            destinationPath.delete();
            return;
        }
        File[] files = destinationPath.listFiles();
        for(int i = 0; i < files.length; i++) {
            System.out.println(url + " 遞回刪除");
            deleteTempDirectory(url);
        }
        destinationPath.delete();
    }//</editor-fold>
    
    /**
     * 解析 tigge XML
     * @param url 檔案路徑
     */
    public void parseTigge(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        abs_path = Paths.get(url);
        String centre = abs_path.getName(abs_path.getNameCount() - 3).toString();
        String yymmdd = abs_path.getName(abs_path.getNameCount() - 2).toString();
        String fileName = abs_path.getFileName().toString();
        String dataSource = "tigge";        
        try {
            String typhoonReginal;
            switch (centre) {
                case "JMA":
                case "ECMWF":
                case "CMA":
                case "KMA":
                    typhoonReginal = "Northwest Pacific";
                    break;
                case "MSC":
                case "UKMO":
                case "NCEP":
                    typhoonReginal = "WP";
                    break;
                default:
                    typhoonReginal = "";
                    break;
            }
            Document doc = Jsoup.parse(new File(url), "UTF8");
            // 以下直接引用原始寫法好
            // 以下直接引用原始寫法好
            String product = doc.select("product").text();
            String raw_ProductionCenter = doc.select("productionCenter").text();
            // 專門處理 Met
            String productionCenter;
            if(raw_ProductionCenter.split("\\s+").length == 2){
                if("Met".equals(raw_ProductionCenter.split("\\s+")[0])){
                    productionCenter = "UKMO";
                }else{
                    productionCenter = raw_ProductionCenter.split("\\s+")[0];
                }
            }else{
                productionCenter = raw_ProductionCenter;
            }
            String applicationType = doc.select("generatingApplication").select("applicationType").text();
            String raw_BaseTime = doc.select("baseTime").text();
            String baseTime;
            // 排除詭異的時間格式
            if(raw_BaseTime.length() >= 20){
                adjust.inputValue(raw_BaseTime);
                baseTime = adjust.outputYMDH();
            }else if(raw_BaseTime.isEmpty()){
                if(doc.select("data").first().text().isEmpty()){
                    // 只有 UKMO 才會遇到的問題 : 沒有 baseTime ，原始碼 : <baseTime></baseTime>
                    baseTime = "";
                }else{
                    String raw_validTime = doc.select("validTime").first().text();
                    adjust.inputValue(raw_validTime);
                    baseTime = adjust.outputYMDH();
                }
            }else{
                baseTime = "";
            }
            // 曾經放錯位置過...
            String raw_modelResolution = doc.select("generatingApplication").select("model").select("modelResolution").text();
            String modelResolution;
            if(raw_modelResolution.contains(",")){
                modelResolution = raw_modelResolution.replaceAll(",", ";");
            }else{
                modelResolution = raw_modelResolution;
            }
            Elements elmsData = doc.select("data");
            for(Element elmData : elmsData){
                String type = elmData.attr("type");
                if(baseTime.isEmpty())break;
                if("analysis".equals(type)){
                    //<editor-fold defaultstate="collapsed" desc="分析場">
                    Elements elmsDisturbance = elmData.select("disturbance");
                    for(Element elmDisturbance : elmsDisturbance){
                        String typhoonName = elmDisturbance.select("cycloneName").text().trim().toUpperCase();
                        String typhoonNumber;
                        if(elmDisturbance.select("cycloneNumber").text().length() == 1){
                            typhoonNumber = "0" + elmDisturbance.select("cycloneNumber").text();
                        }else{
                            typhoonNumber = elmDisturbance.select("cycloneNumber").text();
                        }
                        // 分析場型態
                        String analysisType;
                        if("Cyclone Analysis".equalsIgnoreCase(product)){
                            analysisType = "observation";
                        }else if("Cyclone Forecast".equalsIgnoreCase(product)){
                            analysisType = "model analysis";
                        }else{
                            analysisType = "";
                        }
                        String typhoonLocalID = elmDisturbance.select("localID").text();
                        String typhoonBasin = elmDisturbance.select("basin").text();
                        Elements elmsFix = elmDisturbance.select("fix");
                        // (1) 開啟 "" : 防止沒有分區域 "".equals(typhoonBasin)
                        // (2) 註解 if : 防止有西太平洋颱風，但是分區是 northeast，先暫時不考慮到判斷式內
                        if(typhoonReginal.equals(typhoonBasin) || "".equals(typhoonBasin)){
                            for(Element elmFix : elmsFix){
                                String raw_ValidTime = elmFix.select("validTime").text();
                                String validTime_Year = raw_ValidTime.substring(0, 4);
                                String validTime;
                                if(raw_ValidTime.length() <= 19){
                                    break;
                                }else{
                                    adjust.inputValue(raw_ValidTime);
                                    validTime = adjust.outputYMDH();
                                }   
                                String lat = elmFix.select("latitude").first().text();
                                String latUnits = elmFix.select("latitude").first().attr("units");
                                String lon = elmFix.select("longitude").first().text();
                                String longUnits = elmFix.select("longitude").first().attr("units");                            
                                if( (longUnits.split("\\s").length <= 1) || (latUnits.split("\\s").length <= 1) ){
                                    break;
                                }else if( ("deg S".equals(latUnits)) || ("deg W".equals(longUnits)) ){
                                    break;
                                }
                                // 除錯專用
                                if(lat.contains("*") || lon.contains("*")){
                                    break;
                                }else{
                                    // 防止兩筆同樣資料 2009年 NCEP LINFA
                                    if(Double.parseDouble(lon) <= 95.0){
                                        break;
                                    }
                                }
                                String development = elmFix.select("cycloneData").select("development").text();
                                String pres = elmFix.select("cycloneData").select("minimumPressure").select("min_pressure").text();
                                String spd = elmFix.select("cycloneData").select("maximumWind").select("speed").text();
                                String spdUnits = elmFix.select("cycloneData").select("maximumWind").select("speed").attr("units").toLowerCase();
                                if(!typhoonName.isEmpty()){
                                    int getCWBTyphoonNumber = ty_list.getCWBTyNumber(typhoonName, Integer.parseInt(baseTime.substring(0, 4)));
                                    if(typhoonBasin.isEmpty())typhoonBasin = "";
                                    if(typhoonLocalID.isEmpty())typhoonLocalID = "";
                                    if(typhoonNumber.isEmpty())typhoonNumber = "";
                                    if(development.isEmpty())development = "";
                                    if(pres.isEmpty() || pres.contains("*"))pres = "-999";
                                    if(spd.equals("0") || spd.isEmpty() || spd.contains("*")){
                                        spd = "-999.0";
                                        spdUnits = "";
                                    }
                                    if(spdUnits.isEmpty())spdUnits = "";
                                    String import_tyInfo = String.format("%s,%s,%s",
                                        typhoonName, validTime_Year, getCWBTyphoonNumber);
                                    String import_cetrInfo = String.format("%s,%s,,-999,%s",
                                        productionCenter, applicationType, dataSource);
                                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                        typhoonName, productionCenter, applicationType, typhoonLocalID, typhoonBasin, getCWBTyphoonNumber,
                                        baseTime, validTime, lat, lon, pres, spd, spdUnits, development, 
                                        analysisType, dataSource, type);
                                    hs_tyInfo.add(import_tyInfo);
                                    hs_cetreInfo.add(import_cetrInfo);
                                    al_rawdata.add(import_rawdata);                                          
                                }
                            }
                        }
                    }
                    //</editor-fold>
                }else{
                    //<editor-fold defaultstate="collapsed" desc="模式場">
                    String member = elmData.attr("member");
                    Elements elmsDisturbance = elmData.select("disturbance");
                    for(Element elmDisturbance : elmsDisturbance){
                        String typhoonLocalID = elmDisturbance.select("localID").text();
                        String typhoonBasin = elmDisturbance.select("basin").text();
                        String typhoonName = elmDisturbance.select("cycloneName").text().toUpperCase();
                        int getCWBTyphoonNumber = 0;
                        String typhoonNumber;
                        if(typhoonReginal.equals(typhoonBasin) || "".equals(typhoonBasin)){
                            if("Invest".equalsIgnoreCase(typhoonName)){
                                if(elmDisturbance.select("cycloneNumber").text().length() == 1){
                                    typhoonNumber = "0" + elmDisturbance.select("cycloneNumber").text();
                                }else{
                                    typhoonNumber = elmDisturbance.select("cycloneNumber").text();
                                }
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0, 4) + typhoonNumber );
                            }else if(ty_list.isEnglishNumber(typhoonName)){
                                // 颱風名稱是英文數字轉為數字名稱 + W (one  --> 01W)
                                String raw_typhoonName = typhoonName;
                                // 英人數字轉成阿拉伯數字 + W
                                typhoonNumber = ty_list.numberChange(raw_typhoonName) + "";
                                if(typhoonNumber.length() == 1){
                                    typhoonNumber = "0" + typhoonNumber;
                                }  
                                typhoonName = typhoonNumber + "W";
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0, 4) + typhoonNumber );
                            }else if(typhoonName.matches("[0-9][0-9].*")){
                                typhoonNumber = typhoonName.substring(0, 2);
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0, 4) + typhoonNumber );
                            }else{
                                getCWBTyphoonNumber = ty_list.getCWBTyNumber(typhoonName, Integer.parseInt(baseTime.substring(0, 4)));
                            }
                            Elements elmsFix = elmDisturbance.select("fix");
//                            if(elmsFix.isEmpty())break;//開啟這個會讀不到該 ensembleForecast 資料
                            if(elmsFix.isEmpty())continue;
                            // 防止沒有分區域 "".equals(typhoonBasin)
                            for(Element elmFix : elmsFix){
                                String fcstHour = elmFix.attr("hour");
                                String raw_ValidTime = elmFix.select("validTime").text();
                                String validTime_Year = raw_ValidTime.substring(0, 4);
                                String validTime;
                                if(raw_ValidTime.length() <= 19){
                                    continue;
                                }else{
                                    adjust.inputValue(raw_ValidTime);
                                    validTime = adjust.outputYMDH();
                                }
                                String lat = elmFix.select("latitude").first().text();
                                String latUnits = elmFix.select("latitude").first().attr("units");
                                String lon = elmFix.select("longitude").first().text();
                                String lonUnits = elmFix.select("longitude").first().attr("units");
                                if( (lonUnits.split("\\s").length <= 1) || (latUnits.split("\\s").length <= 1) ){
                                    break;
                                }else if( ("deg S".equals(latUnits)) || ("deg W".equals(lonUnits)) ){
                                    break;
                                }
                                if(lat.contains("*") || lon.contains("*")){
                                    break;
                                }else{
                                    // 防止兩筆同樣資料 2009年 NCEP LINFA
                                    if(Double.parseDouble(lon) <= 95.0){
                                        break;
                                    }  
                                }
                                String pres = elmFix.select("cycloneData").select("minimumPressure").select("pressure").text();
                                if(pres.contains("**"))pres = "-999";
                                String raw_Speed = elmFix.select("cycloneData").select("maximumWind").select("speed").text();
                                String spd = raw_Speed;
                                String spdUnits = elmFix.select("cycloneData").select("maximumWind").select("speed").attr("units").toLowerCase();
                                String geopotentialHeight = "";
                                String import_tyInfo = null, import_cetrInfo = null, import_rawdata = null;
                                if(!typhoonName.isEmpty()){
                                    if(typhoonBasin.isEmpty())typhoonBasin = "";
                                    if(typhoonLocalID.isEmpty())typhoonLocalID = "";
                                    if(pres.isEmpty() || pres.contains("*"))pres = "-999";
                                    if(spd.equals("0") || spd.isEmpty() || spd.contains("*")){
                                        spd = "-999.0";
                                        spdUnits = "";
                                    }
                                    if(spdUnits.isEmpty())spdUnits = "";
                                    if(geopotentialHeight.isEmpty())geopotentialHeight = "-999";
                    //</editor-fold>
                                    if(type.equals("forecast")){
                                        //<editor-fold defaultstate="collapsed" desc="預報">
                                        import_tyInfo = String.format("%s,%s,%s",
                                            typhoonName, validTime_Year, getCWBTyphoonNumber);
                                        import_cetrInfo = String.format("%s,%s,%s,-999,%s",
                                            productionCenter, applicationType, modelResolution, dataSource);
                                        import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                            typhoonName, productionCenter, applicationType, typhoonLocalID, typhoonBasin, getCWBTyphoonNumber,
                                            baseTime, validTime, lat, lon, pres, spd, spdUnits,
                                            fcstHour, modelResolution, dataSource, type);
                                        //</editor-fold>                
                                    }else if(type.equals("ensembleForecast")){
                                        //<editor-fold defaultstate="collapsed" desc="系集">
                                        import_tyInfo = String.format("%s,%s,%s",
                                            typhoonName, validTime_Year, getCWBTyphoonNumber);
                                        import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                                            productionCenter, applicationType, modelResolution, geopotentialHeight, dataSource);
                                        import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                            typhoonName, productionCenter, applicationType, typhoonLocalID, typhoonBasin, getCWBTyphoonNumber,
                                            baseTime, validTime, lat, lon, pres, spd, spdUnits,
                                            fcstHour, member, modelResolution, geopotentialHeight, dataSource, type); 
                                        //</editor-fold>
                                    }
                                    hs_tyInfo.add(import_tyInfo);
                                    hs_cetreInfo.add(import_cetrInfo);
                                    al_rawdata.add(import_rawdata); 
                                }
                            }
                        } 
                    }
                }
            }
            // 以上直接引用原始寫法好
            // 以上直接引用原始寫法好
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            // 搬移 xml
            String destURL = String.format("%s/tigge/%s/%s/%s", whereToDownload, centre, yymmdd, fileName);
            if(new File(url).exists()){
                // 有東西才動作
                moveFile(url, destURL);                
            }
            // 測試看看....
            abs_path = null;
            // 建立颱風、單位清單
            setNameAndCentre();
            // 建立 info
            importParsedInfo();
            // 匯入 content
            importParsedRawdata();  
        }
    }//</editor-fold>
    
    /**
     * 解析 CWB Best Track
     * @param url 
     */
    public void parseCWBTrack(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        String dataSource = "besttrack";
        abs_path = Paths.get(url);
        try(BufferedReader br = new BufferedReader(new FileReader(url))) {
            while(br.ready()) {
                String rawData = br.readLine();
                String raw_ty_name_en = rawData.split("\\s+")[0]; // 2008NEOGURI
                String ty_name_en, tyYear;
                if("Typhname".equals(raw_ty_name_en)){
                    // 標題名稱跳過
                    continue;
                }else{
                    tyYear = raw_ty_name_en.substring(0, 4);
                    ty_name_en = raw_ty_name_en.substring(4, raw_ty_name_en.length()).toUpperCase();
                }
                int getCWBTyphoonNumber = ty_list.getCWBTyNumber(ty_name_en, Integer.parseInt(tyYear)); 
                String baseTime_date = rawData.split("\\s+")[1];
                String baseTime_time = rawData.split("\\s+")[2];
                String raw_BaseTime = baseTime_date + " " + baseTime_time + ":00";
                adjust.inputValue(raw_BaseTime);
                String baseTime = adjust.outputYMDH();
                String validTime = baseTime;
                String lat = rawData.split("\\s+")[3];
                String lon = rawData.split("\\s+")[4];
                String pres = rawData.split("\\s+")[5];
                String spd = rawData.split("\\s+")[6];
                int fcstHour = adjust.diffHour(validTime, baseTime);
                String import_tyInfo = String.format("%s,%s,%d",
                    ty_name_en, tyYear, getCWBTyphoonNumber);
                String import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                    "CWB", "Best Track", "", "-999", "cwb");
                String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s",
                    ty_name_en, "CWB","Best Track", "", "", getCWBTyphoonNumber,
                    baseTime, validTime, lat, lon, pres, spd, "m/s",
                    fcstHour, "", "cwb", dataSource);                
                hs_tyInfo.add(import_tyInfo);
                hs_cetreInfo.add(import_cetrInfo);
                al_rawdata.add(import_rawdata); 
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        } finally {
            // 建立颱風、單位清單
            setNameAndCentre();
            // 建立 info
            importParsedInfo();
            // 匯入 content
            importParsedRawdata(); 
        }
    }//</editor-fold>
    
    /**
     * 解析 typhoon 2000
     */
    public void parseTy2000() {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + "typhoon 2000");
        String dataSource = "typhoon2000";
        String[] unit = new String[]{"HKO:","JTWC:","JMA:","NMC:","CWB:","KMA:","PAGASA:"};
        try{
            for (String s : ty_list.getCWBTyEnName(setYear)) {
                String tyList = String.format("name=%s", s);
                Document doc_ty2000 = Jsoup.connect(ty2000URL + tyList).timeout(5000).get();
                Elements elmsScript = doc_ty2000.getElementsByTag("script");
                // 各單位
                // 除錯時會調整的 code : 測站
                for(int stnNb = 0 ; stnNb < unit.length ; stnNb++){
                    ArrayList<String> forecastTime = new ArrayList<>();
                    forecastTime.add("No Data");
                    for(int i = 0 ; i < elmsScript.size() ; i++){
                        if(i != 0){
                            // (1) (前置作業)擷取所有 javascript 文字
                            String allJavaScript = elmsScript.get(i).html();
                            // (2) (前置作業)利用 text[j] 來區分矩陣範圍，j = 可比喻成預報時間
                            // 設定矩陣時間
                            // 因為 forecastTime index 須從 0 開始，但資料卻從 1 開始 : java.lang.IndexOutOfBoundsException: Index: 1, Size: 1
                            int j = 0;
                            while(true){
                                j++;
                                // 區分矩陣範圍
                                String[] data_step1 = allJavaScript.split("text\\[" + j + "\\] = ");
                                if(data_step1.length == 1)break;
                                int k = 1;
                                // (3) 擷取 text[number] 內容
                                String rawdata_v1 = data_step1[k].split(";")[0];//System.out.println(rawdata_v1);
                                int rawdataLengh = rawdata_v1.length();
                                // (4) 輸出我想要的原始格式
                                String rawdata_v2 = rawdata_v1.substring(1, rawdataLengh - 1);//System.out.println(j + "," + rawdata_v2);  
                                if(!rawdata_v2.contains(unit[stnNb])){
                                    // 防止有一些單位不是從同一個時間才跑的資料
                                    forecastTime.add("No Data");
                                }
                                // (4-1) 區分斷行符號 \n
                                String[] rawdata_v3 = rawdata_v2.split("\\\\n");
                                String raw_BaseTime = rawdata_v3[1].substring(1, rawdata_v3[1].length() - 1);
                                adjust.inputValue(raw_BaseTime);
                                String basetime_yymmdd = adjust.outputYMDH().substring(0, 10);// 2015-09-22
                                ArrayList<String> al_rawdata_v2 = new ArrayList<>();
                                al_rawdata_v2.addAll(Arrays.asList(rawdata_v3));//for(String s : al_rawdata_v2)System.out.println(s);
                                // (4-2) 區分
                                for(int x = 0 ; x < al_rawdata_v2.size() ; x++){
                                    /** 取個測站的範圍 **/
                                    int initNumber = al_rawdata_v2.indexOf(unit[stnNb]);
                                    int stopNumber = 0;
                                    // unit.length = 7                                        
                                    if(stnNb < 5){
                                        stopNumber = al_rawdata_v2.indexOf(unit[stnNb+1]);
                                        // 防止有些預報單位沒有資料(when stopNumber = -1)
                                        if(stopNumber == -1){
                                            for(int z = 2 ; z < (7 - stnNb) ; z++){
                                                /** unit = new String[]{"HKO:","JTWC:","JMA:","NMC:","CWB:","KMA:","PAGASA:"}; **/
                                                stopNumber = al_rawdata_v2.indexOf(unit[stnNb + z - 1]);
                                                if(stopNumber == -1){
                                                    stopNumber = initNumber + unit.length;
                                                }else{
                                                    break;
                                                }
                                            }
                                        }
                                    }else if(stnNb == 5){
                                        stopNumber = al_rawdata_v2.indexOf(unit[stnNb+1]);
                                        if(stopNumber == -1){
                                            stopNumber = initNumber + unit.length;
                                        }
                                    }else{
                                        stopNumber = initNumber + unit.length;
                                    }
                                    // 驗證用
//                                    System.out.println(x + "\t" + initNumber + " - " + stopNumber);
                                    if(initNumber == -1){
                                        break;
                                    }else if(initNumber <= x && x < stopNumber){
//                                        String baseTime = "", validTime = "", fcstHour = "";
//                                        String ty_name_en = al_rawdata_v2.get(0).split("\\s")[2];
                                        if(x == 0 || x == 1 || x == 2){
                                            // x == 0 : TROPICAL CYCLONE KROSA
                                            // x == 1 : (2013-10-29 21:15:18 UTC)
                                            // x == 2 : ==========================
                                        }else{
                                            if(!al_rawdata_v2.get(x).equals(unit[stnNb])){
                                                String baseTime = "", validTime = "", fcstHour = "";
                                                String productionCenter = unit[stnNb].substring(0, unit[stnNb].length()-1);
                                                // 網頁資料輸入時間
                                                String webDataInputTime = al_rawdata_v2.get(1).substring(1, al_rawdata_v2.get(1).length() - 1);
                                                adjust.inputValue(webDataInputTime);
                                                long long_webDataInputTime = adjust.outputNumber();
                                                // 除錯時會調整的 code : 單位
//                                                if(!weatherUnit.equals("HKO"))continue;
                                                String offsetTime = al_rawdata_v2.get(x).split("\\s+")[0];
                                                // 人工除錯，offsetTime = UTCZ
                                                if("UTCZ".equals(offsetTime))offsetTime = "291800Z";
                                                if(!(offsetTime.contains("(") && offsetTime.contains(")"))){
//                                                    if(!isNumeric(al_rawdata_v2.get(x).substring(0, 1))){
                                                    if(!ty_list.isNumeric(offsetTime.substring(0, 1))){
                                                        break;
                                                    }
                                                    String h = offsetTime.substring(2, 4);//System.out.println(h);
                                                    String m = offsetTime.substring(4, 6);//System.out.println(m);
                                                    // java.lang.StringIndexOutOfBoundsException: String index out of range: 6
                                                    offsetTime = String.format("%s:%s:00", h, m);
                                                    baseTime = String.format("%s %s", basetime_yymmdd, offsetTime);
                                                    forecastTime.add(baseTime);
                                                    validTime = baseTime;
                                                    fcstHour = "0";
                                                }else if( "(+024H)".equals(offsetTime) || "(+048H)".equals(offsetTime) || "(+072H)".equals(offsetTime) ||
                                                          "(+096H)".equals(offsetTime) || "(+120H)".equals(offsetTime)) {
                                                    int forecastHour = Integer.parseInt(offsetTime.substring(2, 5));
                                                    // java.lang.IndexOutOfBoundsException: Index: 4, Size: 2
                                                    // java.lang.IndexOutOfBoundsException: Index: 2, Size: 2
                                                    // java.lang.IndexOutOfBoundsException: Index: 4, Size: 4
//                                                    System.out.println(forecastTime.size());
                                                    baseTime = forecastTime.get(j);
                                                    Calendar calendar = Calendar.getInstance();
                                                    adjust.inputValue(baseTime);
                                                    calendar.setTime(adjust.outputJavaDate());
                                                    calendar.add(Calendar.HOUR_OF_DAY, forecastHour);
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                    validTime = String.format("%s", sdf.format(calendar.getTime()));
                                                    fcstHour = String.valueOf(forecastHour);
                                                }
                                                String lat = al_rawdata_v2.get(x).split("\\s+")[1].replaceAll("N", "");
                                                String lon = al_rawdata_v2.get(x).split("\\s+")[2].replaceAll("E", "");
                                                // 簡單除錯
                                                if(lon.contains("W")){
                                                    break;
                                                }else if(lat.contains("E")){
                                                    lat = al_rawdata_v2.get(x).split("\\s+")[1].replaceAll("E", "");
                                                }
                                                String spd, spdUnit;
                                                if(al_rawdata_v2.get(x).split("\\s+").length == 4){
                                                    if("---".equals(al_rawdata_v2.get(x).split("\\s+")[3].replaceAll("KT", ""))){
                                                        // 人工除錯(土法煉鋼)，不知問題在哪
                                                        // java.lang.ArrayIndexOutOfBoundsException: 3
                                                        spd = "-999.0";
                                                        spdUnit = "";
                                                    }else{
                                                        spd = al_rawdata_v2.get(x).split("\\s+")[3].replaceAll("KT", "");
                                                        spdUnit = "kt";
                                                    }
                                                }else{
                                                    // 人工除錯(土法煉鋼)，不知道問題在哪
                                                    lon = al_rawdata_v2.get(x).split("\\s+")[2].replaceAll("E", "").substring(0, 5);
                                                    spd = al_rawdata_v2.get(x).split("\\s+")[2].replaceAll("E", "").substring(6, 9).replaceAll("KT", "");//                       
                                                    spdUnit = "kt";
                                                }
//                                                System.out.println(typhoonName + "," + adjust.getNowTime());
                                                adjust.inputValue(baseTime);
                                                long long_basetime = adjust.outputNumber();
                                                // 如果時間差到 7 小時以上，認定是錯誤資料
                                                if((long_webDataInputTime - long_basetime)/(3600*1000) < 7){
                                                    String ty_name_en = al_rawdata_v2.get(0).split("\\s")[2];
                                                    String tyYear = setYear + "";
                                                    int getCWBTyphoonNumber = ty_list.getCWBTyNumber(ty_name_en, setYear); 
//                                                    int fcstHour = adjust.diffHour(validTime, baseTime);
                                                    String import_tyInfo = String.format("%s,%s,%d",
                                                        ty_name_en, tyYear, getCWBTyphoonNumber);
                                                    String import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                                                        productionCenter, "Offical", "", "-999", dataSource);
                                                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                                        ty_name_en, productionCenter,"Offical", "", "", getCWBTyphoonNumber,
                                                        baseTime, validTime, lat, lon, "-999", spd, "kt",
                                                        fcstHour, "", dataSource, "forecast");    
                                                    hs_tyInfo.add(import_tyInfo);
                                                    hs_cetreInfo.add(import_cetrInfo);
                                                    al_rawdata.add(import_rawdata);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    };  
                    forecastTime.clear();
                } 
            }
        } catch(IOException ex) {
            ex.printStackTrace();
            // 因為 http"s" 所導致的問題 : javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        } finally {
            // 建立颱風、單位清單
            setNameAndCentre();
            // 建立 info
            importParsedInfo();
            // 匯入 content
            importParsedRawdata();
        }
    }//</editor-fold>
    
    /**
     * 解析 JTWC
     * @param url
     */
    public void parseJTWC(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        String dataSource = "jtwc web";
        ArrayList<String> al_PrintResult = new ArrayList<>();
        ArrayList<String> al_temp = new ArrayList<>();
        ArrayList<String> al_PrintFinalResult = new ArrayList<>();
        // 計算有相同的時間有幾組資料
        LinkedHashSet<String> timeList_All = new LinkedHashSet<>();
        // 計算颱風名稱
        LinkedHashSet<String> typhoonName_All = new LinkedHashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(url))) {
            // (1) 先把檔案納入記憶體內
            //<editor-fold defaultstate="collapsed" desc="...">
            ArrayList<String> al_validTime = new ArrayList<>();
            while(br.ready()){
                String rawdata = br.readLine();
                // 防止檔案最後一行有空欄位
                if(rawdata.length() == 0 /*|| rawdata.split(",").length == 23*/)continue;
                // 忽略空格
                String speed = rawdata.split(",")[8].trim();
                // 有一些氣象局是輕颱，但JTWC是熱低壓，所以改成 30kt 以上都考慮進來
                if(Integer.parseInt(speed) >= 30){
                    String typhoonLocalID = "";
                    String typhoonBasin = rawdata.split(",")[0].trim();
                    String typhoonNumber = rawdata.split(",")[1].trim();
                    String raw_BaseTime = rawdata.split(",")[2].trim();
                    String validTime_Year = raw_BaseTime.substring(0, 4);
                    adjust.inputValue(raw_BaseTime);
                    String baseTime = adjust.outputYMDH();
                    String validTime = baseTime;
                    al_validTime.add(validTime);
                    String typhoonName;
                    String minima_pressuresure;
                    String development;
                    String wtf_RAD;
                    String wtf_WindCode;
                    String wtf_RAD1;
                    String wtf_RAD2;
                    String wtf_RAD3;
                    String wtf_RAD4;
                    int rawdataLength = rawdata.split(",").length;
                    if(rawdataLength < 8){
                        break;
                    }else if(rawdataLength <= 11){
                        typhoonName = "";
                        minima_pressuresure = "-999";
                        development = "";
                        wtf_RAD = "";
                        wtf_WindCode = "";
                        wtf_RAD1 = "";
                        wtf_RAD2 = "";
                        wtf_RAD3 = "";
                        wtf_RAD4 = ""; 
                    }else if(rawdata.split(",").length <= 27){
                        typhoonName = "";
                        minima_pressuresure = rawdata.split(",")[9].trim();
                        development = rawdata.split(",")[10].trim();
                        wtf_RAD = rawdata.split(",")[11].trim();
                        wtf_WindCode = rawdata.split(",")[12].trim();
                        wtf_RAD1 = rawdata.split(",")[13].trim();
                        wtf_RAD2 = rawdata.split(",")[14].trim();
                        wtf_RAD3 = rawdata.split(",")[15].trim();
                        wtf_RAD4 = rawdata.split(",")[16].trim();
                    }else{
                        typhoonName = rawdata.split(",")[27].trim();
//                        minima_pressuresure = "-999";
//                        development = "";
                        minima_pressuresure = rawdata.split(",")[9].trim();
                        development = rawdata.split(",")[10].trim();
                        wtf_RAD = rawdata.split(",")[11].trim();
                        wtf_WindCode = rawdata.split(",")[12].trim();
                        wtf_RAD1 = rawdata.split(",")[13].trim();
                        wtf_RAD2 = rawdata.split(",")[14].trim();
                        wtf_RAD3 = rawdata.split(",")[15].trim();
                        wtf_RAD4 = rawdata.split(",")[16].trim();
                    }
                    // 利用 regex 處理混合字串
                    double d_latitude = Double.parseDouble(rawdata.split(",")[6].trim().replaceAll("[a-zA-Z]",""))/10.0;
                    String latitudeitude = d_latitude + "";
                    double d_longitude = Double.parseDouble(rawdata.split(",")[7].trim().replaceAll("[a-zA-Z]",""))/10.0;
                    String longitude = d_longitude + "";
                    String output, typhoonNumber_New;
                    // 因為風速改成 30kt，所以 TD 也要考慮進來
                    if(development.matches("[T][DSY]") || development.matches("[S][T]") || development.equals("")){
                        if(wtf_WindCode.isEmpty())wtf_WindCode = "";
                        if(typhoonNumber.isEmpty()){
                            typhoonNumber_New = typhoonNumber;
                        }else{
                            typhoonNumber_New = validTime_Year + typhoonNumber;
                        }
                        output = String.format(
                            "%s,JTWC,Best Track,,%s,%s,%s,%s,%s,%s,%s,%s,kt,%s,best track,%s,%s,%s,%s,%s,%s,%s",
                            typhoonName,typhoonBasin,typhoonNumber_New,
                            baseTime,validTime,latitudeitude,longitude,minima_pressuresure,speed,development,dataSource,
                            wtf_RAD,wtf_WindCode,wtf_RAD1,wtf_RAD2,wtf_RAD3,wtf_RAD4);
                        al_temp.add(output);
                        timeList_All.add(baseTime);
                    } 
                }
            }//</editor-fold>
            // (2) 合併原始檔案
            //<editor-fold defaultstate="collapsed" desc="...">
            Iterator it = timeList_All.iterator();
            ArrayList<String> afterQC = new ArrayList<>();
            while(it.hasNext()){
                String timeList_NotRepeat = it.next().toString();
//                System.out.println(timeList_NotRepeat);
                String afterComebine = "";
                String result;
                String typhoon_name_store_in_ram = "", typhoon_name_temp;
                int i = 0;
                for (String output : al_temp) {
                    String timelist = output.split(",")[6];
//                    String typhoonData = output.split(output.split(",")[13])[0];
                    typhoon_name_temp = output.split(",")[0];
                    if(!typhoon_name_temp.isEmpty()){
                        // 颱風名稱存在，則存入 typhoon_name_store_in_ram 記憶體內
                        typhoon_name_store_in_ram = typhoon_name_temp;
                    }else{
                        // 颱風名稱不存在，再把存在 typhoon_name_store_in_ram 記憶體的颱風名稱叫出來放到 temp 內
                        typhoon_name_temp = typhoon_name_store_in_ram;
                    }    
                    if(timeList_NotRepeat.equals(timelist)){
                        String splitWord = String.format("kt,%s,best track,%s,", output.split(",")[13], dataSource);
                        String endList = output.split(splitWord)[1];
                        afterComebine += "," + endList;
//                        String result = i + "," + timeList_NotRepeat + afterComebine;
                        // 舊式寫法
                        result = i + "," + output.substring(0, output.length() - endList.length() - 1) + afterComebine;
                        // 處理沒有颱風名稱的狀況
                        if(!result.split(",")[1].isEmpty()){
                            // 當颱風名稱不為空值時
                            // 0,MEKKHALA,JTWC,Best Track,,WP,
//                            System.out.println(result);
                            afterQC.add(result);
                        }else{
                            // 當颱風名稱為空值時
                            // 0,,JTWC,Best Track,,WP,
//                            System.out.println(result.replaceAll(",JTWC,Best Track,", typhoon_name_temp + ",JTWC,Best Track,"));
                            afterQC.add(result.replaceAll(",JTWC,Best Track,", typhoon_name_temp + ",JTWC,Best Track,"));
                        }
                        i++;
                    }
                }
            }//</editor-fold>
            // (3) 輸出我想要的結果
            //<editor-fold defaultstate="collapsed" desc="...">
            // 文字排序
            Comparator<String> comparator = new Comparator<String>() {
                // 回傳正負號差在由小排到大或由大排到小
                @Override
                public int compare(String o1, String o2) {
                    return (Integer.parseInt(o2.split(",")[0]) - Integer.parseInt(o1.split(",")[0]));
                }
            };
            Collections.sort(afterQC, comparator);
            Iterator it_timeList_All = timeList_All.iterator();
            while(it_timeList_All.hasNext()){
                String timeList_NotRepeat = it_timeList_All.next().toString();
                for(String output : afterQC){
                    String temp;
                    String timelist = output.split(",")[7];
                    if(timeList_NotRepeat.equals(timelist)){
                        int judgeArrayAccount = output.substring(2).split(",").length;
                        if(judgeArrayAccount == 34){
                            temp = String.format("%s,,,,,,,JustForDeBug", output.substring(2));
//                            al_PrintResult.add(String.format("%s,,,,,,,", output.substring(2))); 沒有 1 會錯
//                            al_PrintResult.add(String.format("%s,,,,,,,JustForDeBug", output.substring(2)));
                        }else if(judgeArrayAccount == 28){
                            temp = String.format("%s,,,,,,,,,,,,,JustForDeBug", output.substring(2));
//                            al_PrintResult.add(String.format("%s,,,,,,,,,,,,,", output.substring(2))); 沒有 1 會錯
//                            al_PrintResult.add(String.format("%s,,,,,,,,,,,,,JustForDeBug", output.substring(2)));
                        }else if(judgeArrayAccount == 22){
                            temp = String.format("%s,,,,,,,,,,,,,,,,,,,JustForDeBug", output.substring(2));
//                            al_PrintResult.add(String.format("%s,,,,,,,,,,,,,,,,,,,", output.substring(2))); 沒有 1 會錯
//                            al_PrintResult.add(String.format("%s,,,,,,,,,,,,,,,,,,,JustForDeBug", output.substring(2)));
                        }else{
                            temp = output.substring(2);
//                            al_PrintResult.add(output.substring(2));
                        }
                        al_PrintResult.add(temp);
                        typhoonName_All.add(output.substring(2).split(",")[0].trim());
                        break;
                    }
                }
            }//</editor-fold>
//            System.out.println(typhoonName_All.size());
            for(String s : al_PrintResult) {
                String typhoon_name;
                // typhoonName_All = [INVEST, TEN,, MATMO]
                if(typhoonName_All.isEmpty()){
                    // 空字串跳過
                    break;
                }else{
                    if(s.split(",")[0].isEmpty()){
                        // 大膽假設，最後一個名稱是正確颱風名稱
                        typhoon_name = typhoonName_All.toArray()[typhoonName_All.size() - 1].toString();
                        
                    }else{
                        // 如果沒有 one、two...，代表單一檔案的颱風數目只有一個
                        if(typhoonName_All.toArray().length == 1){
                            typhoon_name = typhoonName_All.toArray()[0].toString();
                        }else{
                            if(typhoonName_All.toArray()[typhoonName_All.size() - 1].toString().isEmpty()){
                                typhoon_name = typhoonName_All.toArray()[typhoonName_All.size() - 2].toString();
                            }else{
                                typhoon_name = typhoonName_All.toArray()[typhoonName_All.size() - 1].toString();
                            }
                        }
                    }
                }
                if(ty_list.isEnglishNumber(typhoon_name)){
                    // 排除 JTWC 特有的擾動、熱低壓編號，名稱是數字皆視為異類
                    // 小心 PHANFONE 被視為數字 "ONE"
                    break;
                }else{
                    // s : 原始全部資料
                    String raw_TyphoonName = s.split(",")[0].trim();
                    if(ty_list.isEnglishNumber(raw_TyphoonName) || "INVEST".equals(raw_TyphoonName)){
                        // 如果有英文數字或 INVEST，則取代英文數字或 INVEST
                        al_PrintFinalResult.add(s.replace(raw_TyphoonName, typhoon_name));
                    }else if(raw_TyphoonName.isEmpty()){
                        // 沒有名稱，則外加名稱上去
                        al_PrintFinalResult.add(s.replace(raw_TyphoonName + ",JTWC", typhoon_name + ",JTWC"));
                    }else{
                        // 正常
                        al_PrintFinalResult.add(s);
                    }   
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
            // 最後面少了 1 或文字 就會出現此狀態 java.lang.ArrayIndexOutOfBoundsException: 21 
        }
        // 改變颱風編號(增快速度)
        for(String s : al_PrintFinalResult) {
            String ty_name_en = s.split(",")[0];
            String jtwcTyphoonNumber = s.split(",")[5];
            
            String baseTime = s.split(",")[6];
            String validTime = baseTime;
            int tyYear = Integer.parseInt(baseTime.substring(0, 4));
            int getCWBTyphoonNumber = 0;
            String final_output;
            if(ty_name_en.isEmpty()){
                // 2004年以前，都沒有颱風名稱
                final_output = s;
            }else{
                // 2005年之後
                getCWBTyphoonNumber = ty_list.getCWBTyNumber(ty_name_en, tyYear);
                final_output = s.replaceAll(",WP," + jtwcTyphoonNumber, ",WP," + getCWBTyphoonNumber);
            }
            String lat = final_output.split(",")[8];
            String lon = final_output.split(",")[9];
            String pres = final_output.split(",")[10];
            String spd = final_output.split(",")[11];
            int fcstHour = adjust.diffHour(validTime, baseTime);
            // 以下是新增的
            String import_tyInfo = String.format("%s,%s,%d",
                ty_name_en, tyYear, getCWBTyphoonNumber);
            String import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                "JTWC", "Best Track", "", "-999", dataSource);
            String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s",
                ty_name_en, "JTWC","Best Track", "", "", getCWBTyphoonNumber,
                baseTime, validTime, lat, lon, pres, spd, "kt",
                fcstHour, "", dataSource, "besttrack");    
            hs_tyInfo.add(import_tyInfo);
            hs_cetreInfo.add(import_cetrInfo);
            al_rawdata.add(import_rawdata);     
            // 以上是新增的
        }
        // 建立颱風、單位清單
        setNameAndCentre();
        // 建立 info
        importParsedInfo();
        // 匯入 content
        importParsedRawdata();        
    }//</editor-fold>
    
    /**
     * 解析 CWB WEPS
     * @param url 
     */
    public void parseCWBWEPS(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        ArrayList<String> al_PrintResult = new ArrayList<>();
        String dataSource = "cwb";
        try(BufferedReader br = new BufferedReader(new FileReader(url))) {   
            Path path = Paths.get(url);
            String directoryName = path.getParent().getName(path.getNameCount() - 2).toString(); // 15100418_MUJIGAE
            String fileName = path.getFileName().toString(); // EFS-01_15100418_SLP
            if(!fileName.startsWith("N")){
                String typhoon_name_En = directoryName.split("_")[1];
                String ensembleMember = "";
                if(fileName.startsWith("EFS-")){
                    ensembleMember = fileName.split("_")[0];
                }else if(fileName.matches("M0[0-9].*")){
                    ensembleMember = fileName.split("-")[0];
                }else if(fileName.contains("_EFS-mean_")){
                    ensembleMember = fileName.split("_")[1];
                }
                // 設定初始場時間
                int judgeNumber = 0;
                String output, baseTime = "";
                while(br.ready()){
                    // 先測試一個颱風就好
//                    if(!"MATMO".equals(typhoon_name_En))break;
                    String rawData = br.readLine();
                    String raw_BaseTime = rawData.split("\\s+")[0];
                    adjust.inputValue(raw_BaseTime);
                    String validTime = "";
                    if(judgeNumber == 0){
                        baseTime = adjust.outputYMDH();
                        validTime = baseTime;
                    }else{
                        validTime = adjust.outputYMDH();
                    }
//                    Calendar calendar = Calendar.getInstance();
                    adjust.inputValue(baseTime);
                    long initialTime = adjust.outputNumber();
                    adjust.inputValue(validTime);
                    long endTime = adjust.outputNumber();
                    int fcstHour = (int) ((endTime - initialTime)/1000/60/60);
//                    calendar.setTime(adjustTimeFormat.outputJavaDate());
//                    calendar.add(Calendar.HOUR_OF_DAY, forecastHour);
                    DecimalFormat df = new DecimalFormat("###.##");
                    String latitudeitude, longitude;
                    double temp_Latitude, temp_Longitude;
                    if(rawData.split("\\s+")[1].matches(".*[a-zA-Z].*") || rawData.split("\\s+")[2].matches(".*[a-zA-Z].*")){
                        temp_Latitude = -999.0;
                        temp_Longitude = -999.0;
                    }else{
                        temp_Latitude = Double.parseDouble(rawData.split("\\s+")[1]);
                        temp_Longitude = Double.parseDouble(rawData.split("\\s+")[2]);
                    }
                    if(temp_Latitude <= -200.0 || temp_Longitude <= -200.0){
                        latitudeitude = "-999.0";
                        longitude = "-999.0";
                    }else{
                        latitudeitude = df.format(Double.parseDouble(rawData.split("\\s+")[1]));
                        longitude = df.format(Double.parseDouble(rawData.split("\\s+")[2]));
                    }
                    String minima_pressuresure;
                    if(rawData.split("\\s+").length <= 3){
                        minima_pressuresure = "";
                    }else{
                        minima_pressuresure = rawData.split("\\s+")[3];
                    }
                    output = String.format("%s,CWB,WEPS,,,,%s,%s,%s,%s,%s,-999.0,,%s,%s,,-999,%s", 
                                typhoon_name_En, baseTime, validTime, latitudeitude, longitude ,
                                minima_pressuresure, fcstHour, ensembleMember, dataSource);
//                    System.out.println(output);
                    al_PrintResult.add(output);
                    judgeNumber++;
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        // 改變颱風編號(增快速度)
        for(String s : al_PrintResult) {
            String typhoonName = s.split(",")[0];
            String baseTime = s.split(",")[6];            
            int basetimeYear = Integer.parseInt(baseTime.substring(0, 4));            
            int getCWBTyphoonNumber = ty_list.getCWBTyNumber(typhoonName, basetimeYear);
            String final_output = s.replaceAll(",WEPS,,,,", ",WEPS,,," + getCWBTyphoonNumber + ",");
            String validTime = final_output.split(",")[7];
            String lat = final_output.split(",")[8];
            String lon = final_output.split(",")[9];
            String pres = final_output.split(",")[10];
            if(pres.isEmpty())pres = "-999.0";
            String spd = final_output.split(",")[11];
            String fcstHour = final_output.split(",")[13];
            String member = final_output.split(",")[14];
            String import_tyInfo = String.format("%s,%s,%s",
                typhoonName, basetimeYear, getCWBTyphoonNumber);
            String import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                "CWB", "WEPS", "", "-999", dataSource);
            String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                typhoonName, "CWB", "WEPS", "", "WP", getCWBTyphoonNumber,
                baseTime, validTime, lat, lon, pres, spd, "",
                fcstHour, member, "", "-999", dataSource, "ensembleForecast"); 
            hs_tyInfo.add(import_tyInfo);
            hs_cetreInfo.add(import_cetrInfo);
            al_rawdata.add(import_rawdata);
        }   
        // 建立颱風、單位清單
        setNameAndCentre();
        // 建立 info
        importParsedInfo();
        // 匯入 content
        importParsedRawdata();         
    }//</editor-fold>
    
    /**
     * 是否跑
     * @param parameter
     * @return 跑或不跑 
     */
    public boolean setRunOrNotRun(String parameter) { //<editor-fold defaultstate="collapsed" desc="...">
        return "1".equals(prop.get(parameter));
    }//</editor-fold>
    
    /**
     * 正規化颱風清單和單位清單
     */
    public void setNameAndCentre() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            /********************** 會用到的物件 **********************/
            String judgeExistSQL, insertSQL; 
            /********************** 建立颱風清單 **********************/
            String ty_name, ty_name_tw, ty_year, ty_number;
            for (String s : hs_tyInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                ty_name = s.split(regex)[0];
                ty_year = s.split(regex)[1];
                ty_number = s.split(regex)[2];
                ty_name_tw = ty_list.getCWBTyTWName(Integer.parseInt(ty_number));
                judgeExistSQL = "SELECT COUNT(*) FROM TyphoonInfo WHERE ty_name_en = ? AND ty_year = ? AND ty_number = ?";
                // 判斷是否存在該颱風，判斷條件: 颱風名稱、年分、編號
                // 如果不存在，新建該颱風清單(用 SQL 語法去找存不存在)
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, ty_name);
                prepStmt.setString(2, ty_year);
                prepStmt.setString(3, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        // 不存在才更新(value == 0 代表不存在，select 不到)
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO TyphoonInfo VALUES (?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO TyphoonInfo VALUES (0,?,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_name);
                        prepStmt.setString(2, ty_name_tw);
                        prepStmt.setString(3, ty_year);
                        prepStmt.setString(4, ty_number);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }//</editor-fold>
            /********************** 建立單位清單 **********************/
            String centre, model, resolution, gh, data_source;
            for (String s : hs_cetreInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                // centre 上面已出現過，直接使用就好
                centre = s.split(regex)[0];
                model = s.split(regex)[1];
                resolution = s.split(regex)[2];
                gh = s.split(regex)[3];
                data_source = s.split(regex)[4];
                judgeExistSQL = 
                    "SELECT COUNT(*) FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                // 判斷是否存在該單位，判斷條件: 單位、模式、解析度、高度、來源
                // 如果不存在，新建該單位清單(用 SQL 語法去找存不存在)
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, centre);
                prepStmt.setString(2, model);
                prepStmt.setString(3, resolution);
                prepStmt.setString(4, gh);
                prepStmt.setString(5, data_source);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        // 不存在才更新(value == 0 代表不存在，select 不到)
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO CentreInfo VALUES (?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO CentreInfo VALUES (0,?,?,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, centre);
                        prepStmt.setString(2, model);
                        prepStmt.setString(3, resolution);
                        prepStmt.setString(4, gh);
                        prepStmt.setString(5, data_source);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }                    	
                }
            }//</editor-fold>
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 正規化 Info
     */
    public void importParsedInfo() {//<editor-fold defaultstate="collapsed" desc="...">
        String outputException = "";
        try {
            /********************** 準備各 Type Info 資料 **********************/
            // 原則上上面沒出現過的變數，就和資料庫一樣就好
            String establishFK_ty, establishFK_cetr;
            String ty_info_id, centre_info_id, base_time, type, member;
            String import_anlyInfo, import_foreInfo, import_ensbInfo, import_bstkInfo;
            for (String s : al_rawdata) {
                outputException = "exception: " + s;// 找錯用
                int rawdata_columns = s.split(regex).length;
                ty_info_id = "";
                // 大家位置都一樣(rawdata_columns)
                String ty_name = s.split(regex)[0];
                String ty_number = s.split(regex)[5];
                String centre = s.split(regex)[1];
                String model = s.split(regex)[2];
                base_time = s.split(regex)[6];
                // 新增 member
                String data_source = s.split(regex)[rawdata_columns - 2];
                type = s.split(regex)[rawdata_columns - 1];
                // 位置不一樣(rawdata_columns)
                // 用颱風名稱和編號判斷
                establishFK_ty = "SELECT ty_info_id FROM TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(establishFK_ty);
                prepStmt.setString(1, ty_name);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                }
                // 考慮不同狀態，要考慮的欄位不一樣
                if("analysis".equals(type) || "forecast".equals(type) || "besttrack".equals(type)){
                    // 用單位、模式、來源判斷
                    //<editor-fold defaultstate="collapsed" desc="...">
                    establishFK_cetr = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND data_source = ?";
                    prepStmt = conn.prepareStatement(establishFK_cetr);
                    prepStmt.setString(1, centre);
                    prepStmt.setString(2, model);
                    prepStmt.setString(3, data_source); 
                    //</editor-fold>
                }else if("ensembleForecast".equals(type)){
                    // 用單位、模式、來源判斷、解析度、高度
                    //<editor-fold defaultstate="collapsed" desc="...">
                    String resolution = s.split(regex)[rawdata_columns - 4];
                    String gh = s.split(regex)[rawdata_columns - 3];
                    establishFK_cetr = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                    prepStmt = conn.prepareStatement(establishFK_cetr);
                    prepStmt.setString(1, centre);
                    prepStmt.setString(2, model);
                    prepStmt.setString(3, resolution);
                    prepStmt.setString(4, gh);
                    prepStmt.setString(5, data_source);  
                    //</editor-fold>
                }                 
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                    if(null != type) // 匯入各 Type Info 資訊
                    // 放外面會使 centre_info_id = null
                    switch (type) {
                        case "analysis":
                            import_anlyInfo = String.format("%s,%s,%s", ty_info_id, centre_info_id, base_time);
                            hs_anlyInfo.add(import_anlyInfo);
                            break;
                        case "forecast":
                            import_foreInfo = String.format("%s,%s,%s", ty_info_id, centre_info_id, base_time);
                            hs_foreInfo.add(import_foreInfo);
                            break;
                        case "ensembleForecast":
                            member = s.split(regex)[14];
                            import_ensbInfo = String.format("%s,%s,%s,%s", ty_info_id, centre_info_id, base_time, member);
                            hs_ensbInfo.add(import_ensbInfo);
                            break;
                        case "besttrack":
                            import_bstkInfo = String.format("%s,%s,%s", ty_info_id, centre_info_id, base_time);
                            hs_bstkInfo.add(import_bstkInfo);
                            break;
                        default:
                            break;
                    }
                }    
            } 
        } catch (SQLException ex) {
            System.out.println("exception: " + outputException);
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 正規化 Content
     */
    public void importParsedRawdata() {//<editor-fold defaultstate="collapsed" desc="...">
        String ty_name_en, ty_number;
        String centre, model, resolution, geopotential_height, data_source;
        String a_info_id = "", f_info_id = "", e_info_id = "", bt_info_id = "";
        String ty_info_id = "", centre_info_id = "", base_time, member;
        String insertSQL, judgeExistSQL;
        String outputException = "";
        int value;
        try {
            // 應該不用標籤，因為每個都要跑過一次
/********** 分析場 **********/            
/********** 分析場 **********/
            for (String s : hs_anlyInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                // 從文字檔下手，比較資料庫的狀況
                ty_info_id = s.split(regex)[0];
                centre_info_id = s.split(regex)[1];
                base_time = s.split(regex)[2];
                // 如果不存在，新建各 Type Info (用 SQL 語法去找存不存在)
                insertSQL = "";
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM AnalysisInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的各 Type Info 才更新(value == 0 代表不存在，select 不到)        
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO AnalysisInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO AnalysisInfo VALUES (0,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_info_id);
                        prepStmt.setString(2, centre_info_id);
                        prepStmt.setString(3, base_time);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            // 從 rawdata 匯入 detail table
            /* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
            /* vvvvvvvvvvvv 改分析場 Content Table 的 code  vvvvvvvvvvvv */
            for (String s : al_rawdata) {
                int rawdata_columns = s.split(regex).length;
                String type = s.split(regex)[rawdata_columns - 1];
                // 只保留 analysis
                if(!"analysis".equals(type))continue;
                // 找出關連性
                // (1) 找出 XXXcontent table 的 rawdata
                String lat = s.split(regex)[8];
                String lon = s.split(regex)[9];
                String min_pres = s.split(regex)[10];
                String wind = s.split(regex)[11];
                String wind_unit = s.split(regex)[12];
                String development = s.split(regex)[13];
                String data_type = s.split(regex)[14];
                // (2) 關連 tyinfo table 的 rawdata
                ty_name_en = s.split(regex)[0];
                ty_number = s.split(regex)[5];
                String putTyInfoSQL = "SELECT ty_info_id FROM TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(putTyInfoSQL);
                prepStmt.setString(1, ty_name_en);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                }
                // (3) 關連 centre table 的 rawdata
                centre = s.split(regex)[1];
                model = s.split(regex)[2];
                resolution = s.split(regex)[rawdata_columns - 4];
                geopotential_height = s.split(regex)[rawdata_columns - 3];
                data_source = s.split(regex)[rawdata_columns - 2];
                String putCentreInfoSQL = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                prepStmt = conn.prepareStatement(putCentreInfoSQL);
                prepStmt.setString(1, centre);
                prepStmt.setString(2, model);
                prepStmt.setString(3, resolution);
                prepStmt.setString(4, geopotential_height);
                prepStmt.setString(5, data_source);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                }
                // (4) 關連 XXXinfo table 的 rawdata
                base_time = s.split(regex)[6];
                String putXXXInfoSQL = "SELECT a_info_id FROM AnalysisInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    a_info_id = rs.getString(1);
                }
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO AnalysisContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, a_info_id);
                prepStmt.setString(2, lat);
                prepStmt.setString(3, lon);
                prepStmt.setString(4, min_pres);
                prepStmt.setString(5, wind);
                prepStmt.setString(6, wind_unit);
                prepStmt.setString(7, development);
                prepStmt.setString(8, data_type);
                prepStmt.executeUpdate();
            }
            /* ^^^^^^^^^^^^ 改分析場 Content Table 的 code  ^^^^^^^^^^^^ */
            /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
            //</editor-fold>
/********** 預報場 **********/            
/********** 預報場 **********/
            for (String s : hs_foreInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                // 從文字檔下手，比較資料庫的狀況
                ty_info_id = s.split(regex)[0];
                centre_info_id = s.split(regex)[1];
                base_time = s.split(regex)[2];
                // 如果不存在，新建各 Type Info (用 SQL 語法去找存不存在)
                insertSQL = "";                
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM ForecastInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";               
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的各 Type Info 才更新(value == 0 代表不存在，select 不到)
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO ForecastInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO ForecastInfo VALUES (0,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_info_id);
                        prepStmt.setString(2, centre_info_id);
                        prepStmt.setString(3, base_time);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                } 
            }
            /* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
            /* vvvvvvvvvvvv 改預報場 Content Table 的 code  vvvvvvvvvvvv */
            for (String s : al_rawdata) {
                int rawdata_columns = s.split(regex).length;
                String type = s.split(regex)[rawdata_columns - 1];
                // 只保留 forecast           
                if(!"forecast".equals(type))continue;
                // 找出關連性
                // (1) 找出 XXXcontent table 的 rawdata
                String lat = s.split(regex)[8];
                String lon = s.split(regex)[9];
                String min_pres = s.split(regex)[10];
                String wind = s.split(regex)[11];
                String wind_unit = s.split(regex)[12];
                String valid_time = s.split(regex)[7];
                String valid_hour = s.split(regex)[13];
                // (2) 關連 tyinfo table 的 rawdata
                ty_name_en = s.split(regex)[0];
                ty_number = s.split(regex)[5];      
                String putTyInfoSQL = "SELECT ty_info_id FROM TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(putTyInfoSQL);
                prepStmt.setString(1, ty_name_en);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                }
                // (3) 關連 centre table 的 rawdata
                centre = s.split(regex)[1];
                model = s.split(regex)[2];
                resolution = s.split(regex)[rawdata_columns - 4];
                geopotential_height = s.split(regex)[rawdata_columns - 3];
                data_source = s.split(regex)[rawdata_columns - 2];
                String putCentreInfoSQL = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                prepStmt = conn.prepareStatement(putCentreInfoSQL);
                prepStmt.setString(1, centre);
                prepStmt.setString(2, model);
                prepStmt.setString(3, resolution);
                prepStmt.setString(4, geopotential_height);
                prepStmt.setString(5, data_source);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                }
                // (4) 關連 XXXinfo table 的 rawdata
                base_time = s.split(regex)[6];
                String putXXXInfoSQL = "SELECT f_info_id FROM ForecastInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    f_info_id = rs.getString(1);
                }
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO ForecastContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, f_info_id);
                prepStmt.setString(2, lat);
                prepStmt.setString(3, lon);
                prepStmt.setString(4, min_pres);
                prepStmt.setString(5, wind);
                prepStmt.setString(6, wind_unit);
                prepStmt.setString(7, valid_time);                
                prepStmt.setString(8, valid_hour);
                prepStmt.executeUpdate();
            }
            /* ^^^^^^^^^^^^ 改預報場 Content Table 的 code  ^^^^^^^^^^^^ */
            /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
            //</editor-fold>
/********** 系集場 **********/          
/********** 系集場 **********/
            for (String s : hs_ensbInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                ty_info_id = s.split(regex)[0];
                centre_info_id = s.split(regex)[1];
                base_time = s.split(regex)[2];
                member = s.split(regex)[3];
                // 如果不存在，新建各 Type Info (用 SQL 語法去找存不存在)
                insertSQL = "";                
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM EnsembleInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ? AND member = ?";               
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                prepStmt.setString(4, member);                
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的各 Type Info 才更新(value == 0 代表不存在，select 不到)
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO EnsembleInfo VALUES (?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO EnsembleInfo VALUES (0,?,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_info_id);
                        prepStmt.setString(2, centre_info_id);
                        prepStmt.setString(3, base_time);
                        prepStmt.setString(4, member); 
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            /* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
            /* vvvvvvvvvvvv 改系集場 Content Table 的 code  vvvvvvvvvvvv */
            for (String s : al_rawdata) {
                outputException = "exception: " + s;// 找錯用
                int rawdata_columns = s.split(regex).length;
                String type = s.split(regex)[rawdata_columns - 1];
                // 只保留 ensemble           
                if(!"ensembleForecast".equals(type))continue;
                // 找出關連性
                // (1) 找出 XXXcontent table 的 rawdata
                String lat = s.split(regex)[8];
                String lon = s.split(regex)[9];
                String min_pres = s.split(regex)[10];
                String wind = s.split(regex)[11];
                String wind_unit = s.split(regex)[12];
                String valid_time = s.split(regex)[7];
                String valid_hour = s.split(regex)[13];
                // (2) 關連 tyinfo table 的 rawdata
                ty_name_en = s.split(regex)[0];
                ty_number = s.split(regex)[5];      
                String putTyInfoSQL = "SELECT ty_info_id FROM TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(putTyInfoSQL);
                prepStmt.setString(1, ty_name_en);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                    
                }
                // (3) 關連 centre table 的 rawdata
                centre = s.split(regex)[1];
                model = s.split(regex)[2];
                resolution = s.split(regex)[rawdata_columns - 4];
                geopotential_height = s.split(regex)[rawdata_columns - 3];
                data_source = s.split(regex)[rawdata_columns - 2];
                String putCentreInfoSQL = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                prepStmt = conn.prepareStatement(putCentreInfoSQL);
                prepStmt.setString(1, centre);
                prepStmt.setString(2, model);
                prepStmt.setString(3, resolution);
                prepStmt.setString(4, geopotential_height);
                prepStmt.setString(5, data_source);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                }
                // (4) 關連 XXXinfo table 的 rawdata
                base_time = s.split(regex)[6];
                valid_time = s.split(regex)[7];
                member = s.split(regex)[14];
                String putXXXInfoSQL = "SELECT e_info_id FROM EnsembleInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ? AND member = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                prepStmt.setString(4, member);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    e_info_id = rs.getString(1);
                }
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO EnsembleContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, e_info_id);
                prepStmt.setString(2, lat);
                prepStmt.setString(3, lon);
                prepStmt.setString(4, min_pres);
                prepStmt.setString(5, wind);
                prepStmt.setString(6, wind_unit);
                prepStmt.setString(7, valid_time);
                prepStmt.setString(8, valid_hour);
                prepStmt.executeUpdate();
            }
            /* ^^^^^^^^^^^^ 改系集場 Content Table 的 code  ^^^^^^^^^^^^ */
            /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
            //</editor-fold>
/********** 最佳路徑場 **********/          
/********** 最佳路徑場 **********/
            for (String s : hs_bstkInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                // 從文字檔下手，比較資料庫的狀況
                ty_info_id = s.split(regex)[0];
                centre_info_id = s.split(regex)[1];
//                System.out.println("centre_info_id = " + centre_info_id);
                base_time = s.split(regex)[2];
                // 如果不存在，新建各 Type Info (用 SQL 語法去找存不存在)
                insertSQL = "";
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM BestTrackInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的各 Type Info 才更新(value == 0 代表不存在，select 不到)        
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO BestTrackInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO BestTrackInfo VALUES (0,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_info_id);
                        prepStmt.setString(2, centre_info_id);
//                        System.out.println(
//                                String.format(
//                                "ty_info_id = %s, centre_info_id = %s, base_time = %s", 
//                                        ty_info_id, centre_info_id, base_time));

                        prepStmt.setString(3, base_time);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            // 從 rawdata 匯入 detail table
            /* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
            /* vvvvvvvvvvvv 改最佳路徑官方場 Content Table 的 code  vvvvvvvvvvvv */
            for (String s : al_rawdata) {
                outputException = "exception: " + s;// 找錯用
                int rawdata_columns = s.split(regex).length;
                String type = s.split(regex)[rawdata_columns - 1];
                // 只保留 besttrack
                if(!"besttrack".equals(type))continue;
                // 找出關連性
                // (1) 找出 XXXcontent table 的 rawdata
                String lat = s.split(regex)[8];
                String lon = s.split(regex)[9];
                String min_pres = s.split(regex)[10];
                String wind = s.split(regex)[11];
                String wind_unit = s.split(regex)[12];
                String development = s.split(regex)[13];
                String data_type = s.split(regex)[14];
                // (2) 關連 tyinfo table 的 rawdata
                ty_name_en = s.split(regex)[0];
                ty_number = s.split(regex)[5];
                String putTyInfoSQL = "SELECT ty_info_id FROM TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(putTyInfoSQL);
                prepStmt.setString(1, ty_name_en);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                }
                // (3) 關連 centre table 的 rawdata
                centre = s.split(regex)[1];
                model = s.split(regex)[2];
                resolution = s.split(regex)[rawdata_columns - 4];
                geopotential_height = s.split(regex)[rawdata_columns - 3];
                data_source = s.split(regex)[rawdata_columns - 2];
                String putCentreInfoSQL = "SELECT centre_info_id FROM CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                prepStmt = conn.prepareStatement(putCentreInfoSQL);
                prepStmt.setString(1, centre);
                prepStmt.setString(2, model);
                prepStmt.setString(3, resolution);
                prepStmt.setString(4, geopotential_height);
                prepStmt.setString(5, data_source);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                }
                // (4) 關連 XXXinfo table 的 rawdata
                base_time = s.split(regex)[6];
                String putXXXInfoSQL = "SELECT bt_info_id FROM BestTrackInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    bt_info_id = rs.getString(1);
                }
//                System.out.println(String.format("ty_info_id = %s, centre_info_id = %s, bt_info_id = %s", ty_info_id, centre_info_id, bt_info_id));
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO BestTrackContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, bt_info_id);
                prepStmt.setString(2, lat);
                prepStmt.setString(3, lon);
                prepStmt.setString(4, min_pres);
                prepStmt.setString(5, wind);
                prepStmt.setString(6, wind_unit);
                prepStmt.setString(7, development);
                prepStmt.setString(8, data_type);
                prepStmt.executeUpdate();
            }
            /* ^^^^^^^^^^^^ 改最佳路徑官方場 Content Table 的 code  ^^^^^^^^^^^^ */
            /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
            //</editor-fold>
        } catch (SQLException ex) {
            System.out.println(outputException);
            ex.printStackTrace();
        } finally {
            /**
             * 測試2 處理 java.lang.OutOfMemoryError: Java heap space，清理這些物件
             */
            hs_anlyInfo.clear();
            hs_foreInfo.clear();
            hs_ensbInfo.clear();
            hs_bstkInfo.clear();
            al_rawdata.clear();
        }
    }//</editor-fold>
}
