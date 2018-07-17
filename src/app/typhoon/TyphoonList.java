package app.typhoon;

//<editor-fold defaultstate="collapsed" desc="import...">
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import app.itf.Itf_Prop;
//</editor-fold>

/**
 * 
 * @author tony
 */
public class TyphoonList implements Itf_Prop {
    
    private String url, tylist;
    private String whereToDownload;
    private Document doc;
    private File file;
    private ArrayList<String> al_tylist;
    
    /**
     * 初始設定
     */
    public TyphoonList() {//<editor-fold defaultstate="collapsed" desc="...">
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(tywebProp));
            url = prop.getProperty("cwb_typhoon_list_url");
            whereToDownload = prop.getProperty("cwb_typhoon_list_path");
            tylist = whereToDownload + "tylist.html";
            file = new File(tylist);
            al_tylist = new ArrayList<>();
            /**
             * 測試1 處理 java.lang.OutOfMemoryError: Java heap space，將 doc 擺在建構子
             */
            doc = Jsoup.parse(file , "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//</editor-fold>

    /**
     * 抓氣象局颱風資料庫資料，產出 html 檔案
     */
    public void getCWBTyListToHtml() {//<editor-fold defaultstate="collapsed" desc="...">
        int waitTimeSec = 20;
        WebClient webClient = new WebClient();

        // 關閉 htmlunit warnings
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);      

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(whereToDownload + "tylist.html"),"UTF-8"))) {
            HtmlPage page = webClient.getPage(url);
            // https://developers.google.com/webmasters/ajax-crawling/docs/html-snapshot
            webClient.waitForBackgroundJavaScript(waitTimeSec * 1000);
            List<DomElement> domNodeList = page.getElementsByIdAndOrName("content2");  // 我要的
            for (int i = 0; i < domNodeList.size(); i++) {
                DomElement domElement = (DomElement) domNodeList.get(i);
                bw.write(domElement.asXml());
            }
        } catch (IOException | FailingHttpStatusCodeException ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("from CWB 更新颱風名單");
        }
    }//</editor-fold>

    /**
     * 擷取氣象局颱風編號
     * @param ty_name
     * @param setYear
     * @return 颱風編號(年份 + 編號)
     */
    public int getCWBTyNumber(String ty_name , int setYear) {//<editor-fold defaultstate="collapsed" desc="...">
        Elements elms_tr = doc.select("div").select("table").select("tbody").select("tr");
        for(Element elm_tr : elms_tr){
            String typhoonWarningList = elm_tr.text();
            String typhoonYear = typhoonWarningList.split("\\s+")[0];
            if(isNumeric(typhoonYear)){
                int year = Integer.parseInt(typhoonYear);
                int tyNumber = Integer.parseInt(typhoonWarningList.split("\\s+")[1]);
                String typhoon_name_en = typhoonWarningList.split("\\s+")[3].trim();     
                if(year == setYear){
                    if(typhoon_name_en.contains(ty_name)){
                        return tyNumber;
                    }
                }
            }
        }
        // 沒此颱風名稱時，回傳 -999
        return -999;
    }//</editor-fold>    

    /**
     * 擷取氣象局颱風英文名稱
     * @param setYear
     * @return 颱風英文名稱_年份
     */
    public ArrayList<String> getCWBTyEnName(int setYear) {//<editor-fold defaultstate="collapsed" desc="...">
        Elements elms_tr = doc.select("div").select("table").select("tbody").select("tr");
        for(Element elm_tr : elms_tr){
            String typhoonWarningList = elm_tr.text();
            String typhoonYear = typhoonWarningList.split("\\s+")[0];
            if(isNumeric(typhoonYear)){
                int year = Integer.parseInt(typhoonYear);
                String typhoon_name_en = typhoonWarningList.split("\\s+")[3].trim();
                if(year == setYear){
                    al_tylist.add(typhoon_name_en + "_" + setYear);
                }
            }
        }
        return al_tylist;
    }//</editor-fold>        
    
    /**
     * 擷取氣象局颱風中文名稱
     * @param setTyNumber
     * @return 颱風中文名稱
     */
    public String getCWBTyTWName(int setTyNumber) {//<editor-fold defaultstate="collapsed" desc="...">
        Elements elms_tr = doc.select("div").select("table").select("tbody").select("tr");
        for(Element elm_tr : elms_tr){
            String typhoonWarningList = elm_tr.text();
            String typhoonYear = typhoonWarningList.split("\\s+")[0];
            if(isNumeric(typhoonYear)){
                int tyNumber = Integer.parseInt(typhoonWarningList.split("\\s+")[1]);
                String typhoon_name_tw = typhoonWarningList.split("\\s+")[2].trim();
                if(setTyNumber == tyNumber){
                    return typhoon_name_tw;
                }
            }
        }
        return null;
    }//</editor-fold>  
    
    /**
     * 正規表示式，判斷字串是否是數字
     * @param str
     * @return 
     */
    public boolean isNumeric(String str) {//<editor-fold defaultstate="collapsed" desc="...">
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if(!isNum.matches()){
            return false;
        }
        return true;
    }//</editor-fold>

    /**
     * 正規表示式，判斷字串是否含英文數字(one、two...)且忽略大小寫
     * @param str
     * @return 
     */
    public boolean isEnglishNumber(String str) {//<editor-fold defaultstate="collapsed" desc="...">
        String[] enNumber_a = {"one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen"
                ,"fourteen","fifteen","sixteen","seventeen","eighteen","nineteen","twenty","thirty","forty"};
        ArrayList<String> enNumber_al = new ArrayList<>();
        for (String s : enNumber_a) {
            enNumber_al.add(s);
        }
//        if(enNumber_al.contains(str.split(",")[0].trim().toLowerCase())){
//            System.out.println("包含數字 : " + str.split(",")[0].trim());
//            return true;
//        }else{
//            System.out.println("不包含數字 : " + str.split(",")[0].trim());
//            return false;
//        }
        // 例外數字
//        if("NONETEEN".equals(str.split(",")[0].trim().toLowerCase())){
//            return true;
//        }
        // 邏輯區
        for (int i = 0 ; i < enNumber_a.length ; i++) {
//            System.out.println(str.split(",")[0].trim().toLowerCase() + "," + enNumber_a[i]);
            if(i <= 18){
                if(str.split(",")[0].trim().toLowerCase().contains(enNumber_a[i]) && str.split(",")[0].trim().toLowerCase().length() == enNumber_a[i].length()){
                    // 輸入源如果包含數字且字串成度等於數字長度，回傳正確
                    return true;
                }
            }else{
                if(str.split(",")[0].trim().toLowerCase().contains(enNumber_a[i])){
                    // 輸入源如果包含數字且字串成度等於數字長度，回傳正確
                    return true;
                }
            }
        }
        return false;
    }//</editor-fold>

    /**
     * 英文數字轉阿拉伯數字(one --> 1)
     * @param enNumber
     * @return 
     */
    public String numberChange(String enNumber) {//<editor-fold defaultstate="collapsed" desc="...">
        HashMap<String, Integer> hm = new HashMap<>();
        String[] numberList = {"zero","one","two","three","four","five","six","seven", "eight", "nine","ten",
            "eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen","twenty",
            "thirty", "forty"};
        // 考慮 0 ~ 40 號颱風
        for(int i = 0 ; i < numberList.length ; i++){
            hm.put(numberList[i], i);
        }
        String[] tempNumber = enNumber.split("-");
//        System.out.println(Arrays.toString(tempNumber));
        int onlyNumber = 0;
        for(String s : tempNumber){
//            System.out.println(enNumber + "," + s);
            // 修改 MSC 的詭異名稱
            switch (s.toLowerCase()) {
                case "on":
                case "o":
                    s = "one";
                    break;
                case "tw":
                    s = "two";
                    break;
                case "th":
                    s = "three";
                    break;
                case "fo":
                    s = "four";
                    break;
                case "fi":
                    s = "five";
                    break;
                case "si":
                    s = "six";
                    break;
                case "se":
                    s = "seven";
                    break;
                case "ei":
                    s = "eight";
                    break;
                case "ni":
                    s = "nine";
                    break;
                case "twentyone":
                    // 修改 MSC 的颱風編號不符合定義時 2013-10-03
                    return "21";
                case "twentytwo":
                    return "22";
                case "twentythr":
                case "twentythre":
                    return "23";
                case "twentyfou":
                case "twentyfour":
                    return "24";
                case "twentyfiv":
                case "twentyfive":
                    return "25";
                case "twentysix":
                    return "26";
                case "twentysev":
                case "twentyseve":
                    return "27";
                case "twentyeig":
                    return "28";
                case "twentynin":
                    return "29";
                case "thirty":
                    return "30";
                case "thirtyone":
                    // 修改 MSC 的颱風編號不符合定義時
                    return "31";
                case "thirtytwo":
                    // 修改 MSC 的颱風編號不符合定義時
                    return "32";
                case "thirtythr":
                case "thirtythre":
                    // 修改 MSC 的颱風編號不符合定義時
                    return "33";
                case "thirtyfou":
                case "thirtyfour":
                    return "34";
                case "thirtyfiv":
                case "thirtyfive":
                    return "35";
                default:
                    break;
            }
            onlyNumber += hm.get(s.toLowerCase());
        }
//        System.out.println(onlyNumber);
        return onlyNumber + "";
    }//</editor-fold>
}
