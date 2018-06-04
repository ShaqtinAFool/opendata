package app.webmodel;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import osi.presentation.SSL;

/**
 * htmlunit 設定
 * @author tony
 */
public class HtmlUnit {
    
    private String url;
    private HtmlPage humlPage;

    /**
     * 
     * @param url 網址
     */
    public HtmlUnit(String url) {
        this.url = url;
        // 啟動 HtmlPage
        setHtmlPage();
    }
    
    
    /**
     * 啟動 HtmlPage
     */
    private void setHtmlPage() {
        try {
            // 模擬瀏覽器
            WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);

            // 關閉 htmlunit warnings
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);
            // 啟動 SSL
            webClient.getOptions().setUseInsecureSSL(true);
            
            URL web = new URL(url);
            // 啟動頁面(homePage) ---> 放這裡會GG
            // com.gargoylesoftware.htmlunit.ScriptException: undefined (https://tpc.googlesyndication.com/pagead/js/r20170906/r20110914/abg.js#1)
            // 放這裡可以執行 WTF at 3/21
            humlPage = webClient.getPage(url);

            // 是否使用 js
            webClient.getOptions().setJavaScriptEnabled(true);
            // 是否使用 css
            webClient.getOptions().setCssEnabled(true);
            // 設置連接超時時間，這裡是 10 sec。如果為 0，則無限期等待
            webClient.getOptions().setTimeout(10000);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            // JS 運行錯誤時，是否抛出異常
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            // 當出現 Http error 時，程序不抛异常继续执行
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
            webClient.getOptions().setUseInsecureSSL(false);
            webClient.getOptions().setRedirectEnabled(false);
            webClient.getCookieManager().setCookiesEnabled(false);
            // 设置Ajax异步处理控制器即启用Ajax支持
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.waitForBackgroundJavaScript(10000);
//            webClient.waitForBackgroundJavaScriptStartingBefore(5000);
            // 啟動頁面(homePage)
            // Exception in thread "main" java.lang.NullPointerException
//            HtmlPage htmlPage = webClient.getPage(web);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * 輸出 HtmlPage
     * @return HtmlPage
     */
    public HtmlPage getHtmlPage() {
        return humlPage;
    }
}
