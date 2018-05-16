<!-- MarkdownTOC -->

- 資料庫設定
- 降雨資料正規化
- 颱風資料正規化

<!-- /MarkdownTOC -->

---

# 資料庫設定
- DBSetting
- app
    - db
        - DBSetting implements Itf_Prop, Itf_Database
            - +setDBInfo() → 設定 db 資訊
            - #getReturnValue(input:String): String → 取得連接資料庫的設定
            - +getConn(): Connection → 回傳資料庫連線資訊
        - DBSettingEnum
            - by10Rain
            - byTyphoon
            - byStation

# 降雨資料正規化
- NF_RainData
- NF_HistoryData
- app
    - station
        - StationData extends DBSetting
            - -setStnAddress() → 取得測站地址
                   - 目前已收集 CWB, WRA, CAA
            - +parseOpendataRain() → 解析 opendata 資料 (real time)
            - +parseCWB(url:String) → 解析 qc 過後資料 (from cwb 第三組)
            - +parseCAA(url:String) → 解析民航局資料

- lib
    - +enableSSLSocket() → 處理 SSL 無法用 Jsoup 問題

---

# 颱風資料正規化
- NF_TyphoonList
- app
    - typhoon
        - TyphoonList implements Itf_Prop: 建立颱風清單
            - +getCWBTyListToHtml() → 抓氣象局颱風資料庫資料，產出 html 檔案
            - +getCWBTyNumber(ty_name:String, setYear:int): int → 擷取氣象局颱風編號
            - +getCWBTyEnName(setYear:int): ArrayList<String> → 擷取氣象局颱風英文名稱
            - +getCWBTyTWName(setTyNumber:int): String → 擷取氣象局颱風中文名稱
            - +isNumeric(str:String): boolean → 判斷字串是否是數字
            - +isEnglishNumber(str:String): boolean → 判斷字串是否含英文數字(one、two...)且忽略大小寫
            - +numberChange(enNumber:String): String → 英文數字轉阿拉伯數字(one --> 1)
        - TyphoonData extends DBSetting
            - -setProperty() → 設定 property
            - +downloadData(xmlFile:String, xmlURL:String, createDirPath:String) → 下載 xml
            - +getTigge(timeout:int, dt_enum:DownloadTypeEnum) → 下載 tigge XML (目前隱性建議，用 shell 下載或許較好)
            - +moveFile(sourceURL:String, destinationURL:String) → 搬移 xml
            - +deleteTempDirectory(url:String) → 刪除 tigge 資料夾 by 遞回
            - +parseTigge(url:String) → 解析 tigge XML
            - +parseCWBTrack(url:String) → 解析 CWB Best Track
            - +parseTy2000() → 解析 typhoon 2000
            - +parseJTWC(url:String) → 解析 JTWC
            - +parseCWBWEPS(url:String) → 解析CWB WEPS
            - +setRunOrNotRun(parameter:String): boolean → 是否跑
            - +setNameAndCentre() → 正規化颱風清單和單位清單
            - +importParsedInfo() → 正規化 Info
            - +importParsedRawdata() → 正規化 Content
        - DownloadTypeEnum
            - byHistory
            - byRealtime
            - byStation