package app.cases;

import app.db.DBSetting;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weather.Adjust;

/**
 * 建立個案清單
 * @author tony
 */
public class CasesData extends DBSetting {
    
    private Adjust adjust;
    private String caseType_EN, caseType_TW;
    private Properties prop;
    private Connection conn;
    private HashSet<String> hs_caseDefinition;
    private ArrayList<String> al_overview, al_rawdata;
    private PreparedStatement prepStmt;
    private ResultSet rs;

    /**
     * 
     * @param dbEnum 
     */
    public CasesData(Enum dbEnum) {
        // 必寫
        super(dbEnum);
        // 資料庫
        conn = getConn();        
        adjust = new Adjust("yyyy-MM-dd");
        prop = new Properties();
        hs_caseDefinition = new HashSet<>();
        al_rawdata = new ArrayList<>();
    }
    
    /**
     * 定義梅雨個案，用 poi
     */
    public void defineMeiyu() {//<editor-fold defaultstate="collapsed" desc="...">
        caseType_EN = "Meiyu";
        caseType_TW = "梅雨";
        try {
            prop.load(new FileReader(caseProp));
            String url = prop.getProperty("meiyuURL");
            File f = new File(url);
            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(url));
            // 0 代表第 1 個頁籤
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFRow row = sheet.getRow(1);
            // 欄位名稱設定
            String definition = row.getCell(0).getStringCellValue();
            String data_source = row.getCell(1).getStringCellValue();
            String reference = row.getCell(2).getStringCellValue();
            // 輸出結果
            String output = String.format("%s,%s,%s,%s,%s",
                    caseType_EN, caseType_TW, definition, data_source, reference);
            hs_caseDefinition.add(output);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            importCaseDefinition();
        }
    }//</editor-fold>
    
    /**
     * 定義午後陣雨個案，用 poi
     */
    public void defineThunderstorm() {//<editor-fold defaultstate="collapsed" desc="...">
        caseType_EN = "Thunderstorm";
        caseType_TW = "午後陣雨";
        try {
            prop.load(new FileReader(caseProp));
            String url = prop.getProperty("thunderstormURL");
            File f = new File(url);
            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(url));
            // 0 代表第 1 個頁籤
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFRow row = sheet.getRow(1);
            // 欄位名稱設定
            String definition = row.getCell(0).getStringCellValue();
            String data_source = row.getCell(1).getStringCellValue();
            String reference = row.getCell(2).getStringCellValue();
            // 輸出結果
            String output = String.format("%s,%s,%s,%s,%s",
                    caseType_EN, caseType_TW, definition, data_source, reference);
            hs_caseDefinition.add(output);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>    
    
    /**
     * 解析梅雨個案，用 poi
     */
    public void parseMeiyu() {//<editor-fold defaultstate="collapsed" desc="...">
        caseType_EN = "Meiyu";
        try {
            prop.load(new FileReader(caseProp));
            String url = prop.getProperty("meiyuURL");
            File f = new File(url);
            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(url));
            // workbook.getNumberOfSheets() : 頁籤數量
            for (int j = 1; j < workbook.getNumberOfSheets(); j++) {
                // i-1 代表第 i 個頁籤
                XSSFSheet sheet = workbook.getSheetAt(j);
                XSSFCell cell;
                XSSFRow row;
                // sheet.getLastRowNum() : 列數(橫)，共有幾列
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    row = sheet.getRow(i);
                    // 欄位名稱設定
                    int caseNumber = (int) row.getCell(0).getNumericCellValue();
                    double year = row.getCell(1).getNumericCellValue();
                    double month = row.getCell(2).getNumericCellValue();
                    double day = row.getCell(3).getNumericCellValue();
                    adjust.inputValue((int) year, (int) month, (int) day);
                    String basetime = adjust.outputYMDH();
                    String daily_rainfall = String.format("CWB_Rain_%04d%02d%02d.jpg", (int) year, (int) month, (int) day);
                    String prevailing_wind = "無分析";
                    String describe_rainfall_distribution = row.getCell(5).getStringCellValue();
                    String describe_sfc_weather = row.getCell(7).getStringCellValue();
                    String describe_850_weather = row.getCell(8).getStringCellValue();
                    String describe_500_weather = row.getCell(9).getStringCellValue();
                    String describe_satellite_weather = row.getCell(10).getStringCellValue();
                    String note = "無";
                    // 輸出結果
                    String output = String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            caseType_EN, caseNumber, basetime, 
                            describe_rainfall_distribution, describe_sfc_weather, describe_850_weather,
                            describe_500_weather, describe_satellite_weather, daily_rainfall, prevailing_wind, note);
                    al_rawdata.add(output);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            importCaseDefinition();
            importOverview();
        }
    }//</editor-fold>

    /**
     * 解析午後陣雨個案，用 poi
     */
    public void parseThunderstorm() {//<editor-fold defaultstate="collapsed" desc="...">
        caseType_EN = "Thunderstorm";
        try {
            prop.load(new FileReader(caseProp));
            String url = prop.getProperty("thunderstormURL");
            File f = new File(url);
            XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(url));
            // workbook.getNumberOfSheets() : 頁籤數量
            for (int j = 1; j < workbook.getNumberOfSheets(); j++) {
                // i-1 代表第 i 個頁籤
                XSSFSheet sheet = workbook.getSheetAt(j);
                XSSFCell cell;
                XSSFRow row;
                // sheet.getLastRowNum() : 列數(橫)，共有幾列
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    row = sheet.getRow(i);
                    // 欄位名稱設定
                    int caseNumber = (int) row.getCell(0).getNumericCellValue();
                    double year = row.getCell(1).getNumericCellValue();
                    double month = row.getCell(2).getNumericCellValue();
                    double day = row.getCell(3).getNumericCellValue();
                    adjust.inputValue((int) year, (int) month, (int) day);
                    String basetime = adjust.outputYMDH();
                    String daily_rainfall = String.format("CWB_Rain_%04d%02d%02d.jpg", (int) year, (int) month, (int) day);
                    String prevailing_wind = row.getCell(4).getStringCellValue();
                    String describe_rainfall_distribution = "無分析";
                    String describe_sfc_weather = row.getCell(7).getStringCellValue();
                    String describe_850_weather = "無分析";
                    String describe_500_weather = "無分析";
                    String describe_satellite_weather = "無分析";
                    String note = row.getCell(11).getStringCellValue();
                    // 輸出結果
                    String output = String.format("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            caseType_EN, caseNumber, basetime, 
                            describe_rainfall_distribution, describe_sfc_weather, describe_850_weather,
                            describe_500_weather, describe_satellite_weather, daily_rainfall, prevailing_wind, note);
                    al_rawdata.add(output);
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            importCaseDefinition();
            importOverview();
        }
    }//</editor-fold>
    
    /**
     * 匯入個案定義
     */
    private void importCaseDefinition() {//<editor-fold defaultstate="collapsed" desc="...">
        /********************** 會用到的語法 **********************/
        String judgeExistSQL, insertSQL;
        /********************* 建立 case 資訊 *********************/     
        try {        
            for (String s : hs_caseDefinition) {
                String name_en = s.split(",")[0];
                String name_tw = s.split(",")[1];
                String definite = s.split(",")[2];
                String data_source = s.split(",")[3];
                String reference = s.split(",")[4];
                judgeExistSQL = "SELECT COUNT(*) FROM CaseDefinition WHERE name_en = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, name_en);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    int value = rs.getInt(1);
                    if(value == 0){
                        insertSQL = "INSERT INTO CaseDefinition VALUES (0,?,?,?,?,?)";
                        prepStmt = conn.prepareStatement(insertSQL);
                        prepStmt.setString(1, name_en);
                        prepStmt.setString(2, name_tw);
                        prepStmt.setString(3, definite);
                        prepStmt.setString(4, data_source);
                        prepStmt.setString(5, reference);
                        prepStmt.executeUpdate();
                    }else{
                        // 存在就不動作
                        break;
                    }
                }
            }
            prepStmt.close();
            rs.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>
    
    /**
     * 匯入個案內容
     */
    private void importOverview() {//<editor-fold defaultstate="collapsed" desc="...">
        /********************** 會用到的語法 **********************/
        String judgeExistSQL, insertSQL;
        String cd_id = "";
        /******************* 建立 overview 資訊 *******************/     
        try {
            for (String s : al_rawdata) {
                // 先找 CaseDefinition 有沒有相對應個案
                String name_en = s.split(",")[0];// case_type
                String case_number = s.split(",")[1];
                String base_time = s.split(",")[2];
                String describe_rainfall_distribution = s.split(",")[3];
                String describe_sfc_weather = s.split(",")[4];
                String describe_850_weather = s.split(",")[5];
                String describe_500_weather = s.split(",")[6];                
                String describe_satellite_weather = s.split(",")[7];   
                String daily_rainfall = s.split(",")[8]; 
                String prevailing_wind = s.split(",")[9]; 
                String note = s.split(",")[10]; 
                // 若有就匯入
                judgeExistSQL = "SELECT cd_id FROM CaseDefinition WHERE name_en = ?";
                prepStmt = conn.prepareStatement(judgeExistSQL);
                prepStmt.setString(1, name_en);
                rs = prepStmt.executeQuery();
                while(rs.next()){
                    cd_id = rs.getString(1);
                }
                insertSQL = "INSERT INTO Overview VALUES (0,?,?,?,?,?,?,?,?,?,?,?)";
                prepStmt = conn.prepareStatement(insertSQL);
                prepStmt.setString(1, cd_id);
                prepStmt.setString(2, case_number);
                prepStmt.setString(3, base_time);
                prepStmt.setString(4, describe_rainfall_distribution);
                prepStmt.setString(5, describe_sfc_weather);
                prepStmt.setString(6, describe_850_weather);
                prepStmt.setString(7, describe_500_weather);
                prepStmt.setString(8, describe_satellite_weather);
                prepStmt.setString(9, daily_rainfall);
                prepStmt.setString(10, prevailing_wind);
                prepStmt.setString(11, note);
                prepStmt.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>    
}
