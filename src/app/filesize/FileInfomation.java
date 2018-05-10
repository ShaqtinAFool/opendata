package app.filesize;

import app.itf.Itf_Prop;
import static app.itf.Itf_Prop.tywebProp;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 檔案資訊
 * @author tony
 */
public class FileInfomation implements Itf_Prop {
    
    private File f;
    private Path p;
    private Properties prop;

    /**
     * 建構子
     */
    public FileInfomation() {
        try {
            prop = new Properties();
            prop.load(new FileReader(tywebProp));
        } catch (IOException ex) {
            ex.printStackTrace();
        }     
    }
    
    /**
     * 取得指令目錄名稱
     * @param url
     * @param lev 
     * @return 目錄名稱
     */
    public String getDirectoryName(String url, int lev) {
        p = Paths.get(url);
        String dir = p.getName(lev).toString();
        return dir;
    }
    
    /**
     * 取得目錄層數
     * @param url
     * @return 
     */
    public int getDirectoryLevel(String url) {
        p = Paths.get(prop.get("download_dir_path").toString());
        int lev = p.getNameCount();
        return lev;
    }
    
    /**
     * 取得檔案大小
     * @param url 
     */
    public int getFileSizeByBytes(String url) {
        f = new File(url);
        return (int) f.length();
    }
}
