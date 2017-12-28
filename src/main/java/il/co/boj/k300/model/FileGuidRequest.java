package il.co.boj.k300.model;

/**
 * Created by ofer on 28/12/17.
 */
import lombok.Data;

@Data
public class FileGuidRequest {

    private String ip;
    private String channelRequestName;
    private Integer isLimited;
}
