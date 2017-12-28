package il.co.boj.k300.model;

/**
 * Created by ofer on 28/12/17.
 */
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "result")
public class Response {

    private Boolean isSuccess;
    private String details;
    private String guid;
    private int errorCode;
    private int SanitizeCode;


    public int getSanitizeCode() {
        return SanitizeCode;
    }




    public void setSanitizeCode(int sanitizeCode) {
        SanitizeCode = sanitizeCode;
    }




    public int getErrorCode() {
        return errorCode;
    }




    public String getDetails() {
        return details;
    }
    @XmlElement(name = "details")
    public void setDetails(String details) {
        this.details = details;
    }
    public String getGuid() {
        return guid;
    }
    @XmlElement(name = "guid")
    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    @XmlElement(name = "isSuccess")
    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    @XmlElement(name = "errorCode")
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }



}
