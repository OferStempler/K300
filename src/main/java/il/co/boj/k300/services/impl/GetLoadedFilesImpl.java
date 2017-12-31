package il.co.boj.k300.services.impl;

/**
 * Created by ofer on 31/12/17.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import il.co.boj.k300.configuration.K300Config;
import il.co.boj.k300.dao.AuditRepository;
import il.co.boj.k300.dao.FileUploadRepository;
import il.co.boj.k300.model.Response;
import il.co.boj.k300.model.entities.Audit;
import il.co.boj.k300.model.entities.FileUpload;
import il.co.boj.k300.services.EmailService;
import il.co.boj.k300.services.GetLoadedFiles;
import il.co.boj.k300.services.SanitizeService;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j;

@Service
@Transactional
@Log4j
public class GetLoadedFilesImpl implements GetLoadedFiles {

    @Autowired
    FileUploadRepository fileUploadRepo;

    @Autowired
    K300Config config;

    @Autowired
    AuditRepository auditRepo;

    @Autowired
    EmailService emailService;

    @Autowired
    SanitizeService sanitizeService;

    @Override
    public void getUploadedFiles() {


        ArrayList<FileUpload> listOfFiles;
        log.trace(" **TRACE** Selecting from FILEUPLOADTABLE table. Time:  " + new Date());
        listOfFiles = fileUploadRepo.findByUploaded("R");
        log.trace("**TRACE**  FOUND:[" + listOfFiles.size() + "] ROWS IN FILEUPLOADTABLE table");

        if (listOfFiles.size() > 0 ){
            log.debug("************************************************* Start getUploadedFiles ******************************************************");

            HashMap<String, String> mapResponse = new HashMap<String, String>();
            boolean isSigned = false;
            log.debug("GetFilesImpl.getUploadImages() - FOUND:[" + listOfFiles.size() + "] ROWS IN FILEUPLOADTABLE table");



            for (int i = 0; i < listOfFiles.size(); i++) {
                Response response = new Response();
                FileUpload currentFileUpload = listOfFiles.get(i);
                String guid = currentFileUpload.getFileGuid();
                String filePath = "";
                String fileType = currentFileUpload.getFiletype();
                File dir = new File(config.getSaveFilePath());
                boolean isPdf =false;
                for (File file : dir.listFiles()) {
                    log.debug("GetFilesImpl.getUploadImages() - deleting file:[" + file.getName() + "]");
                    file.delete();
                }

                byte[] currentFile = currentFileUpload.getFileContent();
                String[] args = new String[3]; //?
                String sanitizeFolder = config.getSanitizeFolder();
                String sanitize_ip = config.getSanitizeServer();


                log.debug("Type of current file is:  [" + fileType + "]");
                args[0] = sanitize_ip;
                File fileSource = new File(sanitizeFolder + currentFileUpload.getFileGuid() + "Source." + currentFileUpload.getFiletype());

                args[1] = sanitizeFolder + currentFileUpload.getFileGuid() + "Source." + currentFileUpload.getFiletype();
                args[2] = sanitizeFolder + currentFileUpload.getFileGuid() + "Dest."   + currentFileUpload.getFiletype();

                log.debug("Saving file to:[" + fileSource + "]");
                try {
                    fileSource.createNewFile();
                    FileOutputStream outputStream = new FileOutputStream(fileSource);
                    outputStream.write(currentFile);
                    outputStream.close();

                } catch (Exception e) {
                    String error = "File write Failed. ";
                    log.error(error +  e);
                    setResponse(false, error, guid, 999, 0);
                }


                /// check if image is a digitally signed pdf file


                log.debug("Checking if file is pdf");

                if (fileType.toLowerCase().equals("pdf")) {
                    isPdf = true;
                    log.debug("Found pdf file - sending for digital signature check");

                    filePath = fileSource.getAbsolutePath();
                    isSigned = isDigitalSigned(filePath);
                    if (!isSigned) {
                        log.debug("Did not find any siganture on pdf file. Starting sanitation.");
                    } else {
                        // if the document has a digital signature, send to
                        // email
                        String[] attachFiles = new String[1];
                        attachFiles[0] = filePath;

                        boolean send = emailService.sendEmailWithAttachments(attachFiles);

                        try {
                            if (send) {
                                currentFileUpload.setUploaded("D");
                                log.debug("Successfully sent email. Updating GUID:[" + currentFileUpload.getFileGuid()
                                        + "] in FILE_UPLOAD_TABLE table to 'D'");
                                fileUploadRepo.save(currentFileUpload);
                                log.debug("Updated Uploaded status to D");
                                log.debug("Seleting file:[" + fileSource.getName() + "]");
                                fileSource.delete();

                                Audit audit = auditRepo.findByFileGuid(currentFileUpload.getFileGuid());
                                audit.setSanitizeStatus("0");
                                audit.setSanitizeStatusDescription("Sent to Email success");
                                audit.setSanitizeProcessStatus("NO SANITIZATION");
                                auditRepo.save(audit);

                            } else {
                                log.debug("Failed sending file to Email. Updating GUID:["
                                        + currentFileUpload.getFileGuid() + "] in FILE_UPLOAD_TABLE table to 'F'");
                                currentFileUpload.setUploaded("F");
                                fileUploadRepo.save(currentFileUpload);
                                fileSource.delete();
                                log.debug("Updated Uploaded status to F");
                                log.debug("Seleting file:[" + fileSource.getName() + "]");

                                Audit audit = auditRepo.findByFileGuid(currentFileUpload.getFileGuid());
                                audit.setSanitizeStatus("2");
                                audit.setSanitizeStatusDescription("Sent to Email failed");
                                audit.setSanitizeProcessStatus("NO SANITIZATION");
                                auditRepo.save(audit);
                            }

                        } catch (Exception e) {
                            String error = "Could not save new Uploaded status to db";
                            log.error(error + e);
                            setResponse(false, error, guid, 999, 0);
                        }

                    } // fileIsSigned
                } // file is pdf
                if(!isPdf){
                    log.debug("File is not a pdf file. Sending to sanitation.");
                }
                log.debug("PARMETER To SANITIZE: 1. fileType =["+ currentFileUpload.getFiletype()+ "] 2. Source path  = ["+ args[1] + "] 3. Dest path  = [" + args[2] + "]");


                Long beforeSanitize = System.currentTimeMillis();
                mapResponse = sanitizeService.sanitize(args);
                Long afterSanitize = System.currentTimeMillis();
                log.debug("After Sanitation. Time took [" + ((afterSanitize-beforeSanitize)/1000) + "] seconds");
                if(mapResponse == null){
                    String error = "Could not sanitize";
                    log.error(error );
                    setResponse(false, error, guid, 999, 0);
                }

                String Status = mapResponse.get("Status");
                String error = mapResponse.get("ErrorFile");
                String Message = mapResponse.get("Message");
                String status = mapResponse.get("Status");
                log.debug("Sanitation parameters returned: "
                        + "Status=[" + Status + "], "
                        + "error=[" + error + "], "
                        + "Message=[" + Message + "]" );
                String sanitInd = config.getSanitizeFlag();
                if (sanitInd.equals("false")) {
                    status = "0";
                }

                if (status.equals("1") || status.equals("2")) { // Sanitize status
                    log.debug(" File is blocked or failed ");
                    currentFileUpload = editExtraInfo(currentFileUpload, status, Message);
                    reportFile(currentFileUpload, mapResponse, status);
                } else {
                    try {

                        log.debug("File is OK ^^ ************");
                        String statusMessage = mapResponse.get(" ");
                        String statusCode = mapResponse.get("Status");
                        String proccesStatus = mapResponse.get("ProccesStatus");
                        String typee = mapResponse.get("SanitizeProccesType");
                        String name = mapResponse.get("SanitizeProccesName");
                        String id = mapResponse.get("SanitizeID");
                        String path1 = config.getSaveFilePath();
                        String reportPath = path1 + currentFileUpload.getFileGuid() + "Report.txt";
                        log.debug("GetFilesImpl.getUploadImages() - new PrintWriter with reportPath:[" + reportPath + "]");
                        PrintWriter writer = new PrintWriter(reportPath , "UTF-8");
                        String description = "";
                        description = "GetFilesImpl.getUploadImages() - status = " + statusCode + "message = "
                                + statusMessage + "reportStatus =" + proccesStatus
                                + "report reasonType =" + typee
                                + "reportreason Name =" + name;
                        log.debug("GetFilesImpl.getUploadImages() - status = [" + statusCode + "]  message = ["
                                + statusMessage + "] reportStatus = ["
                                + proccesStatus + " ]report reasonType = [" + typee
                                + " ] reportreason Name = [" + name + "]");
                        writer.println("status = " + statusCode);
                        // writer.println("message = "+statusMessage);
                        writer.println("reportStatus =" + proccesStatus);
                        writer.println("report reasonType =" + typee);
                        writer.println("reportreason Name =" + name);
                        writer.close();
                        FileInputStream fileInStream = new FileInputStream(args[2]);

                        byte[] buff2 = new byte[fileInStream.available()];
                        fileInStream.read(buff2);
                        currentFileUpload.setSanitizeStatus(Status);
                        //FileInputStream failFile = new FileInputStream(path1 + currentFileUpload.getFileguid() + "Report.txt");
                        log.debug("GetFilesImpl.getUploadImages() - reading from failFile:[" + reportPath + "]");
                        FileInputStream failFile = new FileInputStream( reportPath );
                        byte[] failFilebyte = new byte[failFile.available()];
                        failFile.read(failFilebyte);
                        // description= encodeImage(failFilebyte);
                        log.debug("GetFilesImpl.getUploadImages() - status = " + statusCode + "message = "
                                + statusMessage + "reportStatus =" + proccesStatus
                                + "report reasonType =" + typee
                                + "reportreason Name =" + name);
                        failFile.close();

                        currentFileUpload = editExtraInfo(currentFileUpload, status, Message);

                        if (sanitInd.equals("false")) {
                            log.debug("SANITIZE  Failed, calling ESB setUploadFile()");
                            setUploadFile(currentFileUpload, currentFileUpload.getFileContent(), description);
                        } else {
                            log.debug("SANITIZE  Succeeded, calling ESB setUploadFile()");
                            setUploadFile(currentFileUpload, buff2, description);
                        }
                        currentFileUpload.setUploaded("D");
                        log.debug("GetFilesImpl.getUploadImages() - updating GUID:[" + currentFileUpload.getFileGuid() + "] in FILEUPLOADTABLE table to 'D'");
                        fileUploadRepo.save(currentFileUpload);
                        Audit audit = auditRepo.findByFileGuid(currentFileUpload.getFileGuid());
                        audit.setSanitizeStatus(statusCode);
                        audit.setSanitizeStatusDescription(description);
                        audit.setSanitizeID(id);
                        audit.setSanitizeProcessStatus(proccesStatus);
                        audit.setSanitizeProcessType(typee);
                        audit.setSanitizeProcessName(name);
                        auditRepo.save(audit);
                        fileInStream.close();
                        fileInStream.close();
                    } catch (Throwable e) {
                        log.error("GetFilesImpl.getUploadImages() - Failed the message  : " + e.getMessage(), e);
                    }
                }//else
                log.debug("GetFilesImpl.getUploadImages() - deleting file:[" + fileSource.getName() + "]");
                fileSource.delete();
                log.debug("GetFilesImpl.getUploadImages() - ****************** End getUploadImages  GetFilesImpl ***********************************");
            }//to sanitize
        }//for

    }//if  listOfFiles.size() > 0


//--------------------------------------------------------------------------------------------------------------


    private Response setResponse(Boolean isSuccess, String details, String guid, int errorCode, int SanitizeCode ){
        // errorCode and SanitizeCode are 0 for null
        Response response = new Response();
        response.setDetails(details);
        response.setErrorCode(errorCode);
        response.setGuid(guid);
        response.setIsSuccess(isSuccess);
        response.setSanitizeCode(SanitizeCode);


        return response;
    }
    //--------------------------------------------------------------------------------------------------------------
    private Response creatAudit(String blocked, String ip, String serviceName, int returnCode, int SanitizeCode, String channelRequestName, String userAgent, boolean isSuccess, String guid, String error){

        Response response = new Response();
        Audit audit = new Audit();

        SimpleDateFormat dateFormat = new SimpleDateFormat(	"yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String date2DB = dateFormat.format(date);
        try {
            audit.setBlocked(blocked);
            audit.setIp(ip);
            audit.setServiceName(serviceName);
            audit.setRequestDate(date2DB);
            audit.setReturnCode(returnCode);
            audit.setChannelRequestName(channelRequestName);
            audit.setFileGuid(guid);
            audit.setUserAgent(userAgent);
            auditRepo.save(audit);
            log.debug("Successfully saved to Audit table.");
        } catch (Exception e){
            log.error("Could not save audt to database. " + e );
        }

        response = setResponse(isSuccess, error, guid, returnCode, SanitizeCode);
        return response;
    }
    //--------------------------------------------------------------------------------------------------------------
    public boolean isDigitalSigned(String filePath)  {
        try {
            boolean isSigned = false;
            //need the pdsfOne jar....
            PdfDocument doc = new PdfDocument();
            doc.load( new File(filePath) );
            List<Object> list = doc.getAllFormFields();
            if ( list == null || list.size() == 0 ){
                return false;
            }
            for (Object object : list) {
                if ( object instanceof PdfFormSignatureField ){
                    log.debug("Found signatute on pdf file: [" +((PdfFormSignatureField)object).getTitle() +"]");
                    isSigned = true;
                }
            }
            doc.close();
            return isSigned;

        }catch (Exception e){
            log.error("Could not figure if digitally signed. " + e);
            return false;
        }
    }
    //--------------------------------------------------------------------------------------------------------------

    private void reportFile(FileUpload fileUpload,
                            HashMap<String, String> mapResponse, String status) {
        try {

            log.debug("THE SANITIZE FAILED  CREATING TXT FILE reportFile AND SENDING TO ESB");
            String statusCode = mapResponse.get("Status");
            String proccesStatus = mapResponse.get("ProccesStatus");
            String typee = mapResponse.get("SanitizeProccesType");
            String name = mapResponse.get("SanitizeProccesName");
            String path = config.getSanitizeFolder();
            PrintWriter writer = new PrintWriter(path+ fileUpload.getFileGuid() + "Report.txt", "UTF-8");
            String description = "";
            if (status.equals("1")) {
                writer.println("תקלה בהלבנת קובץ");
            } else {
                writer.println("קובץ חסום - תקלה בהלבנת הקובץ");
            }
            log.debug("status = [" + statusCode + "]  reportStatus = ["
                    + proccesStatus + " ]report reasonType = [" + typee
                    + " ] reportreason Name = [" + name + "]");
            writer.println("status = " + statusCode);
            writer.println("reportStatus =" + proccesStatus);
            writer.println("report reasonType =" + typee);
            writer.println("reportreason Name =" + name);
            fileUpload.setSanitizeStatus(statusCode);
            fileUpload.setFiletype("txt");
            fileUpload.setUploaded("F");
            byte[] empty = new byte[1];
            fileUpload.setFileContent(empty);
            fileUploadRepo.save(fileUpload);
            Audit audit = auditRepo.findByFileGuid(fileUpload.getFileGuid());
            audit.setSanitizeStatus(statusCode);
            auditRepo.save(audit);
            writer.close();
            FileInputStream failFile = new FileInputStream(path+ fileUpload.getFileGuid() + "Report.txt");
            byte[] failFilebyte = new byte[failFile.available()];
            failFile.read(failFilebyte);
            description = encodeImage(failFilebyte);
            setUploadFile(fileUpload, failFilebyte, description);
            failFile.close();
        } catch (Throwable e) {
            e.printStackTrace();
            log.error("ReportFile Failed" + e.getMessage(), e);
            e.printStackTrace();
        }

    }
    //--------------------------------------------------------------------------------------------------------------

    public  String encodeImage(byte[] imageByteArray) {
        return Base64.encodeBase64URLSafeString(imageByteArray);
    }
    //--------------------------------------------------------------------------------------------------------------

    private void setUploadFile(FileUpload fileUpload, byte[] filebyte, String description) {
        log.debug("GetFilesImpl.setUploadFile() - in. description:[" + description + "], File guild:[" + fileUpload.getFileGuid() + "]");
        try {
            String SetUploadFile = config.getSetUploadFile();
            log.debug("URL  TO ESB SetUploadFile =  [" + SetUploadFile + "]");
            java.net.URL endpointURL = new URL(SetUploadFile);
            SetUploadFileSOAPStub stub = new SetUploadFileSOAPStub(endpointURL, null);

            jerusalemService.ESB.com.SetUploadFile.RequestChannel requestChannel = new jerusalemService.ESB.com.SetUploadFile.RequestChannel();
            requestChannel.setHeader(buildHeader("SetUploadFile"));

            jerusalemService.ESB.com.SetUploadFile.ChannelInput channelInput = new jerusalemService.ESB.com.SetUploadFile.ChannelInput();
            SetUploadFileType setUploadFileType = new SetUploadFileType();
            jerusalemService.ESB.com.SetUploadFile.RequestType request = new RequestType();
            request.setFileGuid(fileUpload.getFileGuid());
            if (filebyte == null) {
                request.setAttachmentFile(fileUpload.getFileContent());
            } else {
                request.setAttachmentFile(filebyte);
            }
            request.setChannelRequestName(fileUpload.getChannelRequestName());
            request.setFileType(fileUpload.getFiletype());
            request.setFileGuid(fileUpload.getFileGuid());
            request.setFileUploadIP(fileUpload.getGetFileGuidIP());
            request.setSanitizeStatus(fileUpload.getSanitizeStatus());
            request.setSanitizeStatusDescription(description);
            request.setExtraInfo(fileUpload.getExtraInfo());
            setUploadFileType.setRequest(request);
            channelInput.setSetUploadFile(setUploadFileType);
            requestChannel.setChannel(channelInput);
            stub.setTimeout(120000);

            log.debug("GetFilesImpl.setUploadFile() - B4 SetUploadFile ESB. @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            ResponseChannel responseChannel = stub.excute(requestChannel);
            if (
                    responseChannel != null &&
                            responseChannel.getHeader() != null &&
                            responseChannel.getHeader().getResponseMessages() != null &&
                            responseChannel.getHeader().getResponseMessages().length > 0 &&
                            responseChannel.getHeader().getResponseMessages()[0] != null
                    ){
                log.debug("GetFilesImpl.setUploadFile() - ESB RESPONSE code:[" + responseChannel.getHeader().getResponseMessages()[0].getResponseCode() + "]");
                log.debug("GetFilesImpl.setUploadFile() - ESB RESPONSE desc:[" + responseChannel.getHeader().getResponseMessages()[0].getMessageText() + "]");
            }else{
                log.debug("GetFilesImpl.setUploadFile() - NULL ESB RESPONSE");
            }

            log.debug("GetFilesImpl.setUploadFile() - AFTER SetUploadFile ESB @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ ");
            log.debug("---------------------------------------------------------------------------");
        } catch (Throwable e1) {
            e1.printStackTrace();
            log.error(" Error get message:[" + e1.getMessage() + "]", e1);

        }
    }

    private FileUpload editExtraInfo(FileUpload currentFileUpload, String sanitizeCode, String sanitizeDescription) {

        try {
            if (currentFileUpload.getChannelRequestName().equals("MRG_WEB")) {
                log.debug("Channel request name is MRG_WEB, edditing extraiInfo");
                String extraInfo = currentFileUpload.getExtraInfo();
                if (extraInfo != null && !extraInfo.equals("")) {
                    //for sanitizeCode = 0 - sanitizeDescription with ""
                    if (sanitizeCode.equals("0")) {
                        extraInfo = extraInfo.substring(0, extraInfo.lastIndexOf("}")) + ", \"sanitizeCode\": " + "\""
                                + sanitizeCode + "\"" + ", \"sanitizeDesc\": " + "\"" + sanitizeDescription + "\""
                                + "}";
                    } else {
                        //for sanitizeCode != 0 - sanitizeDescription no ""
                        extraInfo = extraInfo.substring(0, extraInfo.lastIndexOf("}")) + ", \"sanitizeCode\": " + "\""
                                + sanitizeCode + "\"" + ", \"sanitizeDesc\": " + sanitizeDescription + "}";
                    }
                    log.debug("Returning new extraInfo: [" + extraInfo + "]");
                    currentFileUpload.setExtraInfo(extraInfo);
                } else {
                    log.error("extraInfo is null or empty. Rerurning it with out changes");
                }
            }
        } catch (Exception e){
            log.error("Could not change extraInfo. + " + e);
        }
        return currentFileUpload;
    }

    protected Header buildHeader(String serviceName) {
        Header header = new Header();
        // ---- TimeStamp ---------
        Timestamp ts = new Timestamp();
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String dateStr = sdf.format(d);
        ts.setRequestTimestamp(dateStr); // Calendar.getInstance().toString());
        ts.setRequestTimeout(new BigInteger("50"));
        ts.setTimeZone("");
        header.setTimestamp(ts);
        // ---- ServiceInfo ---------
        ServiceInfo si = new ServiceInfo();
        si.setServiceName(serviceName);
        si.setSubService("s");
        si.setStep("0");
        si.setPriority(new BigInteger("0"));
        si.setMessageId("1");
        si.setFrom("192.25.0.12");
        si.setReplyTo("192.25.0.12");
        si.setFaultTo("192.25.0.12");
        si.setRelatedTo("192.25.0.12");
        si.setAction("1");
        header.setServiceInfo(si);
        // ---- PageInfo ---------
        PagingInfo pinf = new PagingInfo();
        pinf.setMaxNumberOfRecords(new BigInteger("50"));
        pinf.setLastSentRecordNumber(new BigInteger("0"));
        header.setPagingInfo(pinf);
        // ---- EnvironmentInfo ---------
        UUID trxID = UUID.randomUUID();
        EnvironmentInfo ei = new EnvironmentInfo();
        ei.setChannelName("K300");
        ei.setClientIP("192.168.125.1");
        ei.setApplicationServerIP("192.168.125.1");
        ei.setSessionId(trxID.toString().substring(0, 15));
        ei.setCoreSessionID("");
        ei.setTransactionId(trxID.toString().substring(0, 15));
        ei.setLanguage("HEB");
        ei.setVersionId("");
        ei.setDevice("");
        ei.setPlatform("");
        ei.setOS("");
        ei.setUserId("1"); // XR76581");
        ei.setUserTypeId("1");
        header.setEnvironmentInfo(ei);
        // ---- UserInfo ---------
        UserInfo ui = new UserInfo(); // "1", "1");
        ui.setIdType("1");
        ui.setIdNumber("066159344");
        header.setUserInfo(ui);
        // ---- AccountInfo ---------
        AccountInfo ai = new AccountInfo(); // "1", "1", "1");
        ai.setBankNumber("1");
        ai.setBranchNumber("50"); //
        ai.setAccountNumber("1"); // 200161870");
        header.setAccountInfo(ai);
        // ---- HEADER END ---------
        return header;
    }

}
