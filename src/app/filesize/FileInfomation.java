package app.filesize;

import java.io.File;

/**
 * 檔案資訊
 * @author tony
 */
public class FileInfomation {
    
    /**
     * 檔案大小
     * @param url 
     */
    public int getByte(String url) {
        File f = new File(url);
        return (int) f.length();
    }
}
