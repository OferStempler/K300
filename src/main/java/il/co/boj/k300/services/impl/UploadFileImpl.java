package il.co.boj.k300.services.impl;

/**
 * Created by ofer on 31/12/17.
 */
import il.co.boj.k300.configuration.K300Config;
import il.co.boj.k300.dao.AuditRepository;
import il.co.boj.k300.dao.BlockedRepository;
import il.co.boj.k300.dao.FileUploadRepository;
import il.co.boj.k300.model.Response;
import il.co.boj.k300.model.entities.Audit;
import il.co.boj.k300.model.entities.Blocked;
import il.co.boj.k300.model.entities.FileUpload;
import il.co.boj.k300.services.UploadFileService;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
@Log4j
@Transactional
@Service
public class UploadFileImpl implements UploadFileService{

    @Autowired
    K300Config config;

    @Autowired
    BlockedRepository blockedRepository;

    @Autowired
    AuditRepository audotRepo;

    @Autowired
    FileUploadRepository fileUploadRepo;

    SimpleDateFormat dateFormat = new SimpleDateFormat(	"yyyy-MM-dd HH:mm:ss");

    final String IMAGE = "image";
    //	final String JPG = "jpg";
//	final String JPEG = "jpeg";
//	final String PNG = "png";
//	final String BMP = "bmp";
//	final String TIFF = "tif";
    final String PDF = "pdf";
    final String  APPLICATION= "application";

