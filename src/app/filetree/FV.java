package app.filetree;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import app.itf.Itf_Prop;

/**
 * 檔案路徑
 * @author tony
 */
public class FV implements Itf_Prop {
    
    private final WalkFileTree w_file;
    private final Properties prop;
    private Enum e;

    /**
     * 初始設定
     * @param e
     */
    public FV(Enum e) {
        this.e = e;
        w_file = new WalkFileTree();
        prop = new Properties();
    }
    
    /**
     * 將檔案路徑收集起來
     * @return ArrayList<?>
     */
    public ArrayList<?> getPath(){
        ArrayList<String> al_url = new ArrayList<>();
        try {
            if(e.equals(FVEnum.typhoon)){
                prop.load(new FileReader(tywebProp));
                String whereToDownload = "";
                if("1".equals(prop.get("tigge_parse_realtime"))){
                    // 即時資料
                    whereToDownload = prop.getProperty("download_dir_path_temp");
                    
                }else if(!"1".equals(prop.get("tigge_parse_realtime"))){
                    // 歷史資料
                    whereToDownload = prop.getProperty("download_dir_path");
                }
                Path path = Paths.get(whereToDownload);
                Files.walkFileTree(path, w_file.findFile());
                for (String url : w_file.returnFV()) {
                    if(!url.contains("DS_Store"))al_url.add(url);
                }                
            }else if(e.equals(FVEnum.station)){
                prop.load(new FileReader(stnwebProp));
                String whereToDownload = prop.getProperty("history_file_path");
                Path path = Paths.get(whereToDownload);
                Files.walkFileTree(path, w_file.findFile());
                for (String url : w_file.returnFV()) {
                    if(!url.contains("DS_Store"))al_url.add(url);
                }                
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
