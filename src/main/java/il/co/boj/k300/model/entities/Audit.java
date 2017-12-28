package il.co.boj.k300.model.entities;

/**
 * Created by ofer on 28/12/17.
 */
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "AUDIT")
@Data
public class Audit   {


    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "ID")
    private String id;

    @Column(name = "IP")
    private String ip;

    @Column(name = "REQUEST_DATE")
    private String requestDate;
    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "FILE_GUID")
    private String fileGuid;
    @Column(name = "CHANNEL_REQUST_NAME")
    private String channelRequestName;

    @Column(name = "USER_AGENT")
    private String userAgent;
    @Column(name = "BLOCKED")
    private String blocked;
    @Column(name = "SANITIZE_STATUS")
    private String sanitizeStatus;

    @Column(name = "SANITIZE_STATUSE_DESCRIPTION")
    private String sanitizeStatusDescription;
    @Column(name = "SANITIZE_ID")
    private String sanitizeID;
    @Column(name = "SANITIZE_PROCESS_STATUS")
    private String sanitizeProcessStatus;
    @Column(name = "SANITIZE_PROCESS_TYPE")
    private String sanitizeProcessType;

    @Column(name = "SANITIZE_PROCESS_NAME")
    private String sanitizeProcessName;

    @Column(name = "RETURN_CODE")
    private int returnCode;




}