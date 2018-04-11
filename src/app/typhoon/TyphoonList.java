package app.typhoon;

//<editor-fold defaultstate="collapsed" desc="import...">
import app.itf.ITF_DB;
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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//</editor-fold>

public class TyphoonList implements ITF_DB {
    
    private String url;
    private String whereToDownload; 
    
    /**
     * 初始設定
     */
    public TyphoonList() {
        Properties prop = new Properties();
        try {
            prop.load(new FileReader(tywebProp));
            url = prop.getProperty("cwb_typhoon_list_url");
            whereToDownload = prop.getProperty("cwb_typhoon_list_path");
        } catch (IOException ex) {
            ex.printStackTrace();
        }        
    }
    
    /**
     * 抓氣象局颱風資料庫資料
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
        }
    }//</editor-fold>

    /**
     * 公開使用：擷取氣象局颱風編號
     * @param tigge_typhoon_name
     * @param setYear
     * @return 颱風編號(年份 + 編號)
     */
    public int getCWBTyNumber(String tigge_typhoon_name , int setYear) {//<editor-fold defaultstate="collapsed" desc="...">
        String url = whereToDownload + "tylist.html";
        int tyNumber = 0;
        File file = new File(url);
        try {
            Document doc = Jsoup.parse(file , "UTF-8");
            Elements elms_tr = doc.select("div").select("table").select("tbody").select("tr");
            for(Element elm_tr : elms_tr){
                String typhoonWarningList = elm_tr.text();
                String typhoonYear = typhoonWarningList.split("\\s+")[0];
                if(isNumeric(typhoonYear)){
                    int year = Integer.parseInt(typhoonYear);
                    tyNumber = Integer.parseInt(typhoonWarningList.split("\\s+")[1]);
                    String typhoon_name_tw = typhoonWarningList.split("\\s+")[2].trim();
                    String typhoon_name_en = typhoonWarningList.split("\\s+")[3].trim();     
                    String output = String.format("%d,%s,%s,%s" , year , tyNumber , typhoon_name_tw , typhoon_name_en);
                    if(year == setYear){
//                        if(tigge_typhoon_name.equals(typhoon_name_en)){// 原本寫法
                        if(typhoon_name_en.contains(tigge_typhoon_name)){
//                            System.out.println(output);
                            return tyNumber;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("**Error**");
            System.err.println("ParseCode > getCWBTyphoonNumber : " + ex.toString());
        }
        // 沒此颱風名稱時，回傳 -999
        return -999;
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
            if(s.toLowerCase().equals("on") || s.toLowerCase().equals("o")){
                s = "one";
            }else if(s.toLowerCase().equals("tw")){
                s = "two";
            }else if(s.toLowerCase().equals("th")){
                s = "three";
            }else if(s.toLowerCase().equals("fo")){
                s = "four";
            }else if(s.toLowerCase().equals("fi")){
                s = "five";
            }else if(s.toLowerCase().equals("si")){
                s = "six";
            }else if(s.toLowerCase().equals("se")){
                s = "seven";
            }else if(s.toLowerCase().equals("ei")){
                s = "eight";
            }else if(s.toLowerCase().equals("ni")){
                s = "nine";
            }else if(s.toLowerCase().equals("twentyone")){
                // 修改 MSC 的颱風編號不符合定義時 2013-10-03
                return "21";
            }else if(s.toLowerCase().equals("twentytwo")){
                return "22"; 
            }else if(s.toLowerCase().equals("twentythr") || s.toLowerCase().equals("twentythre")){
                return "23";   
            }else if(s.toLowerCase().equals("twentyfou") || s.toLowerCase().equals("twentyfour")){
                return "24";   
            }else if(s.toLowerCase().equals("twentyfiv") || s.toLowerCase().equals("twentyfive")){
                return "25";
            }else if(s.toLowerCase().equals("twentysix")){
                return "26";
            }else if(s.toLowerCase().equals("twentysev") || s.toLowerCase().equals("twentyseve")){
                return "27";
            }else if(s.toLowerCase().equals("twentyeig")){
                return "28";
            }else if(s.toLowerCase().equals("twentynin")){
                return "29";
            }else if(s.toLowerCase().equals("thirty")){
                return "30";
            }else if(s.toLowerCase().equals("thirtyone")){
                // 修改 MSC 的颱風編號不符合定義時
                return "31";
            }
            onlyNumber += hm.get(s.toLowerCase());
        }
//        System.out.println(onlyNumber);
        return onlyNumber + "";
    }//</editor-fold>
    
}