    //--------------------------------------------------------------------------------------------------------------
    @Override
    public Response getFileGuid(String channelRequestName, String ip, Integer isLimited, HttpServletRequest request) {

        Response response = new Response();
        String userAgent = request.getHeader("user-agent");
        String serviceName = "getFileGuid";
        //validating must params
        log.debug("Validating mandatory parameters");
        response = validateParams(channelRequestName, ip, userAgent, isLimited, request);
        if (!response.getIsSuccess()){
            return response;
        }
        log.debug("Successfully validated mandatory parameters");
        String guid = UUID.randomUUID().toString();
        int maxIp = Integer.parseInt(config.getMaxIp());
        int minIp = Integer.parseInt(config.getMinIp());

        //checking that ip is not already blocked
        log.debug("Checking if ip is blocked in Blocked table");
        Blocked blocked = blockedRepository.checkBlockedIp(ip, serviceName);
        if (blocked != null) {
            String error = "Ip is already blocked for Requsts from IP = [" + ip + "]";
            log.debug(error);
            response = creatAudit("Y", ip, serviceName, 101, 0, channelRequestName, userAgent, false, guid, error);
            return response;
        }


        log.debug("Ip is not blocked. Getting number of requests from  ip = [" + ip + "]");
        int numbersOfIp = -1;
        numbersOfIp = audotRepo.countBlockedIp(ip, serviceName);
        log.debug("Number of requests from same ip = [" + numbersOfIp + "]");


        //checking for is limited param
        if (isLimited !=null && isLimited == 1 )	{
            log.debug("Checking number of reqeusts for isLimitedParam = ["+isLimited+"]");
            Blocked blockedUnlim = blockedRepository.checkBlockedIp(ip, serviceName);

            int unlimParm = Integer.parseInt(config.getUnlim());
            log.debug("unlimParam  is ["+unlimParm+"].");
            if (numbersOfIp > unlimParm) {
                log.debug("numbersOfIp [" +numbersOfIp +"],  is bigger than unlimParm  [" +unlimParm +"].");

                if (blockedUnlim == null) {
                    String error = ". Blocking Ip for the first time";
                    log.debug(error);
                    response = blockAndSave(channelRequestName, ip, userAgent, response, serviceName, guid, error);
                    return response;
                } else {
                    String error = " Ip was already blocked";
                    log.debug(error);
                    response = blockAndSave(channelRequestName, ip, userAgent, response, serviceName, guid, error);
                    return response;
                }
            }
        }

        if (isLimited == null || isLimited != 1) {
            log.debug("isLimitedParam is = [" + isLimited + "]. Checking maxIp and minIp");
            if (numbersOfIp > maxIp) {
                log.debug("numbersOfIp [" + numbersOfIp + "] is bigger than max [" + maxIp + "]");
                Blocked blockedNotUnlim = blockedRepository.checkBlockedIp(ip, serviceName);
                if (blockedNotUnlim == null) {
                    String error = ". Blocking Ip for the first time";
                    log.debug(error);
                    response = blockAndSave(channelRequestName, ip, userAgent, response, serviceName, guid, error);
                    return response;
                } else {
                    String error = " Ip was already blocked";
                    log.debug(error);
                    response = blockAndSave(channelRequestName, ip, userAgent, response, serviceName, guid, error);
                    return response;
                }

            }  else if (numbersOfIp <= maxIp && numbersOfIp > minIp) {

                log.debug(" numbersOfIp [" + numbersOfIp + "] is between max [" + maxIp + "] and min [" + minIp + "]");

                boolean save = saveToFileUploadTable(guid, channelRequestName, ip, "G", 100);
                String error = "";
                response = creatAudit("N", ip, serviceName, 101, 0, channelRequestName, userAgent, save, guid, error);
                return response;
            } else {
                log.debug(" numbersOfIp [" + numbersOfIp + "] is less than min [" + minIp + "]. Getting file Guid");
                boolean save = saveToFileUploadTable(guid, channelRequestName, ip, "G", 100);
                String error = "";
                response = creatAudit("N", ip, serviceName, 100, 0, channelRequestName, userAgent, save, guid, error);
                return response;
            }
        }

        log.debug("Getting file Guid");
        boolean save = saveToFileUploadTable(guid, channelRequestName, ip, "G", 100);
        String error = "";
        response = creatAudit("N", ip, serviceName, 100, 0, channelRequestName, userAgent, save, guid, error);
        return response;

    }
    //--------------------------------------------------------------------------------------------------------------
    private boolean saveToFileUploadTable(String guid, String channelRequestName, String ip, String uploaded, int returnCode ){
        FileUpload fileUpload = new FileUpload();
        boolean saved = true;
        Date date = new Date();
        String date2DB = dateFormat.format(date);
        try {
            fileUpload.setFileGuid(guid);
            fileUpload.setFileUploadRequestDate(date2DB);
            fileUpload.setChannelRequestName(channelRequestName);
            fileUpload.setGetFileGuidIP(ip);
            fileUpload.setUploaded(uploaded);
            fileUpload.setReturnCode(returnCode);
            log.debug("Saving to fileUploadTable");
            fileUploadRepo.save(fileUpload);
            log.debug("SUCCESSFULLY saved file into fileUploadTable. fileGuid:[" + guid + "] load status: ["+uploaded+"]" );
        } catch (Exception e){
            log.error("Failed saving file into fileUploiadTable" + e);
            saved = false;
        }
        return saved;
    }
    //--------------------------------------------------------------------------------------------------------------
    private Response validateParams(String channelRequestName, String ip, String userAgent, Integer isLimited, HttpServletRequest request) {

        Response response = new Response();
        response.setIsSuccess(true);
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        log.debug(("Ip from X-FORWARDED-FOR: [" + ipAddress + "]"));
        if (ipAddress == null) {
            log.debug(("Ip Address From header is null, getting ip from remote adress"));
            ipAddress = request.getRemoteAddr();
            log.debug("Ip from remote address: [" + ipAddress + "]");
        }

        if (ipAddress == null ||ipAddress.equals("") ||channelRequestName == null ||channelRequestName.equals("") ) {
            String error = "One of the mandatory fields (ipAddress and channelRequestName) is null or empty";
            log.debug (error);
            response = setResponse(false, error, null, 104, 0);
            response.setDetails(error);
            response.setIsSuccess(Boolean.valueOf(false));
            return response;
        }
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
            audotRepo.save(audit);
            log.debug("Successfully saved to Audit table.");
        } catch (Exception e){
            log.error("Could not save audt to database. " + e );
        }

