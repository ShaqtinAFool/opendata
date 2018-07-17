package app.station;

//<editor-fold defaultstate="collapsed" desc="...">
import app.db.DBSetting;
import app.excptn.StnIdINotFoundException;
import app.webmodel.HtmlUnit;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import osi.presentation.SSL;
import weather.Adjust;
//</editor-fold>

/**
 * 解析雨量資料
 */
public class StationData extends DBSetting {

    private final Connection conn;
    private Properties prop;
    private final ArrayList<String> al_rawdata;
    // 映射:可參考 http://jax-work-archive.blogspot.tw/2015/02/java-setlistmap.html
    private final HashMap<String, String> hm_stnAddress, hm_county, hm_stn;
    private HashSet<String> hs_stnInfo;
    private final String db_type;
    private Adjust adjust;
    private BufferedReader br;
    private SSL ssl;

    /**
     *
     * @param dbEnum
     */
    public StationData(Enum dbEnum) {//<editor-fold defaultstate="collapsed" desc="...">
        // 必寫
        super(dbEnum);
        // 資料庫
        prop = new Properties();
        db_type = getReturnValue("dbType");
        conn = getConn();
        // 原始資料
        al_rawdata = new ArrayList<>();
        hm_stnAddress = new HashMap<>();
        hm_county = new HashMap<>();
        hm_stn = new HashMap<>();
        hs_stnInfo = new HashSet<>();
        // 時間格式
        adjust = new Adjust();
        // 啟動 ssl
        ssl = new SSL();
        // 將測站地址放到 List 裡面
        setStnAddress();
    }//</editor-fold>

