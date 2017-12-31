package il.co.boj.k300.schedular;

/**
 * Created by ofer on 31/12/17.
 */
import il.co.boj.k300.dao.FileUploadRepository;
import il.co.boj.k300.services.GetLoadedFiles;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@EnableScheduling
@Log4j
@Component
public class SchedularDB {

    @Autowired
    GetLoadedFiles getLoadedFiles;

    @Autowired
    FileUploadRepository fileUploadRep;

    //---------------------------------------------------------------------------------------------------------------
    @Scheduled(cron="*45 * * * * ?")
    public void checkLoadFile()	{
        log.trace("Check for uploadedFile. Time: " + new Date());

        getLoadedFiles.getUploadedFiles();
    }

    //------------------------

    @Scheduled (cron = " 0 1 1 * * ?")
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public void deleteUploadFileTable(){
        log.debug("SchedulerToDB.deleteFileUpload() - *****B4 the daily FileUploadTable cleaning Scheduler *****");
        fileUploadRep.deleteAll();
        log.debug("SchedulerToDB.deleteFileUpload() - *****after the daily FileUploadTable cleaning Scheduler *****");


    }


}
