package il.co.boj.k300.dao;

/**
 * Created by ofer on 28/12/17.
 */
import java.util.ArrayList;

import il.co.boj.k300.model.entities.Audit;
import il.co.boj.k300.model.entities.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface FileUploadRepository  extends JpaRepository<FileUpload, Long>
{
    @Query(value = "select  * from fileuploadtable where FILE_GUID= :fileGuid AND SANITIZE_STATUS  is not null ", nativeQuery = true)
    public FileUpload  CheckGuid(@Param("fileGuid") String fileGuid);

    @Query(value = "select COUNT(*) from audit where FILE_GUID= :fileGuid ", nativeQuery = true)
    public int  countnumGuid(@Param("fileGuid") String fileGuid);

    @Query(value = "select * from audit where FILE_GUID= :fileGuid AND  REQUEST_DATE >= DATE_SUB(NOW(),INTERVAL 1 HOUR) ", nativeQuery = true)
    public int  countByguId(@Param("fileGuid") String fileGuid);

    @Query(value = "select COUNT(*) from audit where FILE_GUID= :fileGuid AND REQUEST_DATE >= DATE_SUB(NOW(),INTERVAL 1 HOUR) ", nativeQuery = true)
    public int  countByguId24hr(@Param("fileGuid") String fileGuid);

    @Query(value = " select   *    from audit  where IP = ?1 AND FILE_GUID = ?2 ", nativeQuery = true)
    public ArrayList<Audit> ipNotSame(String ip, String fileGuid);


    public FileUpload findByFileGuid(String guid);

    //This one should be indexed
    public     ArrayList<FileUpload> findByUploaded(String loaded);

    //@Query(value = " SELECT  * FROM k300.fileuploadtable where uploaded ='D'   limit 10  ", nativeQuery = true)
    @Query(value = " select * from fileuploadtable where  FileUploadSavingDate <DATE_SUB(NOW(),INTERVAL 1 WEEK)  limit 10  ", nativeQuery = true)
    public ArrayList<  FileUpload> findByUploadedDel();

    //clean FileUploadTable
    @Modifying
    @Query(value = "delete from FileUpload")
    public void deleteAll();

}