        response = setResponse(isSuccess, error, guid, returnCode, SanitizeCode);
        return response;
    }

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
    private Response blockAndSave(String channelRequestName, String ip,	String userAgent, Response response, String serviceName, String guid, String error){

        Date date = new Date();
        String date2DB = dateFormat.format(date);
        log.debug(error);

        Blocked blocked = new Blocked();
        blocked.setIp(ip);
        blocked.setUpdatedate(date2DB);
        blocked.setServiceName(serviceName);
        blocked.setIp(ip);
        blocked.setChannelRequestName(channelRequestName);
        blockedRepository.save(blocked);

        response = creatAudit("y", ip, serviceName, 101, 0, channelRequestName, userAgent, false, guid, error);
        return response;
    }

    //--------------------------------------------------------------------------------------------------------------
    @Override
    public Response loadFile(MultipartFile attachmentFile, String ip, String fileGuid, String synchronous , Integer isLimited, String extraInfo, HttpServletRequest request) {

        int guidCounter;
        int guidCounter24h;
        String fileSize = config.getFileSize();
        String serviceName = "UploadFile";
        String maxIp = config.getMaxIp();
        String userAgent = request.getHeader("user-agent");
        String fileSaveingPath= config.getSaveFilePath();
        String fileName = attachmentFile.getOriginalFilename();
        byte[] attachmentFileBytes =null;
        try {
            fileName = new String(fileName.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            log.error("Could not encode file name");
            e1.printStackTrace();
        }
        try {
            attachmentFileBytes = attachmentFile.getBytes();
        } catch (Exception e) {
            log.error("Could not load multiPart file. " + e);
        }


        //validating all params exists
        FileUpload fileUpload;
        Response response = new Response();
        String fileType = "";
        response = validateLoadFile(attachmentFile, request, fileGuid, ip , serviceName, "", userAgent);
        if (response != null){
            return response;
        }
        log.debug("Successfully validated mandatory parameters");



        //moving file to its destination
        String filePath = fileSaveingPath +fileGuid+ fileName;
        File dest = new File(filePath);
        log.debug("Moving file to destination =[" + dest + "] ");
        try {
            attachmentFile.transferTo(dest);
        }catch (Exception e){
            String error = "Could not move file";
            log.error(error + e);
            response = creatAudit("N", maxIp, serviceName, 115, 0, "", userAgent, false, fileGuid, error);
            return response;
        }

        //get content type
        fileType = attachmentFile.getContentType();
        if(fileType ==null || fileType.equals("")){
            Tika tika = new Tika();
            fileType = tika.detect(filePath);
        }

        // getting file type and deleting file
        fileType = FilenameUtils.getExtension(filePath);
        if (fileType != null) {
            log.debug("file extension is:  [" + fileType + "]");
            try {
                fileType = Files.probeContentType(dest.toPath());
                log.debug("file type is:  [" + fileType + "]");
            } catch (Exception e) {
                String error = "Could not get file contemt type";
                log.error(error);
                response = creatAudit("N", maxIp, serviceName, 115, 0, "", userAgent, false, fileGuid, error);
                return response;
            }
        } else {
            String error = "File Upload failed. Could not get file extension.";
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 115, 0, "", userAgent, false, fileGuid, error);
            return response;
        }
        //checking file name is less than 50 chars
        fileName = FilenameUtils.removeExtension(fileName);
        if(fileName.length()>50){
            String error = "File Upload failed. Original file name is longer than 50 chars [" + fileName+ "] fileName size:  [" + fileName.length()+ "] ";
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 115, 0, "", userAgent, false, fileGuid, error);
            return response;
        }



        //checking that fileGuid exists
        log.debug( "Getting number of uses from table AUDIT");
        guidCounter = fileUploadRepo.countnumGuid(fileGuid);
        log.debug("Number of uses for fileGuid=[" + fileGuid+ "] is : [" + guidCounter+"]");

        fileUpload = fileUploadRepo.findByFileGuid(fileGuid);
        if (fileUpload == null){
            String error = "Bad FileGuid. FileGuid is not in database";
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 104, 0, "", userAgent, false, fileGuid, error);
            return response;
        }
        //loading file content into fileUpload object
        try {
            log.debug("Initializing fileUpload with attached file content");
//			byte[] bufferFile = attachmentFile.getBytes();
//			byte[] bufferFile = new byte[inputstream.available()];
//			inputstream.read(bufferFile);
            fileUpload.setFileContent(attachmentFileBytes);
            log.debug("After setting fileUpload with file content");
        }
        catch (Exception e)
        {
            String error = "Could not load file content" ;
            log.error(error + e);
            response = creatAudit("N", maxIp, serviceName, 105, 0, "", userAgent, false, fileGuid, error);
            return response;
        }

        //checking that current FileGuid is not in use
        FileUpload isTwice = fileUploadRepo.CheckGuid(fileGuid);
        if (isTwice != null) {
            String error = "FileGuid already in used";
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 104, 0, "", userAgent, false, fileGuid, error);
            return response;
        }
        log.debug("Current FileGuid is not in use");

        // checking if file guid was in use in the last 24h
        log.debug("Checking that current FileGuid was not in use for the past 24 hours");
        guidCounter24h = fileUploadRepo.countByguId24hr(fileGuid);
        if (guidCounter24h > 1) {
            String error = "FileGuid was already used in the past 24 hours";
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 104, 0, "", userAgent, false, fileGuid, error);
            return response;
        }



        //check file size
        int maxSize = (Math.round(Integer.parseInt(fileSize))/1000);
        int fileSizekb = Math.round((attachmentFile.getSize())/1000);
        log.debug("FileGuid was not in use in the past 24 hours. Checking file size is less than [" + maxSize + "]kb");
        if (attachmentFile.getSize() > Integer.parseInt(fileSize)) {

            String error= "File Size Is Too Big . Source File Size:  [" + fileSizekb + "]";
            response = creatAudit("N", maxIp, serviceName, 106, 0, "", userAgent, false, fileGuid, error);
            return response;
        }
        log.debug("File size is ok:  [" + fileSizekb + "]kb. Validating file type");



        //validate file type
        response = validateFileType(fileType, maxIp, serviceName, userAgent, fileGuid);
        if (response != null){
            return response;
        }
        log.debug("Successfully validated file type. Checking for changing fileType");
        fileType = changeFileType(fileType);

        //deleting file
        log.debug("Deleteing file from destination:  ["+ dest + "]");
        dest.delete();
        log.debug("Successfully deleted file ["+ dest + "]");


        //saving file into database
        response = saveFile(fileUpload, dest, fileType, maxIp, extraInfo, serviceName, userAgent, fileGuid);

        return response;
    }

    private Response saveFile(FileUpload fileUpload, File dest, String fileType, String ip, String extraInfo, String serviceName, String userAgent, String guid) {
        Response response = new Response();

        //Status R means it is after the first upload only and was not sanitized yet


        Date date = new Date();
        String date2DB = dateFormat.format(date);
        try{
            fileUpload.setUploaded("R");
            fileUpload.setFiletype(fileType);
            fileUpload.setGetFileGuidIP(ip);
            fileUpload.setFileUploadRequestDate(date2DB);
            fileUpload.setExtraInfo(extraInfo);
            log.debug("Saving file into FileUploadTable. Load status is R.");
            fileUploadRepo.save(fileUpload);
            log.debug("Successfully saved file to database. Deleting file from destination") ;
//		log.debug("  UploadFileImp.loadFile()- was deleted " +dest.delete());
            dest.deleteOnExit();
            log.debug("Successfully deleted file ["+ dest + "]");
        }catch (Exception e){
            String error = "Failed to complete loading file to database";
            log.error(error);
            response = creatAudit("N", ip, serviceName, 101, 0, "", userAgent, false, guid, error);
        }

        response = creatAudit("N", ip, serviceName, 0, 0, "", userAgent, true, guid, "SUCCESS");
        return response;
    }

    private String changeFileType(String fileType) {
        boolean cahnged = false;
        if (fileType.subSequence(0, 5).equals(IMAGE)) {
            fileType = fileType.substring(6, fileType.length());
        }
        if (fileType.contains(PDF)) {
            fileType = "pdf";
            cahnged = true;
        }
        if (fileType.contains("msword")) {
            fileType = "doc";
            cahnged = true;
        }
        if (fileType.contains("wordprocessingml")) {
            fileType = "docx";
            cahnged = true;
        }
        if (fileType.contains("excel")) {
            fileType = "xls";
            cahnged = true;
        }
        if (cahnged){
            log.debug("Changed fileType to: ["+fileType+"]");
        }
        return fileType;
    }
    private Response validateFileType(String fileType, String maxIp, String serviceName, String userAgent, String fileGuid) {
        //Modified this method a lot from last version which made no sense...

        Response response = new Response();

        if ((!fileType.contains(IMAGE) && !fileType.contains(APPLICATION))) {
            String error = ("File Type Is Not Allowed. FileType  [" + fileType	+ "]");
            log.error(error);
            response = creatAudit("N", maxIp, serviceName, 105, 0, "", userAgent, false, fileGuid, error);
            return response;

        }
        if (fileType.subSequence(0, 5).equals(IMAGE)) {
            if (!getAllowedListFromProps(fileType)){
                String error = ("File Type Is Not Allowed. FileType  [" + fileType	+ "]");
                log.error(error);
                response = creatAudit("N", maxIp, serviceName, 105, 0, "", userAgent, false, fileGuid, error);
                return response;
            }

        }
        // fix to be on the specific index or last index
        else if (fileType.contains(APPLICATION)) {
            if (fileType.subSequence(0, 11).equals(APPLICATION)) {
                if ((!fileType.subSequence(12, 15).equals(PDF)&& !fileType.subSequence(12, fileType.length()).equals("vnd.openxmlformats-officedocument.wordprocessingml.document")	&& !fileType
                        .subSequence(12, fileType.length())
                        .equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet") && !fileType.subSequence(12, fileType.length()).equals("msword") && !fileType.contains("excel")))
                {
                    String error = ("File Type Is Not Allowed. FileType  [" + fileType	+ "]");
                    log.error(error);
                    response = creatAudit("N", maxIp, serviceName, 105, 0, "", userAgent, false, fileGuid, error);
                    return response;
                }
            }
        }

        return null;
    }
    private Response validateLoadFile(MultipartFile attachmentFile, HttpServletRequest request, String fileGuid, String ip, String serviceName, String channelRequestName, String userAgent) {
        Response response = new Response();
        log.debug("Validating loadFile properties");

        if (attachmentFile == null || attachmentFile.getSize() == 0) {
            String error = "ERROR! attachmentFile is null";
            log.error(error);
            response = creatAudit("N", ip, serviceName, 115, 0, channelRequestName, userAgent, false, fileGuid, error);
            return response;
        }
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        log.debug(("Ip from X-FORWARDED-FOR: [" + ipAddress + "]"));
        if (ipAddress == null) {
            log.debug(("Ip Address From header is null, getting ip from remote adress"));
            ipAddress = request.getRemoteAddr();
            log.debug("Ip from remote address: [" + ipAddress + "]");
        }

        if (ipAddress == null || ipAddress.equals("") ){
            String error = "Could not find Ip from remoteAdress or from X-Forwarded-for";
            log.error(error);
            response = creatAudit("N", ip, serviceName, 115, 0, channelRequestName, userAgent, false, fileGuid, error);
            return response;
        }

        if ( fileGuid == null || fileGuid.equals("")) {
            String error = "File Guid is null";
            log.error(error);
            response = creatAudit("N", ip, serviceName, 115, 0, channelRequestName, userAgent, false, fileGuid, error);
            return response;
        }

        return null;
    }

    private boolean getAllowedListFromProps(String fileType) {

        String allowed = config.getAllowedImageFormats();
        boolean ok = false;
        if (allowed != null) {
            log.debug("Cheacking if file type is allowed. fileType: [" + fileType + "] Allowed formats: [" + allowed+ "]");
            List<String> allowedList = Arrays.asList(allowed.split("\\s*, \\s*"));
            if (allowedList != null && allowedList.size() > 0) {
                for (String format : allowedList) {
                    if (fileType.contains(format.toLowerCase())) {
                        log.debug("File type found in allowedImageFormats list [" + fileType + "]");
                        ok = true;
                        break;
                    }
                }
            } else {
                log.error("allowedImagesForamts List ie empty. Add at least one allowed foramt type under k300.essentials.allowedImageFormats."
                        + " Make sure each allowed type is seperated by a comma.");
            }
        } else {
            log.error("Could not find allowedImagesForamts List under k300.essentials.allowedImageFormats in properties file. Make sure"
                    + "a valid list exists,  and that each allowed type is seperated by a comma.");
        }
        return ok;
    }


}

