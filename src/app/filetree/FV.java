package app.filetree;

import app.itf.ITF_DB;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

/**
 * 檔案路徑
 * @author tony
 */
public class FV implements ITF_DB {
    
    private final WalkFileTree w_file;

    /**
     * 初始設定
     */
    public FV() {
        w_file = new WalkFileTree();
    }
    
    /**
     * 將檔案路徑收集起來
     * @return ArrayList<?>
     */
    public ArrayList<?> getTyphoonPath(){
        Properties prop = new Properties();
        ArrayList<String> al_url = new ArrayList<>();
        try {
            prop.load(new FileReader(tywebProp));
            String whereToDownload = prop.getProperty("download_dir_path");
            Path path = Paths.get(whereToDownload);
            Files.walkFileTree(path, w_file.findFile());
            for (String url : w_file.returnFV()) {
                al_url.add(url);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return al_url;
    }
    
    
//    public static void main(String[] args) {
//        FV fv = new FV();
//        for (Object string : fv.getTyphoonPath()) {
//            System.out.println(string);
//        }
//    }
}
