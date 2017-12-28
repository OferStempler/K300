package il.co.boj.k300.model.entities;

/**
 * Created by ofer on 28/12/17.
 */
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "FILEUPLOADTABLE", indexes = {@Index(name = "uploaded", columnList = "Uploaded")})
@Data
public class FileUpload   {


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "ID")
    private String id;

    @Column(name = "FILE_GUID")
    private String fileGuid;


    @Column(name = "FILE_UPLOADREQUEST_DATE")
    private String fileUploadRequestDate;

    @Column(name = "CHANNEL_REQUEST_NAME")
    private String channelRequestName;

    @Column(name = "GET_FILE_GUID_IP")
    private String getFileGuidIP;

    @Column(name = "FILE_TYPE")
    private String filetype;

    @Column(name = "UPLOADED")
    private String uploaded;

    @Column(name = "SANITIZE_STATUS")
    private String sanitizeStatus;

    @Column(name = "RETURN_CODE")
    private int returnCode;

    @Column(name = "FILE_UPLOAD_SAVING_DATE")
    private String fileUploadSavingDate;

    @Column(name = "UPLOAD_FILE_IP")
    private String uploadFileIP;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "FILE_CONTENT")
    private byte[] fileContent;

    @Column(name = "EXTRA_INFO")
    private String extraInfo;


}
