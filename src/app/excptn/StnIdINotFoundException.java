package app.excptn;

/**
 *
 * @author tony
 */
public class StnIdINotFoundException extends Exception {
    
    /**
     * 
     * @param stationId 
     */
    public StnIdINotFoundException(String stationId) {
        super(stationId + " can not found!!");
    }
}
