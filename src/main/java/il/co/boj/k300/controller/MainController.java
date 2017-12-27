package il.co.boj.k300.controller;

/**
 * Created by ofer on 27/12/17.
 */
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import il.co.boj.K300.model.Response;
import il.co.boj.K300.services.UploadFile;
import lombok.extern.log4j.Log4j;

@Controller
@Log4j
@RequestMapping(value = {"/K300/FileController"})
public class MainController {

    @Autowired
    UploadFile uploadFile;
    // http://K300/FileController/test
    //---------------------------------------------------------------------------------------------------------------------------------
    @RequestMapping(value={"/test", "/monitor"}, method={RequestMethod.GET})
    @ResponseStatus(value=HttpStatus.OK)
    @ResponseBody
    public String testK300() {
        log.debug("---------------------------------------Start----test-------------------------------------------------------");
        String response= "K300 is up and running " + new Date();
        log.debug(response);
        log.debug("---------------------------------------End----test-------------------------------------------------------");
        return response;
    }
    //---------------------------------------------------------------------------------------------------------------------------------

    @RequestMapping(value={"/getFileGuid"}, method={RequestMethod.POST})
    @ResponseStatus(value=HttpStatus.OK)
    @ResponseBody
    public Response getFileGuid(String ip, String channelRequestName, HttpServletRequest request, Integer isLimited) {

        log.debug("---------------------------------------Start----getFileGuid-------------------------------------------------------");
        log.debug( "Parameters : ip = [" + ip + "]," + "channelRequestName = [" + channelRequestName + "]," + "user-agent = [" + request.getHeader("user-agent") + "], IsLimited param =[ " + isLimited + " ]");

        Response response = new Response();
        response = uploadFile.getFileGuid(channelRequestName, ip, isLimited, request);
        log.debug("---------------------------------------End----getFileGuid-------------------------------------------------------");
        return response;
    }

    //This is to test from PostMan
//	@RequestMapping(value={"/getFileGuid"}, method={RequestMethod.POST})
//	@ResponseStatus(value=HttpStatus.OK)
//	@ResponseBody
//	public Response getFileGuidPostMan(@RequestBody FileGuidRequest fileGuidRequest, HttpServletRequest request) {
//
//		log.debug("---------------------------------------Start----getFileGuid-------------------------------------------------------");
//		log.debug( "Parameters : ip = [" + fileGuidRequest.getIp() + "]," + "channelRequestName = [" + fileGuidRequest.getChannelRequestName() + "]," + "user-agent = [" + request.getHeader("user-agent") + "], IsLimited param =[ " + fileGuidRequest.getIsLimited() + " ]");
//
//		Response response = new Response();
//		response = uploadFile.getFileGuid(fileGuidRequest.getChannelRequestName(), fileGuidRequest.getIp(), fileGuidRequest.getIsLimited(), request);
//		log.debug("---------------------------------------End----getFileGuid-------------------------------------------------------");
//		return response;
//	}
    //---------------------------------------------------------------------------------------------------------------------------------
    @RequestMapping(value={"/uploadFile"}, method={RequestMethod.POST})
    @ResponseStatus(value=HttpStatus.OK)
    @ResponseBody
    public Response uploadFile(MultipartFile attachmentFile, String ip, String fileGuid, String synchronous, HttpServletRequest request,Integer isLimited, String extraInfo) {
        log.debug("---------------------------------------Start----uploadFile-------------------------------------------------------");
        log.debug("Parameters : FileName=[" + attachmentFile.getOriginalFilename() + "]," + "ip=[" + ip + "]," + "fileGuid=[" + fileGuid + "]," + "synchronous=[" + synchronous + "]," + "user-agent=[" + request.getHeader("user-agent") + "], IsLimited param =[ " + isLimited + " ] extraInfo: [" +extraInfo+"]");

        Response response = new Response();
        response = uploadFile.loadFile(attachmentFile, ip, fileGuid, synchronous, isLimited, extraInfo, request);
        log.debug("---------------------------------------End----uploadFile-------------------------------------------------------");
        return response;
        //---------------------------------------------------------------------------------------------------------------------------------
    }


}
