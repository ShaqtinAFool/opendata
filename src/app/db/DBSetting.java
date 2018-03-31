package app.db;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 作業系統環境設定
 * @author 1312032
 */
public class DBSetting {
    
    private Properties prop;
    private Connection conn;
    private String db, driver, jdbcURL, user, password;
    
    /**
     * 設定初始條件
     */
    public DBSetting() {//<editor-fold defaultstate="collapsed" desc="...">
        prop = new Properties();
        try {
            prop.load(new FileReader("./src/db.properties"));
            db = prop.getProperty("db");
            if(db.equals("SQL_Server")){
                driver = prop.getProperty("mssql_driver");
                jdbcURL = prop.getProperty("mssql_jdbcURL");
                user = prop.getProperty("mssql_user");
                password = prop.getProperty("mssql_password");
            }else if(db.equals("MySQL")){
                driver = prop.getProperty("mysql_driver");
                jdbcURL = prop.getProperty("mysql_jdbcURL");
                user = prop.getProperty("mysql_user");
                password = prop.getProperty("mysql_password"); 
            }else{
                driver = prop.getProperty("maria_driver");
                jdbcURL = prop.getProperty("maria_jdbcURL");
                user = prop.getProperty("maria_user");
                password = prop.getProperty("maria_password"); 
            }
            Class.forName(driver);
            conn = DriverManager.getConnection(jdbcURL, user, password);            
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("使用 " + db);
        }
    }//</editor-fold>
        
    /**
     * 取得連接資料庫的設定
     * @param input 輸入文字
     * @return 
     */
    protected String getReturnValue(String input) {
        String returnValue;
        switch (input) {
            case "driver":
                returnValue = driver;
                break;
            case "url":
                returnValue = jdbcURL;
                break;
            case "user":
                returnValue = user;
                break;
            case "password":
                returnValue = password;
                break;
            case "dbType":
//                returnValue = db_type.name();
                returnValue = db;
                break;
            default:
                returnValue = "";
        }
        return returnValue;
    }
    
    /**
     * 回傳資料庫連線資訊
     * @return Connection
     */
    public Connection getConn() {//<editor-fold defaultstate="collapsed" desc="...">
        return conn;
    }//</editor-fold>
}