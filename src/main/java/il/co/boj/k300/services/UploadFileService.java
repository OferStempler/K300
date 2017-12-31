package il.co.boj.k300.services;

/**
 * Created by ofer on 31/12/17.
 */
import il.co.boj.k300.model.Response;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface UploadFileService {
    public Response getFileGuid(String ChannelRequestName, String ip, Integer isLimited, HttpServletRequest request);

    public Response loadFile(MultipartFile attachmentFile, String ip, String fileGuid, String synchronous, Integer isLimited, String extraInfo, HttpServletRequest request);


}