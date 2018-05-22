import app.cases.CasesData;
import app.db.DBSettingEnum;

/**
 * 個案
 * @author tony
 */
public class NF_Cases {
    public static void main(String[] args) {
        CasesData c = new CasesData(DBSettingEnum.byCase);
        
        c.defineMeiyu();
        c.parseMeiyu();
        c.defineThunderstorm();
        c.parseThunderstorm();

    }
}
