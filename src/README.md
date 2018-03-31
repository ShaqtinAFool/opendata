<!-- MarkdownTOC -->

- [NF_TyphoonList](#nftyphoonlist)
- [NF_RainData](#nfraindata)

<!-- /MarkdownTOC -->

---

# NF_TyphoonList
颱風資料正規化  
- downloadTigge(int timeout): 下載 tigge XML
- parseTigge(String url): 解析 tigge XML
- setNameAndCentre(): 正規化颱風清單和單位清單
- setXXXInfo(): 正規化 Info
- setXXXContent(): 正規化 Content
- main(String[] args):
	- NF_TyphoonList nf = new NF_TyphoonList();// 主要專案
	- FV fv = new FV();// 走訪目錄
	- TyphoonList tl = new TyphoonList();// 下載颱風清單
		- fore
			- nf.parseTigge((String) url);
			- nf.setNameAndCentre();
			- nf.setXXXInfo();
			- nf.setXXXContent();  

---

# NF_RainData
降雨資料正規化  
- enableSSLSocket(): 處理 SSL 無法用 Jsoup 問題
- setStnAddress(): 取得測站地址
        - 目前已收集 CWB, WRA
- parseData(): 解析讀進來的資料
- main(String[] args):
	- NF_RainData nf = new NF_RainData();
	- nf.enableSSLSocket();
	- nf.setStnAddress();
	- nf.parseData();