//<editor-fold defaultstate="collapsed" desc="import...">
import app.db.DBSetting;
import app.filesize.FileInfomation;
import app.filetree.FV;
import app.filetree.WalkFileTree;
import app.itf.ITF_DB;
import app.typhoon.TyphoonList;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weather.Adjust;
//</editor-fold>

/**
 * 練習正規化:颱風資料
 * @author 1312032
 */
public class NF_TyphoonList extends DBSetting implements ITF_DB {

    private Connection conn;
    private String db_type, whereToDownload, regex;
    private Adjust adjust;
    private TyphoonList ty_list;
    private WalkFileTree w_file;
    private PreparedStatement prepStmt;
    private ResultSet rs;
    private HashSet<String> hs_tyInfo;
    private HashSet<String> hs_cetreInfo;
    private HashSet<String> hs_analyInfo;
    private HashSet<String> hs_foreInfo;
    private HashSet<String> hs_ensbInfo;
    private ArrayList<String> al_rawdata;
    
    /**
     * 初始設定
     */
    public NF_TyphoonList() {//<editor-fold defaultstate="collapsed" desc="...">
        // 分格符號
        regex = ",";
        // 資料庫
        db_type = getReturnValue("dbType");
        conn = getConn();
        hs_tyInfo = new HashSet<>();
        hs_cetreInfo = new HashSet<>();
        hs_analyInfo = new HashSet<>();
        hs_foreInfo = new HashSet<>();
        hs_ensbInfo = new HashSet<>();
        // 原始資料
        al_rawdata = new ArrayList<>();
        // 其他設定
        adjust = new Adjust("yyyy-MM-dd HH:mm:ss");// 時間格式
        ty_list = new TyphoonList();
        w_file = new WalkFileTree();// 走訪目錄
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(tywebProp));
            whereToDownload = prop.getProperty("download_dir_path");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 下載 tigge XML (目前隱性建議，用 shell 下載或許較好)
     * @param timeout 緩衝時間
     */
    public void downloadTigge(int timeout) {//<editor-fold defaultstate="collapsed" desc="...">
        String[] centre = {"CMA", "ECMWF", "JMA", "KMA", "MSC", "NCEP", /*"STI",*/ "UKMO"};
        try {
            // 以下區域是設定變數
            String yymmdd, createDirPath;
            Document doc_lev1, doc_lev2;
            URL wantToDownloadURL;
            File makeDownloadDir, createFile, deleteFile;
            Properties prop = new Properties();
            // 以上區域是設定變數
            for (int i = 0; i < centre.length; i++) {
                String tiggeURL = prop.getProperty("tigge_url") + centre[i];
                doc_lev1 = Jsoup.connect(tiggeURL).timeout(timeout).get();
                Elements elms_a_lev1 = doc_lev1.select("a");
                int rows = 0;
                for (Element elm_a_lev1 : elms_a_lev1) {
                    if(rows >= 5){
                        // 建資料夾用
                        yymmdd = elm_a_lev1.text();
                        doc_lev2 = Jsoup.connect(elm_a_lev1.absUrl("href")).timeout(timeout).get();
                        Elements elms_a_lev2 = doc_lev2.select("a");
                        for (Element elm_a_lev2 : elms_a_lev2) {
                            String xmlURL = elm_a_lev2.absUrl("href");
                            if(xmlURL.contains("_tigge_")){
                                // 取出檔案名稱
                                String xmlFile = elm_a_lev2.text();
                                createDirPath = String.format("%s%s/%s", whereToDownload, centre[i], yymmdd);
                                wantToDownloadURL = new URL(xmlURL);
                                // 建立資料夾，不知道有沒有強過老方法 createDirectory.mkdirs();
                                makeDownloadDir = new File(createDirPath);
                                FileUtils.forceMkdir(makeDownloadDir);
                                createFile = new File(createDirPath + xmlFile);
                                // 下載網頁上檔案
                                FileUtils.copyURLToFile(wantToDownloadURL, createFile);
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
     * 解析 tigge XML
     * @param url 檔案路徑
     */
    public void parseTigge(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            Path path = Paths.get(whereToDownload);
            Files.walkFileTree(path, w_file.findFile());
            // 以下區域是設定變數
            String centre;
            // 以上區域是設定變數
            System.out.println(url);
            Path abs_path = Paths.get(url);
            centre = abs_path.getName(2).toString();
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
            //<editor-fold defaultstate="collapsed" desc="...">
            String product = doc.select("product").text();
            String raw_ProductionCenter = doc.select("productionCenter").text();
            // 專門處理 Met
            String productionCenter = "";
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
            String dataSource = "tigge";
            for(Element elmData : elmsData){
                String type = elmData.attr("type");
                if(baseTime.isEmpty())break;
                if("analysis".equals(type)){//<editor-fold defaultstate="collapsed" desc="...">
                    // 處理分析場
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
                                String validTime_Year = raw_ValidTime.substring(0 , 4);
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
                                String min_pressure = elmFix.select("cycloneData").select("minimumPressure").select("min_pressure").text();
                                String speed = elmFix.select("cycloneData").select("maximumWind").select("speed").text();
                                String spdUnits = elmFix.select("cycloneData").select("maximumWind").select("speed").attr("units").toLowerCase();
                                if(!typhoonName.isEmpty()){
                                    int getCWBTyphoonNumber = ty_list.getCWBTyNumber(typhoonName , Integer.parseInt(baseTime.substring(0 , 4)));
                                    if(typhoonBasin.isEmpty())typhoonBasin = "";
                                    if(typhoonLocalID.isEmpty())typhoonLocalID = "";
                                    if(typhoonNumber.isEmpty())typhoonNumber = "";
                                    if(development.isEmpty())development = "";
                                    if(min_pressure.isEmpty() || min_pressure.contains("*"))min_pressure = "-999";
                                    if(speed.equals("0") || speed.isEmpty() || speed.contains("*")){
                                        speed = "-999.0";
                                        spdUnits = "";
                                    }
                                    if(spdUnits.isEmpty())spdUnits = "";
                                    String import_tyInfo = String.format("%s,%s,%s",
                                        typhoonName, validTime_Year, getCWBTyphoonNumber);
                                    String import_cetrInfo = String.format("%s,%s,,-999,%s",
                                        productionCenter, applicationType, dataSource);
                                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                        typhoonName, productionCenter, applicationType, typhoonLocalID, typhoonBasin, getCWBTyphoonNumber,
                                        baseTime, validTime, lat, lon, min_pressure, speed, spdUnits, development, 
                                        analysisType, dataSource, type);
                                    hs_tyInfo.add(import_tyInfo);
                                    hs_cetreInfo.add(import_cetrInfo);
                                    al_rawdata.add(import_rawdata);                                          
                                }
                            }
                        }
                    }//</editor-fold>
                }else{
                    // 處理模式場
                    //<editor-fold defaultstate="collapsed" desc="...">
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
                                typhoonNumber = elmDisturbance.select("cycloneNumber").text();
                                if(elmDisturbance.select("cycloneNumber").text().length() == 1){
                                    typhoonNumber = "0" + elmDisturbance.select("cycloneNumber").text();
                                }else{
                                    typhoonNumber = elmDisturbance.select("cycloneNumber").text();
                                }
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0 , 4) + typhoonNumber );
                            }else if(ty_list.isEnglishNumber(typhoonName)){
                                // 颱風名稱是英文數字轉為數字名稱 + W (one  --> 01W)
                                String raw_typhoonName = typhoonName;
                                // 英人數字轉成阿拉伯數字 + W
                                typhoonNumber = ty_list.numberChange(raw_typhoonName) + "";
                                if(typhoonNumber.length() == 1){
                                    typhoonNumber = "0" + typhoonNumber;
                                }  
                                typhoonName = typhoonNumber + "W";
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0 , 4) + typhoonNumber );
                            }else if(typhoonName.matches("[0-9][0-9].*")){
                                typhoonNumber = typhoonName.substring(0 , 2);
                                getCWBTyphoonNumber = Integer.parseInt( baseTime.substring(0 , 4) + typhoonNumber );
                            }else{
                                getCWBTyphoonNumber = ty_list.getCWBTyNumber(typhoonName , Integer.parseInt(baseTime.substring(0 , 4)));
                            }
                            Elements elmsFix = elmDisturbance.select("fix");
//                                if(elmsFix.isEmpty())break;//開啟這個會讀不到該 ensembleForecast 資料
                            if(elmsFix.isEmpty())continue;
                            // 防止沒有分區域 "".equals(typhoonBasin)
                            for(Element elmFix : elmsFix){
                                String fcstHour = elmFix.attr("hour");
                                String raw_ValidTime = elmFix.select("validTime").text();
                                String validTime_Year = raw_ValidTime.substring(0 , 4);
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
                                String min_pressure = elmFix.select("cycloneData").select("minimumPressure").select("pressure").text();
                                if(min_pressure.contains("**"))min_pressure = "-999";
                                String raw_Speed = elmFix.select("cycloneData").select("maximumWind").select("speed").text();
                                String speed = raw_Speed;
                                String spdUnits = elmFix.select("cycloneData").select("maximumWind").select("speed").attr("units").toLowerCase();
                                String typhoonNumber_New;
                                String geopotentialHeight = "";
                                if(!typhoonName.isEmpty()){
                                    if(typhoonBasin.isEmpty())typhoonBasin = "";
                                    if(typhoonLocalID.isEmpty())typhoonLocalID = "";
                                    if(min_pressure.isEmpty() || min_pressure.contains("*"))min_pressure = "-999";
                                    if(speed.equals("0") || speed.isEmpty() || speed.contains("*")){
                                        speed = "-999.0";
                                        spdUnits = "";
                                    }
                                    if(spdUnits.isEmpty())spdUnits = "";
                                    if(geopotentialHeight.isEmpty())geopotentialHeight = "-999";//</editor-fold>
                                    if(type.equals("forecast")){//<editor-fold defaultstate="collapsed" desc="...">
                                        // 預報 
                                        String import_tyInfo = String.format("%s,%s,%s",
                                            typhoonName, validTime_Year, getCWBTyphoonNumber);
                                        String import_cetrInfo = String.format("%s,%s,%s,-999,%s",
                                            productionCenter, applicationType, modelResolution, dataSource);
                                        String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                            typhoonName,productionCenter,applicationType,typhoonLocalID,typhoonBasin,getCWBTyphoonNumber,
                                            baseTime,validTime,lat,lon,min_pressure,speed,spdUnits,
                                            fcstHour,modelResolution,dataSource,type);
                                        hs_tyInfo.add(import_tyInfo);
                                        hs_cetreInfo.add(import_cetrInfo);
                                        al_rawdata.add(import_rawdata);//</editor-fold>                
                                    }else if(type.equals("ensembleForecast")){//<editor-fold defaultstate="collapsed" desc="...">
                                        // 系集
                                        String import_tyInfo = String.format("%s,%s,%s",
                                            typhoonName, validTime_Year, getCWBTyphoonNumber);
                                        String import_cetrInfo = String.format("%s,%s,%s,%s,%s",
                                            productionCenter, applicationType, modelResolution, geopotentialHeight, dataSource);
                                        String import_rawdata = String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                            typhoonName,productionCenter,applicationType,typhoonLocalID,typhoonBasin,getCWBTyphoonNumber,
                                            baseTime,validTime,lat,lon,min_pressure,speed,spdUnits,
                                            fcstHour,member,modelResolution,geopotentialHeight,dataSource,type);
                                        hs_tyInfo.add(import_tyInfo);
                                        hs_cetreInfo.add(import_cetrInfo);
                                        al_rawdata.add(import_rawdata);                                             
                                    }//</editor-fold>
                                }
                            }
                        } 
                    }
                }
            }//</editor-fold>
            // 以上直接引用原始寫法好
            // 以上直接引用原始寫法好
            /********************** 會用到的物件 **********************/
            String judgeExistSQL, insertSQL, updateSQL, selectSQL; 
            /********************** 建立颱風清單 **********************/
            String ty_name, ty_year, ty_number;
            for (String s : hs_tyInfo) {
                ty_name = s.split(regex)[0];
                ty_year = s.split(regex)[1];
                ty_number = s.split(regex)[2];
                judgeExistSQL = "SELECT COUNT(*) FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_year = ? AND ty_number = ?";
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
                            insertSQL = "INSERT INTO Ty_TyphoonInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_TyphoonInfo VALUES (0,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_name);
                        prepStmt.setString(2, ty_year);
                        prepStmt.setString(3, ty_number);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            /********************** 建立單位清單 **********************/
            String model, resolution, gh, data_source;
            for (String s : hs_cetreInfo) {
                // centre 上面已出現過，直接使用就好
                model = s.split(regex)[1];
                resolution = s.split(regex)[2];
                gh = s.split(regex)[3];
                data_source = s.split(regex)[4];
                judgeExistSQL = 
                    "SELECT COUNT(*) FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
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
                            insertSQL = "INSERT INTO Ty_CentreInfo VALUES (?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_CentreInfo VALUES (0,?,?,?,?,?)";// MySQL
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
            }
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 正規化颱風清單和單位清單
     */
    public void setNameAndCentre() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            /********************** 會用到的物件 **********************/
            String judgeExistSQL, insertSQL, updateSQL, selectSQL; 
            /********************** 建立颱風清單 **********************/
            String ty_name, ty_year, ty_number;
            for (String s : hs_tyInfo) {
                ty_name = s.split(regex)[0];
                ty_year = s.split(regex)[1];
                ty_number = s.split(regex)[2];
                judgeExistSQL = "SELECT COUNT(*) FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_year = ? AND ty_number = ?";
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
                            insertSQL = "INSERT INTO Ty_TyphoonInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_TyphoonInfo VALUES (0,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, ty_name);
                        prepStmt.setString(2, ty_year);
                        prepStmt.setString(3, ty_number);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            /********************** 建立單位清單 **********************/
            String centre, model, resolution, gh, data_source;
            for (String s : hs_cetreInfo) {
                // centre 上面已出現過，直接使用就好
                centre = s.split(regex)[0];
                model = s.split(regex)[1];
                resolution = s.split(regex)[2];
                gh = s.split(regex)[3];
                data_source = s.split(regex)[4];
                judgeExistSQL = 
                    "SELECT COUNT(*) FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
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
                            insertSQL = "INSERT INTO Ty_CentreInfo VALUES (?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_CentreInfo VALUES (0,?,?,?,?,?)";// MySQL
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
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 正規化 Info
     */
    public void setXXXInfo() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            /********************** 準備各 Type Info 資料 **********************/
            // 原則上上面沒出現過的變數，就和資料庫一樣就好
            String establishFK_ty, establishFK_cetr;
            String ty_info_id, centre_info_id, base_time, type;
            for (String s : al_rawdata) {
                int rawdata_columns = s.split(regex).length;
                ty_info_id = "";
                centre_info_id = "";
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
                establishFK_ty = "SELECT ty_info_id FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
                prepStmt = conn.prepareStatement(establishFK_ty);
                prepStmt.setString(1, ty_name);
                prepStmt.setString(2, ty_number);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    ty_info_id = rs.getString(1);
                }
                // 考慮不同狀態，要考慮的欄位不一樣
                if("analysis".equals(type)){
                    // 用單位、模式、來源判斷
                    establishFK_cetr = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND data_source = ?";
                    prepStmt = conn.prepareStatement(establishFK_cetr);
                    prepStmt.setString(1, centre);
                    prepStmt.setString(2, model);
                    prepStmt.setString(3, data_source); 
                }else if("forecast".equals(type)){
                    // 用單位、模式、來源判斷
                    establishFK_cetr = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND data_source = ?";
                    prepStmt = conn.prepareStatement(establishFK_cetr);
                    prepStmt.setString(1, centre);
                    prepStmt.setString(2, model);
                    prepStmt.setString(3, data_source);
                }else if("ensembleForecast".equals(type)){
                    // 用單位、模式、來源判斷、解析度、高度
                    String resolution = s.split(regex)[rawdata_columns - 4];
                    String gh = s.split(regex)[rawdata_columns - 3];
                    establishFK_cetr = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
                    prepStmt = conn.prepareStatement(establishFK_cetr);
                    prepStmt.setString(1, centre);
                    prepStmt.setString(2, model);
                    prepStmt.setString(3, resolution);
                    prepStmt.setString(4, gh);
                    prepStmt.setString(5, data_source);  
                }                    
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    centre_info_id = rs.getString(1);
                    // 匯入各 Type Info 資訊
                    // 放外面會使 centre_info_id = null
                    if("analysis".equals(type)){
                        String import_analyInfo = String.format("%s,%s,%s", ty_info_id, centre_info_id, base_time);
                        hs_analyInfo.add(import_analyInfo);
                    }else if("forecast".equals(type)){
//                        String valid_time = s.split(regex)[7];
//                        String import_foreInfo = String.format("%s,%s,%s,%s", ty_info_id, centre_info_id, base_time, valid_time);
                        String import_foreInfo = String.format("%s,%s,%s", ty_info_id, centre_info_id, base_time);
                        hs_foreInfo.add(import_foreInfo);
                    }else if("ensembleForecast".equals(type)){
//                        String valid_time = s.split(regex)[7];
                        String member = s.split(regex)[14];
//                        String import_ensbInfo = String.format("%s,%s,%s,%s,%s", ty_info_id, centre_info_id, base_time, valid_time, member);
                        String import_ensbInfo = String.format("%s,%s,%s,%s", ty_info_id, centre_info_id, base_time, member);
                        hs_ensbInfo.add(import_ensbInfo);
                    }   
                }    
            } 
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 正規化 Content
     */
    public void setXXXContent() {//<editor-fold defaultstate="collapsed" desc="...">
        String ty_name_en = "", ty_year = "", ty_number = "";
        String centre = "", model = "", resolution = "", geopotential_height = "", data_source = "";
        String a_info_id = "", f_info_id = "", e_info_id = "";
        String ty_info_id = "", centre_info_id = "", base_time = "", member = "";
        String insertSQL = "", judgeExistSQL = "";
        int value;
        ArrayList<String> al_deleteTemp = new ArrayList<>();
        try {
            // 應該不用標籤，因為每個都要跑過一次
/********** 分析場 **********/            
/********** 分析場 **********/
            for (String s : hs_analyInfo) {//<editor-fold defaultstate="collapsed" desc="...">
                // 從文字檔下手，比較資料庫的狀況
                ty_info_id = s.split(regex)[0];
                centre_info_id = s.split(regex)[1];
                base_time = s.split(regex)[2];
                // 如果不存在，新建各 Type Info (用 SQL 語法去找存不存在)
                insertSQL = "";
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM Ty_AnalysisInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
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
                            insertSQL = "INSERT INTO Ty_AnalysisInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) | "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_AnalysisInfo VALUES (0,?,?,?)";// MySQL
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
            // 必放，才不會影響到後面
//            prepStmt.clearParameters();
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
                String putTyInfoSQL = "SELECT ty_info_id FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
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
                String putCentreInfoSQL = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
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
                String putXXXInfoSQL = "SELECT a_info_id FROM Ty_AnalysisInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    a_info_id = rs.getString(1);
                }
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO Ty_AnalysisContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
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
            // 必放，才不會影響下面的 prepStmt (重複匯入)
//            prepStmt.clearParameters();
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
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM Ty_ForecastInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";               
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
                            insertSQL = "INSERT INTO Ty_ForecastInfo VALUES (?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) | "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_ForecastInfo VALUES (0,?,?,?)";// MySQL
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
            // 必放，才不會影響下面的 prepStmt (重複匯入)
//            prepStmt.clearParameters();
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
                String putTyInfoSQL = "SELECT ty_info_id FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
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
                String putCentreInfoSQL = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
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
                String putXXXInfoSQL = "SELECT f_info_id FROM Ty_ForecastInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ?";
                prepStmt = conn.prepareStatement(putXXXInfoSQL);
                prepStmt.setString(1, ty_info_id);
                prepStmt.setString(2, centre_info_id);
                prepStmt.setString(3, base_time);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    f_info_id = rs.getString(1);
                }
                // 匯入 XXXContent 
                insertSQL = "INSERT INTO Ty_ForecastContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, f_info_id);
                prepStmt.setString(2, lat);
                prepStmt.setString(3, lon);
                prepStmt.setString(4, min_pres);
                prepStmt.setString(5, wind);
                prepStmt.setString(6, wind_unit);
                prepStmt.setString(7, valid_time);                
                prepStmt.setString(8, valid_hour);
//                System.out.println(f_info_id + "," + lat + "," + lon + "," + min_pres);
                prepStmt.executeUpdate();
            }
            // 必放，才不會影響下面的 prepStmt (重複匯入)
//            prepStmt.clearParameters();
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
                judgeExistSQL = "SELECT COUNT(ty_info_id) FROM Ty_EnsembleInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ? AND member = ?";               
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
                            insertSQL = "INSERT INTO Ty_EnsembleInfo VALUES (?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) | "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO Ty_EnsembleInfo VALUES (0,?,?,?,?)";// MySQL
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
            // 必放，才不會影響下面的 prepStmt (重複匯入)
//            prepStmt.clearParameters();
            /* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
            /* vvvvvvvvvvvv 改系集場 Content Table 的 code  vvvvvvvvvvvv */
            for (String s : al_rawdata) {
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
                String putTyInfoSQL = "SELECT ty_info_id FROM Ty_TyphoonInfo WHERE ty_name_en = ? AND ty_number = ?";
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
                String putCentreInfoSQL = "SELECT centre_info_id FROM Ty_CentreInfo WHERE centre = ? AND model = ? AND resolution = ? AND geopotential_height = ? AND data_source = ?";
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
                String putXXXInfoSQL = "SELECT e_info_id FROM Ty_EnsembleInfo WHERE ty_info_id = ? AND centre_info_id = ? AND base_time = ? AND member = ?";
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
                insertSQL = "INSERT INTO Ty_EnsembleContent VALUES (0,?,?,?,?,?,?,?,?)";// MySQL
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
            // 必放，才不會影響下面的 prepStmt (重複匯入)
//            prepStmt.clearParameters();
            /* ^^^^^^^^^^^^ 改系集場 ontent Table 的 code  ^^^^^^^^^^^^ */
            /* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */
            //</editor-fold>
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            al_rawdata.clear();
        }
    }//</editor-fold>
    
    public static void main(String[] args) {
        // 啟動各功能
        NF_TyphoonList nf = new NF_TyphoonList();// 主要專案
        FV fv = new FV();// 走訪目錄
        TyphoonList tl = new TyphoonList();// 下載颱風清單
        FileInfomation fi = new FileInfomation();// 判斷檔案資訊
        /****************** 以下開始執行 ******************/
        tl.getCWBTyListToHtml();
        // 下載
//        nf.downloadTigge(15000);
        for (Object url : fv.getTyphoonPath()) {
            // 檔案大小為 0，直接跳過
            if(fi.getByte((String) url) == 0)continue;
            // 解析資料
            nf.parseTigge((String) url);
            // 建立颱風、單位清單
            nf.setNameAndCentre();
            // 建立 info
            nf.setXXXInfo();
            // 匯入 content
            nf.setXXXContent();            
        }
    }
}
