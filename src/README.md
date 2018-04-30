<!-- MarkdownTOC -->

- DBSetting
- NF_RainData
    - 降雨資料正規化
- NF_TyphoonList
    - 颱風資料正規化

<!-- /MarkdownTOC -->

---
# DBSetting
- app
    - db
        - setDBInfo(): 設定 db 資訊
        - getReturnValue(String input): 取得連接資料庫的設定
        - getConn(): 回傳資料庫連線資訊

# NF_RainData
## 降雨資料正規化  
- app
    - rain
        - RainData extends DBSetting
            - setStnAddress(): 取得測站地址
                   - 目前已收集 CWB, WRA    
            - parseData(): 解析讀進來的資料
- lib
    - enableSSLSocket(): 處理 SSL 無法用 Jsoup 問題

---

# NF_TyphoonList
## 颱風資料正規化  
- app
    - typhoon
        - TyphoonList implements Itf_Prop: 建立颱風清單
            - getCWBTyListToHtml(): 抓氣象局颱風資料庫資料
            - getCWBTyNumber(String tigge_typhoon_name , int setYear): 擷取氣象局颱風編號
            - isNumeric(String str)
            - isEnglishNumber(String str)
            - numberChange(String enNumber)
        - TyphoonData extends DBSetting
            - setProperty(): 設定 property
            - getTigge(int timeout, DownloadTypeEnum dt_enum): 下載 tigge XML (目前隱性建議，用 shell 下載或許較好)
            - downloadData(String xmlFile, String xmlURL, String createDirPath): 下載 xml
            - parseTigge(String url): 解析 tigge XML
            - setNameAndCentre(): 正規化颱風清單和單位清單
            - setXXXInfo(): 正規化 Info
            - setXXXContent(): 正規化 Content
        - DownloadTypeEnum
            - byHistory
            - byRealtime