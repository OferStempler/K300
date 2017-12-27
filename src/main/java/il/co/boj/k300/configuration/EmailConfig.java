package il.co.boj.k300.configuration;

/**
 * Created by ofer on 27/12/17.
 */

        import org.springframework.boot.context.properties.ConfigurationProperties;
        import org.springframework.context.annotation.Configuration;
        import lombok.Data;

@Data
@Configuration
@ConfigurationProperties (prefix = "email")
public class EmailConfig {

    private String userName;
    private String password;
    private String host;
    private String port;
    private String toAddress;
    private String subject;
    private String message;

    private String enableTestEmail;
    private String saveFileTo;





}