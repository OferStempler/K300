package il.co.boj.k300.configuration;

/**
 * Created by ofer on 27/12/17.
 */
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties (prefix = "K300")
public class K300Config {

    private String maxIp;
    private String minIp;
    private String unlim;
    private String saveFilePath;
    private String fileSize;
    private String sanitizeFolder;
    private String sanitizeServer;
    public String wsDelay;
    public String statusCounter;
    public String sanitizeFlag;
    public String setUploadFile;
    public String allowedImageFormats;

}