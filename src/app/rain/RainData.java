package app.rain;

import app.db.DBSetting;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weather.Adjust;

/**
 * 解析雨量資料
 */
public class RainData extends DBSetting {

    private final Connection conn;
    private final ArrayList<String> al_rawdata;
    // 映射:可參考 http://jax-work-archive.blogspot.tw/2015/02/java-setlistmap.html
    private final HashMap<String, String> hm_stnAddress;
    private final String db_type;

    
    public RainData() {
        // 資料庫
        db_type = getReturnValue("dbType");
        conn = getConn();
        // 原始資料
        al_rawdata = new ArrayList<>();
        hm_stnAddress = new HashMap<>();        
    }

    
    
    /**
     * 取得測站地址，目前已收集 CWB, WRA
     */
    public void setStnAddress() {//<editor-fold defaultstate="collapsed" desc="...">
        String[] stnType = {"CWB", "WRA"};
        String openDataURL;
        String whereToDownload;
        String fileName;
        URL wantToDownloadURL;
        File makeDownloadDir, createFile, deleteFile;
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(stnwebProp));
            for (String s : stnType) {
                if("CWB".equals(s)){
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
//                                System.out.println(obsId + "," + locAddress);
                                hm_stnAddress.put(obsId, locAddress);
                            }
                        }
                    }
                }else if("WRA".equals(s)){
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
                    }
                    br.close();
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
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 解析讀進來的資料
     */
    public void parseData() {//<editor-fold defaultstate="collapsed" desc="...">
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(stnwebProp));
            String dataid = prop.getProperty("dataid_rain10");
            String apikey = prop.getProperty("apikey");
//            String openDataURL = prop.getProperty("cwb_station_list");
            String whereToDownload = prop.getProperty("file_input_path");
            String fileName = prop.getProperty("file_name");
            URL wantToDownloadURL;
            File makeDownloadDir, createFile, deleteFile;
            HashSet<String> hs_stnInfo = new HashSet<>();
            ArrayList<String> al_rawdata = new ArrayList<>();
            String regex = ",";
            String openDataURL;
            Adjust adjust = new Adjust("yyyy-MM-dd HH:mm:ss");
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
                String import_rawdata = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s" , 
                        lat, lon, locationName, stationId, localTime, elev,
                        rain, min_10, hour_3, hour_6, hour_12, hour_24, now,
                        city, town, attribute);
                String import_stnInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%s" , 
                        lat, lon, locationName, stationId, elev,
                        city, town, attribute);
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
                String import_stnInfo = 
                        String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                stationId, locationName, city, town, stnAddress, attribute, lat, lon, elev);
//                System.out.println((rows++) + "," + import_stnInfo);
                // 將測站列表放入動態陣列來做以下判斷，此陣列的值基本上不會重複
                al_stnInfo.add(import_stnInfo); 
                // 判斷是否在 StnInfo 已經存特定測站
                // 如果不存在，新建站點(用 SQL 語法去找存不存在)
                judgeExistSQL = "SELECT COUNT(stn_id) FROM Rn_StationInfo WHERE stn_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, stationId);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        // 不存在的測站才更新(value == 0 代表不存在，select 不到)
                        insertSQL = "";
                        if("SQL_Server".equals(db_type)){
                            insertSQL = "INSERT INTO Rn_StationInfo VALUES (?,?,?,?,?,?,?,?,?)";// MSSQL
                        }else if("MySQL".equals(db_type) | "MariaDB".equals(db_type)){
                            insertSQL = "INSERT INTO Rn_StationInfo VALUES (0,?,?,?,?,?,?,?,?,?)";// MySQL
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
                        establishFK = "SELECT stn_info_id FROM Rn_StationInfo WHERE stn_id = ?";
                        prepStmt = conn.prepareStatement(establishFK);
                        prepStmt.setString(1, stationId);
                        rs = prepStmt.executeQuery();
                        while(rs.next()){
                            int stnInfo_Id = rs.getInt(1);
                            insertSQL = "";
                            if("SQL_Server".equals(db_type)){
                                insertSQL = "INSERT INTO Rn_ObservationData VALUES (?,?,?,?,?,?,?,?,?)";// MSSQL
                            }else if("MySQL".equals(db_type) | "MariaDB".equals(db_type)){
                                insertSQL = "INSERT INTO Rn_ObservationData VALUES (0,?,?,?,?,?,?,?,?,?)";// MySQL
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
            selectSQL = "SELECT stn_info_id, min(obs_time), max(obs_time) FROM Rn_ObservationData GROUP BY stn_info_id;";
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
                judgeExistSQL = "SELECT COUNT(stn_info_id) FROM Rn_StationTimeRange WHERE stn_info_id = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setInt(1, stnInfo_Id);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    // 逐行判斷是否有重複的測站
                    if(value == 0){
                        // 如果沒有這個測站，就 Insert
//                        System.out.println("Insert");
                        insertSQL = "INSERT INTO Rn_StationTimeRange VALUES (?,?,?)";
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setInt(1, stnInfo_Id);
                        prepStmt.setString(2, initialTime);
                        prepStmt.setString(3, finalTime);
                        
                    }else{
                        // 如果有這個測站，就 Update
//                        System.out.println("Update");
                        updateSQL = "UPDATE Rn_StationTimeRange SET ini_time = ?, fnl_time = ? WHERE stn_info_id = ?";
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
        }
    }//</editor-fold>    
}
