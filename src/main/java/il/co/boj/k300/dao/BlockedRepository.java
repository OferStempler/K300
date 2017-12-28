package il.co.boj.k300.dao;

/**
 * Created by ofer on 28/12/17.
 */
import il.co.boj.k300.model.entities.Blocked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedRepository  extends JpaRepository<Blocked, Long>
{
    //select * from BLOCKED where ip = ?1 AND  serviceName = ?2 AND datetime(updatedate) >= datetime('now','-1 day','+3 hours') AND datetime(updatedate) <= datetime('now','+3 hours')",nativeQuery=true)
    @Query(value = "select * from BLOCKED where IP= :ip AND  SERVICE_NAME = :serviceName  AND UPDATED_DATE >= DATE_SUB(NOW(),INTERVAL 24 HOUR) ", nativeQuery = true)
    public Blocked  checkBlockedIp(@Param("ip") String ip, @Param("serviceName") String serviceName);
    //select * from   blocked  where RequestDate >= DATE_SUB(NOW(),INTERVAL 24 HOUR);
    @Query(value= "select * from BLOCKED where IP = ip AND where FILE_GUID = :fileGuid AND  UPDATED_DATE >= DATE_SUB(NOW(),INTERVAL 24 HOUR) AND updatedate <=DATE_SUB(NOW()) ",nativeQuery=true)
    //	  @Query(value= "select * from BLOCKED where ip = ?1 AND where fileGuid = ?2 AND  datetime(updatedate) >= datetime('now','-1 day','+3 hours') AND datetime(updatedate) <= datetime('now','+26 hours')",nativeQuery=true)
    public Blocked  checkguidBlocked(String ip,String fileGuid);
    //public   Blocked save(Blocked file);
    public    Blocked  findByIp(String ip);

}