    /**
     * 取得測站地址，目前已收集 CWB, WRA, CAA
     */
    private void setStnAddress() {//<editor-fold defaultstate="collapsed" desc="...">
//        String[] stnType = {"CWB", "WRA", "CAA"};
        String[] stnType = {"CAA"/*, "AF"*/};
        String openDataURL, whereToDownload, fileName;
        URL wantToDownloadURL;
        File makeDownloadDir, createFile, deleteFile;
        try {
            prop.load(new FileReader(stnwebProp));
            for (String s : stnType) {
                if(null != s)
                switch (s) {
                    case "CWB":
                        //<editor-fold defaultstate="collapsed" desc="...">
                        openDataURL = prop.getProperty("cwb_station_list");
                        whereToDownload = prop.getProperty("cwb_temp_file_path");
                        fileName = prop.getProperty("cwb_temp_file_name");
                        // 堅持先下載 XML 再解析，不然直接線上解析會太多突發狀況，例如欄位空值...等等
                        wantToDownloadURL = new URL(openDataURL);
                        // 建立資料夾，不知道有沒有強過老方法 createDirectory.mkdirs();
                        makeDownloadDir = new File(whereToDownload);
                        FileUtils.forceMkdir(makeDownloadDir);
                        createFile = new File(whereToDownload + fileName);
                        // 下載網頁上檔案
                        FileUtils.copyURLToFile(wantToDownloadURL, createFile);
                        Document doc = Jsoup.parse(new File(whereToDownload + fileName), "BIG5");
                        Elements elms_table = doc.select("table.MsoNormalTable");
                        for (Element elm_table : elms_table) {
                            Elements elms_tr = elm_table.select("tr");
                            String row_content;
                            for (Element elm_tr : elms_tr) {
                                row_content = elm_tr.text();
                                if(row_content.matches("^[C4].*")){
                                    // 篩選出測站資訊
                                    Elements elms_td = elm_tr.select("td");
                                    String obsId = elms_td.get(0).text();
                                    String locAddress = elms_td.get(6).text();
//                                    System.out.println(obsId + "," + locAddress);
                                    hm_stnAddress.put(obsId, locAddress);
                                }
                            }
                        }//</editor-fold>
                        break;
                    case "WRA":
                        //<editor-fold defaultstate="collapsed" desc="...">
                        openDataURL = prop.getProperty("wra_station_list");
                        whereToDownload = prop.getProperty("wra_temp_file_path");
                        fileName = prop.getProperty("wra_temp_file_name");
                        // 堅持先下載 XML 再解析，不然直接線上解析會太多突發狀況，例如欄位空值...等等
                        wantToDownloadURL = new URL(openDataURL);
                        // 建立資料夾，不知道有沒有強過老方法 createDirectory.mkdirs();
                        makeDownloadDir = new File(whereToDownload);
                        FileUtils.forceMkdir(makeDownloadDir);
                        createFile = new File(whereToDownload + fileName);
                        // 下載網頁上檔案
                        FileUtils.copyURLToFile(wantToDownloadURL, createFile);
                        /********************** 解析原始資料 **********************/
                        BufferedReader br = new BufferedReader(new FileReader(createFile));
                        String content = "";
                        // 讀取 JSON 檔案
                        while(br.ready()){
                            content += br.readLine();
                        }   br.close();
                        // 解析 JSON 檔案，等同 Jsop Document
                        JSONObject json_obj = new JSONObject(content);
                        // JSONArray = Jsoup 第一層 Element
                        JSONArray json_ary = json_obj.getJSONArray("WaterResourcesAgencyRainfallStationsProfile_OPENDATA");
                        // 從 JSON 的 key 取出 value
                        for (int i = 0; i < json_ary.length(); i++) {
                            // 取得 JSONArray 的物件轉成 JSONObject，再轉成字串
                            String obsId = json_ary.getJSONObject(i).get("ObservatoryIdentifier").toString().trim();
                            String locAddress = json_ary.getJSONObject(i).get("LocationAddress").toString().trim();
                            hm_stnAddress.put(obsId, locAddress);
                        }
                        // 刪除檔案
//                    createFile.delete();
                        //</editor-fold>
                        break;
                    case "CAA":
                        //<editor-fold defaultstate="collapsed" desc="...">
                        hm_stnAddress.put("46686", "25.081,121.226,桃園航空氣象臺,桃園機場,桃園市,大園區,55.7");
                        hm_stnAddress.put("46696", "25.072,121.553,松山航空氣象臺,松山機場,台北市,松山區,15.0");
                        hm_stnAddress.put("46740", "22.573,120.349,高雄航空氣象臺,小港機場,高雄市,小港區,11.7");
                        hm_stnAddress.put("46738", "22.757,121.093,豐年航空氣象臺,豐年機場,台東縣,台東市,57.0");
                        hm_stnAddress.put("46787", "22.029,121.527,蘭嶼航空氣象臺,蘭嶼機場,台東縣,蘭嶼鄉,13.4");
                        hm_stnAddress.put("46786", "22.674,121.458,綠島航空氣象臺,綠島機場,台東縣,綠島鄉,8.5");
                        hm_stnAddress.put("46788", "26.227,119.999,北竿航空氣象臺,北竿機場,連江縣,北竿鄉,12.5");
                        hm_stnAddress.put("46736", "24.432,118.357,金門航空氣象臺,尚義機場,金門縣,金湖鎮,40.2");
                        hm_stnAddress.put("46789", "26.161,119.957,南竿航空氣象臺,南竿機場,連江縣,南竿鄉,70.7");
                        hm_stnAddress.put("46790", "22.041,120.730,恆春航空氣象臺,恆春機場,屏東縣,恆春鎮,14.0");
                        //</editor-fold>
                        break;
                    case "AF":
                        //<editor-fold defaultstate="collapsed" desc="...">
                        hm_stnAddress.put("NN", "緯度,經度,台南,,台南市,XX區,高度");
                        hm_stnAddress.put("LC", "緯度,經度,龍勤,,台南市,XX區,高度");
                        hm_stnAddress.put("XY", "緯度,經度,歸仁,,台南市,XX區,高度");
                        hm_stnAddress.put("QS", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("RA", "緯度,經度,左營,,台南市,XX區,高度");
                        hm_stnAddress.put("AY", "緯度,經度,岡山,,台南市,XX區,高度");
                        hm_stnAddress.put("CS", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("YU", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("QC", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("LM", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("GM", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("DC", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("SQ", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("TZ", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("KU", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("NO", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("MQ", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("SP", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("WK", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("DI", "緯度,經度,,,台南市,XX區,高度");
                        hm_stnAddress.put("PO", "緯度,經度,,,台南市,XX區,高度");
                        //</editor-fold>
                        break;
                    default:
                        break;
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>

    /**
     * 解析 opendata 資料 (real time)
     */
    public void parseOpendataRain() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            prop.load(new FileReader(stnwebProp));
            String dataid = prop.getProperty("dataid_rain10");
            String apikey = prop.getProperty("apikey");
//            String openDataURL = prop.getProperty("cwb_station_list");
            String whereToDownload = prop.getProperty("file_input_path");
            String fileName = prop.getProperty("file_name");
            URL wantToDownloadURL;
            File makeDownloadDir, createFile, deleteFile;
//            ArrayList<String> al_rawdata = new ArrayList<>();
            String regex = ",";
            String openDataURL;
//            Adjust adjust = new Adjust("yyyy-MM-dd HH:mm:ss");
            // 堅持先下載 XML 再解析，不然直接線上解析會太多突發狀況，例如欄位空值...等等
            openDataURL = String.format("http://opendata.cwb.gov.tw/opendataapi?dataid=%s&authorizationkey=%s",
                                        dataid, apikey);
            wantToDownloadURL = new URL(openDataURL);
            // 建立資料夾，不知道有沒有強過老方法 createDirectory.mkdirs();
            makeDownloadDir = new File(whereToDownload);
            FileUtils.forceMkdir(makeDownloadDir);
            createFile = new File(whereToDownload + fileName);
            // 下載網頁上檔案
            FileUtils.copyURLToFile(wantToDownloadURL, createFile);
            /********************** 解析原始資料 **********************/
            // Jsoup 1.10 版本讀到 locationName 時候很怪...，所以換成 1.8.3 版本
//            Document doc = Jsoup.connect(openDataURL).timeout(15000).get();
            Document doc = Jsoup.parse(new File(whereToDownload + fileName), "UTF-8");
            Elements elms_location = doc.select("location");
            String lat, lon, locationName, stationId, time, localTime, elev,
                    rain, min_10, hour_3, hour_6, hour_12, hour_24, now, city, town, attribute, stnAddress;
            int rows = 1;
            for (Element elm_location : elms_location) {
                lat = elm_location.select("lat").text();                     // <lat>23.9637</lat>
                lon = elm_location.select("lon").text();                     // <lon>120.8369</lon>
                locationName = elm_location.select("locationName").text();   // <locationName>九份二山</locationName>
                stationId = elm_location.select("stationId").text();         // <stationId>C1I230</stationId>
                time = elm_location.select("obsTime").text();                // <obsTime>2017-06-06T10:20:00+08:00</obsTime>
                adjust.inputValue(time);
                localTime = adjust.outputYMDH();                                // localTime = 2017-06-06 08:00:00
                // 氣象變數 : 第一段
                Elements elms_value = elm_location.select("value");
                elev = elms_value.get(0).text();
                // rain : 60分鐘累積雨量
                rain = elms_value.get(1).text();
                min_10 = elms_value.get(2).text();
                hour_3 = elms_value.get(3).text();
                hour_6 = elms_value.get(4).text();
                hour_12 = elms_value.get(5).text();
                hour_24 = elms_value.get(6).text();
                // now : 本日累積雨量
                now = elms_value.get(7).text();
                // 測站位置 : 第二段
                Elements elms_parameterValue = elm_location.select("parameterValue");
                city = elms_parameterValue.get(0).text();
                town = elms_parameterValue.get(2).text();
                attribute = elms_parameterValue.get(4).text();
                // 輸出結果
                String import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        lat, lon, locationName, stationId, localTime, elev,
                        rain, min_10, hour_3, hour_6, hour_12, hour_24, now,
                        city, town, attribute);
                String import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                        lat, lon, locationName, stationId, elev, city, town, attribute);
//                System.out.println((rows++) + "," + import_rawdata);
                // 匯入測站資訊
                hs_stnInfo.add(import_stnInfo);
                // 匯入原始資料
                al_rawdata.add(import_rawdata);
            }
            /********************** 會用到的語法 **********************/
            String judgeExistSQL, insertSQL, updateSQL, selectSQL, establishFK;
            /********************** 建立測站資訊 **********************/
            // StnInfo 插入 PK
            ArrayList<String> al_stnInfo = new ArrayList<>();
            PreparedStatement prepStmt;
            ResultSet rs;
            for (String s : hs_stnInfo) {
                // 讀測站資訊
                lon = s.split(regex)[0];
                lat = s.split(regex)[1];
                locationName = s.split(regex)[2];
                stationId = s.split(regex)[3];
                elev = s.split(regex)[4];
                city = s.split(regex)[5];
                town = s.split(regex)[6];
                attribute = s.split(regex)[7];
                // 另外寫方法來讀網頁資料(for stnAddress)
                // 解析測站地址
                stnAddress = hm_stnAddress.get(stationId);
                String import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        stationId, locationName, city, town, stnAddress, attribute, lat, lon, elev);
//                System.out.println((rows++) + "," + import_stnInfo);
                // 將測站列表放入動態陣列來做以下判斷，此陣列的值基本上不會重複
                al_stnInfo.add(import_stnInfo);
                // 判斷是否在 StnInfo 已經存特定測站
                // 如果不存在，新建站點(用 SQL 語法去找存不存在)
                judgeExistSQL = "SELECT COUNT(stn_id) FROM StationInfo WHERE stn_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, stationId);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的測站才更新(value == 0 代表不存在，select 不到)
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO StationInfo VALUES (?,?,?,?,?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO StationInfo VALUES (0,?,?,?,?,?,?,?,?,?)";// MySQL
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, stationId);
                        prepStmt.setString(2, locationName);
                        prepStmt.setString(3, city);
                        prepStmt.setString(4, town);
                        prepStmt.setString(5, stnAddress);
                        prepStmt.setString(6, attribute);
                        prepStmt.setString(7, lat);
                        prepStmt.setString(8, lon);
                        prepStmt.setString(9, elev);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            /********************** 建立觀測資料 **********************/
            for (String s : al_rawdata) {// 讀原始資料
                /**
                 * 把以下當作 SQL 語法，要將讀進來的資料對應出測站代碼
                 * SELECT * FROM FileInfo WHERE stn = @ss
                 */
                stationId = s.split(regex)[3];
                localTime = s.split(regex)[4];
                rain = s.split(regex)[6];
                min_10 = s.split(regex)[7];
                hour_3 = s.split(regex)[8];
                hour_6 = s.split(regex)[9];
                hour_12 = s.split(regex)[10];
                hour_24 = s.split(regex)[11];
                now = s.split(regex)[12];
                for (String s_stnoInfo : al_stnInfo) {
                    // al_stnInfo 是基準
                    String stnId_stnoInfo = s_stnoInfo.split(regex)[0];
                    if(stationId.equals(stnId_stnoInfo)){
                        // 當讀到的測站和 StnInfo 測站一樣時
                        establishFK = "SELECT stn_info_id FROM StationInfo WHERE stn_id = ?";
                        prepStmt = conn.prepareStatement(establishFK);
                        prepStmt.setString(1, stationId);
                        rs = prepStmt.executeQuery();
                        while(rs.next()){
                            int stnInfo_Id = rs.getInt(1);
                            insertSQL = "";
                            if("SQL_Server".equals(db_type)){
                                insertSQL = "INSERT INTO ObservationData VALUES (?,?,?,?,?,?,?,?,?)";// MSSQL
                            }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                                insertSQL = "INSERT INTO ObservationData VALUES (0,?,?,?,?,?,?,?,?,?)";// MySQL
                            }
                            prepStmt = conn.prepareStatement(insertSQL);
//                            System.out.println("stnInfo_Id: " + stnInfo_Id);
                            prepStmt.setInt(1, stnInfo_Id);
                            prepStmt.setString(2, localTime);
                            prepStmt.setString(3, rain);
                            prepStmt.setString(4, min_10);
                            prepStmt.setString(5, hour_3);
                            prepStmt.setString(6, hour_6);
                            prepStmt.setString(7, hour_12);
                            prepStmt.setString(8, hour_24);
                            prepStmt.setString(9, now);
                            prepStmt.executeUpdate();
                        }
                    }
                }
            }
            /********************** 建立資料起迄時間 **********************/
            ArrayList<String> al_observationTimeRange = new ArrayList<>();
            selectSQL = "SELECT stn_info_id, min(obs_time), max(obs_time) FROM ObservationData GROUP BY stn_info_id;";
            prepStmt = conn.prepareStatement(selectSQL);
            rs = prepStmt.executeQuery();
            rows = 0;
            while(rs.next()){
                int stnInfo_Id = rs.getInt(1);
                String initialTime = rs.getString(2);
                String finalTime = rs.getString(3);
                String import_output = String.format("%d,%s,%s", stnInfo_Id, initialTime, finalTime);
                al_observationTimeRange.add(import_output);
//                System.out.println((rows++) + ", " + import_output);
            }
            // 逐行判斷是否有重複的測站
            System.out.println("開始更新");
            for (String s : al_observationTimeRange) {
                int stnInfo_Id = Integer.parseInt(s.split(regex)[0]);
                String initialTime = s.split(regex)[1];
                String finalTime = s.split(regex)[2];
                judgeExistSQL = "SELECT COUNT(stn_info_id) FROM StationTimeRange WHERE stn_info_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setInt(1, stnInfo_Id);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    // 逐行判斷是否有重複的測站
                    if(value == 0){
                        // 如果沒有這個測站，就 Insert
//                        System.out.println("Insert");
                        insertSQL = "INSERT INTO StationTimeRange VALUES (?,?,?)";
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setInt(1, stnInfo_Id);
                        prepStmt.setString(2, initialTime);
                        prepStmt.setString(3, finalTime);
                    }else{
                        // 如果有這個測站，就 Update
//                        System.out.println("Update");
                        updateSQL = "UPDATE StationTimeRange SET ini_time = ?, fnl_time = ? WHERE stn_info_id = ?";
                        prepStmt = conn.prepareStatement(updateSQL);
                        prepStmt.setString(1, initialTime);
                        prepStmt.setString(2, finalTime);
                        prepStmt.setInt(3, stnInfo_Id);
                    }
                    prepStmt.executeUpdate();
                }
            }
            System.out.println("更新結束");
            // 刪除檔案
//            createFile.delete();
        } catch(IOException | SQLException ex) {
            ex.printStackTrace();
            // java.sql.SQLException: Parameter index out of range (1 > number of parameters, which is 0). 資料庫匯入時的欄位數量錯誤
        } finally {
            al_rawdata.clear();
        }
    }//</editor-fold>
    /**
     * 解析 qc 過後資料 (from cwb 第三組)
     * @param url
     */
    public void parseCWB(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        // 設定參數位置
        int row_place_PS01 = 0, row_place_TX01 = 0, row_place_RH01 = 0, row_place_WD01 = 0, row_place_WD02 = 0, row_place_PP01 = 0;
//        String temp = null;
        try {
            // 讀檔案
            br = new BufferedReader(new FileReader(url));
            while(br.ready()){
                String s = br.readLine();
                // 先判斷參數位置
                if(s.contains("# stno")){
                    int string_lengh_PS01 = s.indexOf("PS01");
                    int string_lengh_TX01 = s.indexOf("TX01");
                    int string_lengh_RH01 = s.indexOf("RH01");
                    int string_lengh_WD01 = s.indexOf("WD01");
                    int string_lengh_WD02 = s.indexOf("WD02");
                    int string_lengh_PP01 = s.indexOf("PP01");
                    row_place_PS01 = s.substring(0, string_lengh_PS01).split("\\s+").length - 1;
                    row_place_TX01 = s.substring(0, string_lengh_TX01).split("\\s+").length - 1;
                    row_place_RH01 = s.substring(0, string_lengh_RH01).split("\\s+").length - 1;
                    row_place_WD01 = s.substring(0, string_lengh_WD01).split("\\s+").length - 1;
                    row_place_WD02 = s.substring(0, string_lengh_WD02).split("\\s+").length - 1;
                    row_place_PP01 = s.substring(0, string_lengh_PP01).split("\\s+").length - 1;
//                    temp = String.format("%d,%d,%d,%d,%d,%d", row_place_PS01, row_place_TX01, row_place_RH01,
//                                        row_place_WD01, row_place_WD02, row_place_PP01);     
                }
                // start
                if(s.matches("C[01].*")){//<editor-fold defaultstate="collapsed" desc="...">
                    String stationId = s.split("\\s+")[0];
                    String raw_localTime = s.split("\\s+")[1];
                    String ps = s.split("\\s+")[row_place_PS01];
                    adjust.inputValue(raw_localTime);
                    String localTime = adjust.outputYMDH();
                    String tp = s.split("\\s+")[row_place_TX01];
                    String rh = s.split("\\s+")[row_place_RH01];
                    String ws = s.split("\\s+")[row_place_WD01];
                    String wd = s.split("\\s+")[row_place_WD02];
                    String rn = s.split("\\s+")[row_place_PP01];
                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            stationId, localTime, ps, tp, rh, ws, wd, rn);
                    // 匯入原始資料
                    al_rawdata.add(import_rawdata);
                    //</editor-fold>
                }else if(s.matches("46.*")){//<editor-fold defaultstate="collapsed" desc="...">
                    String stationId = s.split("\\s+")[0];
                    String raw_localTime = s.split("\\s+")[1];
                    String ps = s.split("\\s+")[row_place_PS01];
                    adjust.inputValue(raw_localTime);
                    String localTime = adjust.outputYMDH();
                    String tp = s.split("\\s+")[row_place_TX01];
                    String rh = s.split("\\s+")[row_place_RH01];
                    String ws = s.split("\\s+")[row_place_WD01];
                    String wd = s.split("\\s+")[row_place_WD02];
                    String rn = s.split("\\s+")[row_place_PP01];
                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            stationId, localTime, ps, tp, rh, ws, wd, rn);
                    // 匯入原始資料
                    al_rawdata.add(import_rawdata);
                }//</editor-fold>
            }
            br.close();
            /********************** 匯入資料庫 **********************/
            PreparedStatement prepStmt;
            ResultSet rs;
            String judgeExistSQL, insertSQL;
            for (String s : al_rawdata) {
                String stationId = s.split(",")[0];
                String localTime = s.split(",")[1];
                String ps = s.split(",")[2];
                String tp = s.split(",")[3];
                String rh = s.split(",")[4];
                String ws = s.split(",")[5];
                String wd = s.split(",")[6];
                String rn = s.split(",")[7];
                // 找相對應的測站
                judgeExistSQL = "SELECT stn_info_id FROM StationInfo WHERE stn_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, stationId);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    String stn_info_id = rs.getString("stn_info_id");
                    if(!stn_info_id.isEmpty()){
                        // 如果 stn_info_id 存在存在存在存在存在，就開始匯入 qcdata
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO QcData VALUES (?,?,?,?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO QcData VALUES (0,?,?,?,?,?,?,?,?)";// MySQL or MariaDB
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, stn_info_id);
                        prepStmt.setString(2, localTime);
                        prepStmt.setString(3, rn);
                        prepStmt.setString(4, ps);
                        prepStmt.setString(5, tp);
                        prepStmt.setString(6, rh);
                        prepStmt.setString(7, ws);
                        prepStmt.setString(8, wd);
                        prepStmt.executeUpdate();
                    }else{
                        // stn_info_id 為空值
                        System.err.println(stationId + " can not found!!");
//                        throw new StnIdINotFoundException(stationId);
                        // 不存在就離開這個鬼地方
                        break;
                    }
                }
            }
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
        } finally {
            // 釋放 resource
            al_rawdata.clear();
        }
    }//</editor-fold>

    /**
     * 解析民航局資料
     * @param url 
     */
    public void parseCAA(String url) {//<editor-fold defaultstate="collapsed" desc="...">
        System.out.println(adjust.getNowTime() + "  " + url);
        DecimalFormat df = new DecimalFormat("#.0");
        // 讀檔案
        try {
            // 讀檔案
            br = new BufferedReader(new FileReader(url));
            while(br.ready()){
                String s = br.readLine();
                String import_stnInfo = null, import_rawdata = null;
                if(url.contains(".txt")){
                    if(s.contains("end"))continue;
                    String stationId = "46" + s.substring(0, 3);
                    String locationName = hm_stnAddress.get(stationId).split(",")[2];
                    String lat = hm_stnAddress.get(stationId).split(",")[0];
                    String lon = hm_stnAddress.get(stationId).split(",")[1];
                    String elev = hm_stnAddress.get(stationId).split(",")[6];
                    String city = hm_stnAddress.get(stationId).split(",")[4];
                    String town = hm_stnAddress.get(stationId).split(",")[5];
                    String attribute = "民用航空局";
                    String raw_localTime = s.substring(3, 15);
                    adjust.inputValue(raw_localTime);
                    String localTime = adjust.outputYMDH();
                    String ps = Double.parseDouble(s.substring(66, 71)) / 10.0 + "";
                    String tp = Double.parseDouble(s.substring(58, 61)) / 10.0 + "";
                    String rh = s.substring(64, 66).trim();
                    if(rh.isEmpty())rh = "-999";
                    String raw_ws = s.substring(17, 20).trim();
                    String ws;
                    if(raw_ws.isEmpty()){
                        ws = "-999";
                    }else{
                        ws = df.format(Double.parseDouble(raw_ws) * 0.514) + "";
                    }
                    String wd = Double.parseDouble(s.substring(15, 17)) * 10 + "";
                    String raw_rn_old = s.substring(71, 76).trim();
                    String rn;
                    if(raw_rn_old.isEmpty()){
                        rn = "-999.0";
                    }else{
                        double raw_rn = Double.parseDouble(raw_rn_old) / 100.0;
                        if(raw_rn >= 500.0){
                            rn = "-999.0";
                        }else{
                            rn = raw_rn + "";
                        }
                    }
                    import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            stationId, localTime, ps, tp, rh, ws, wd, rn);
                    import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            lat, lon, locationName, stationId, elev, city, town, attribute);
                }else if(url.contains(".csv")){
                    if(!s.matches("[0-9].*"))continue;
                    String stationId = "46" + s.split(",")[0];
                    String locationName = hm_stnAddress.get(stationId).split(",")[2];
                    String lat = hm_stnAddress.get(stationId).split(",")[0];
                    String lon = hm_stnAddress.get(stationId).split(",")[1];
                    String elev = hm_stnAddress.get(stationId).split(",")[6];
                    String city = hm_stnAddress.get(stationId).split(",")[4];
                    String town = hm_stnAddress.get(stationId).split(",")[5];
                    String attribute = "民用航空局";
                    int year = Integer.parseInt(s.split(",")[1]);
                    int month = Integer.parseInt(s.split(",")[2]);
                    int day = Integer.parseInt(s.split(",")[3]);
                    int hour = Integer.parseInt(s.split(",")[4]);
                    int minute = Integer.parseInt(s.split(",")[5]);
                    adjust.inputValue(year, month, day, hour, minute);
                    String localTime = adjust.outputYMDH();
                    String ps = s.split(",")[29].trim();
                    String tp = s.split(",")[26].trim();
                    String rh = s.split(",")[28].trim();
                    String ws = s.split(",")[7].trim();
                    String wd = s.split(",")[6].trim();
                    String raw_rn = s.split(",")[30].trim();
                    String rn;
                    if(raw_rn.isEmpty()){
                        rn = "-999.0";
                    }else{
                        rn = raw_rn;
                    }
                    import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            stationId, localTime, ps, tp, rh, ws, wd, rn);
                    import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                            lat, lon, locationName, stationId, elev, city, town, attribute);
                }
                // 匯入測站資訊
                hs_stnInfo.add(import_stnInfo);
                // 匯入原始資料
                al_rawdata.add(import_rawdata);
            }
            br.close();
            /********************** 會用到的語法 **********************/
            String judgeExistSQL, insertSQL, updateSQL, selectSQL, establishFK;
            PreparedStatement prepStmt;
            ResultSet rs;
            /********************** 建立測站資訊 **********************/
            // StnInfo 插入 PK
            ArrayList<String> al_stnInfo = new ArrayList<>();
            for (String s : hs_stnInfo) {
                // 讀測站資訊
                String lon = s.split(",")[0];
                String lat = s.split(",")[1];
                String locationName = s.split(",")[2];
                String stationId = s.split(",")[3];
                String elev = s.split(",")[4];
                String city = s.split(",")[5];
                String town = s.split(",")[6];
                String attribute = s.split(",")[7];
                // 另外寫方法來讀網頁資料(for stnAddress)
                // 解析測站地址
                String stnAddress = hm_stnAddress.get(stationId).split(",")[3];
                String import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        stationId, locationName, city, town, stnAddress, attribute, lat, lon, elev);
//                System.out.println((rows++) + "," + import_stnInfo);
                // 將測站列表放入動態陣列來做以下判斷，此陣列的值基本上不會重複
                al_stnInfo.add(import_stnInfo); 
                // 判斷是否在 StnInfo 已經存特定測站
                // 如果不存在，新建站點(用 SQL 語法去找存不存在)
                judgeExistSQL = "SELECT COUNT(stn_id) FROM StationInfo WHERE stn_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, stationId);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的測站才更新(value == 0 代表不存在，select 不到)
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO StationInfo VALUES (?,?,?,?,?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO StationInfo VALUES (0,?,?,?,?,?,?,?,?,?)";// MySQL or MariaDB
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, stationId);
                        prepStmt.setString(2, locationName);
                        prepStmt.setString(3, city);
                        prepStmt.setString(4, town);
                        prepStmt.setString(5, stnAddress);
                        prepStmt.setString(6, attribute);
                        prepStmt.setString(7, lat);
                        prepStmt.setString(8, lon);
                        prepStmt.setString(9, elev);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            /********************** 匯入資料庫 **********************/
            for (String s : al_rawdata) {
                String stationId = s.split(",")[0];
                String localTime = s.split(",")[1];
                String ps = s.split(",")[2];
                String tp = s.split(",")[3];
                String rh = s.split(",")[4];
                String ws = s.split(",")[5];
                String wd = s.split(",")[6];
                String rn = s.split(",")[7];
                // 找相對應的測站
                judgeExistSQL = "SELECT stn_info_id FROM StationInfo WHERE stn_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, stationId);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    String stn_info_id = rs.getString("stn_info_id");
                    if(!stn_info_id.isEmpty()){
                        // 如果 stn_info_id 存在存在存在存在存在，就開始匯入 qcdata
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO QcData VALUES (?,?,?,?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) || "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO QcData VALUES (0,?,?,?,?,?,?,?,?)";// MySQL or MariaDB
                        }
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, stn_info_id);
                        prepStmt.setString(2, localTime);
                        prepStmt.setString(3, rn);
                        prepStmt.setString(4, ps);
                        prepStmt.setString(5, tp);
                        prepStmt.setString(6, rh);
                        prepStmt.setString(7, ws);
                        prepStmt.setString(8, wd);
                        prepStmt.executeUpdate();
                    }else{
                        // stn_info_id 為空值
                        System.err.println(stationId + " can not found!!");
//                        throw new StnIdINotFoundException(stationId);
                        // 不存在就離開這個鬼地方
                        break;
                    }
                }
            }
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
        } finally {
            // 釋放 resource
            hs_stnInfo.clear();
            al_rawdata.clear();
        }
    }//</editor-fold>

    /**
     * 解析空軍資料
     * @param url
     */
    public void parseAF(String url) {

    }
    
    /**
     * 解析觀測資料查詢系統
     */
    public void parseCODis() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            prop.load(new FileReader(stnwebProp));
            String url_main = prop.getProperty("codis_list");
            HtmlUnit htmlUnit = new HtmlUnit(url_main);
            HtmlPage htmlPage = htmlUnit.getHtmlPage();
            HtmlForm htmlForm = htmlPage.getFormByName("stationSelect");
            /**
             * 處理縣市
             * <select name="stationCounty" id="stationCounty" style="width: 220px;">
                <option value="臺北市">臺北市 (TaipeiCity)</option>
                <option value="新北市">新北市 (NewTaipeiCity)</option>
             */
            DomElement de_stationCounty = htmlPage.getElementById("stationCounty");
            DomNodeList<HtmlElement> dnl_stationCounty = de_stationCounty.getElementsByTagName("option");
            for (HtmlElement he_stationCounty : dnl_stationCounty) {
                String stationCounty = he_stationCounty.getAttribute("value");
                HtmlSelect htmlSelect_stationCounty = htmlForm.getSelectByName("stationCounty");
                HtmlOption htmlOption_stationCounty = htmlSelect_stationCounty.getOptionByValue(stationCounty);
                htmlSelect_stationCounty.setSelectedAttribute(htmlOption_stationCounty, true);
                /**
                 * 處理測站
                 * <select name="station" id="station" style="width: 220px;">
                   <option value="466910">鞍部 (ANBU)</option>
                   <option value="466920">臺北 (TAIPEI)</option>
                */
                DomElement de_station = htmlPage.getElementById("station");
                DomNodeList<HtmlElement> dnl_station = de_station.getElementsByTagName("option");
                for (HtmlElement he_station : dnl_station) {
                    String station_id = he_station.getAttribute("value");
                    String station_tw = he_station.getTextContent();
                    HtmlSelect htmlSelect_station = htmlForm.getSelectByName("station");
                    HtmlOption htmlOption_station = htmlSelect_station.getOptionByValue(station_id);
                    htmlSelect_station.setSelectedAttribute(htmlOption_station, true);
                    // 時間範圍
                    adjust = new Adjust("yyyy-MM-dd");
                    String inital_date = prop.getProperty("inital_date");
                    adjust.inputValue(inital_date);
                    int rangeDay = adjust.diffDay(inital_date, "2010-01-02");
                    int d = 0;
                    do {
                        String date = adjust.adjustDay(d);
                        // 輸出網址
                        String url_stnData = String.format(
                                "https://e-service.cwb.gov.tw/HistoryDataQuery/DayDataController.do?command=viewMain&station=%s&stname=%s&datepicker=%s", 
                                station_id, station_tw, date);
                        System.out.println(url_stnData);
                        ssl.enableSSLSocket();
                        Document doc = Jsoup.connect(url_stnData).get();
                        Elements elms_div = doc.select("div");
                        for (Element elm_div : elms_div) {
                            /**
                             * 跳過這個
                             * <div id="hea_t" style="position:fixed;top:0px;">
                             */
                            if(elm_div.hasAttr("id"))continue;
                            /**
                             * 選這個才是有資料的地方
                               <div class="CSSTableGenerator" style="position:relative;top:30px">
                             */
                            String label = elm_div.select("label").text();
                            if(label.equals("本段時間區間內無觀測資料。"))break;
                            Elements elms_table = elm_div.select("table");
                            for (Element elm_table : elms_table) {
                                Elements elms_tr = elm_table.select("tr");
                                for (Element elm_tr : elms_tr) {
                                    /**
                                     * 前兩列不要看
                                     * <tr class="first_tr">
                                       <tr class="second_tr">
                                     */                                    
                                    if(elm_tr.hasClass("first_tr") == true || elm_tr.hasClass("second_tr") == true)continue;
                                    Elements elms_td = elm_tr.select("td");
                                    String obsTime = elms_td.get(0).text().trim();
                                    String stnPres = elms_td.get(1).text().trim();
//                                    String seaPres = elms_td.get(2).text().trim();
                                    String temperature = elms_td.get(3).text();
                                    String td = elms_td.get(4).text();
                                    String rh = elms_td.get(5).text();
                                    String ws = elms_td.get(6).text();
                                    String wd = elms_td.get(7).text().trim();
                                    String precp = elms_td.get(10).text().trim();     
                                    
                                    String import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                            station_id, obsTime, stnPres, temperature, td,
                                            rh, ws, wd, precp);
                                    System.out.println(import_rawdata);
                                }
                            }
                        }
                        d++;
                        break;
                    } while(d <= rangeDay);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
}
