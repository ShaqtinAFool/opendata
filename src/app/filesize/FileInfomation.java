package app.filesize;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 檔案資訊
 * @author tony
 */
public class FileInfomation {
    
    private File f;
    private Path p;
    
    /**
     * 取得指令目錄名稱
     * @param url
     * @param lev 
     * @return 目錄名稱
     */
    public String getDirectory(String url, int lev) {
        p = Paths.get(url);
        String dir = p.getName(lev).toString();
        return dir;
    }
    
    /**
     * 取得檔案大小
     * @param url 
     */
    public int getFileSizeByByte(String url) {
        f = new File(url);
        return (int) f.length();
    }
}
