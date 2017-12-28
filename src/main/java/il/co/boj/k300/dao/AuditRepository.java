package il.co.boj.k300.dao;

import il.co.boj.k300.model.entities.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface AuditRepository extends JpaRepository<Audit, Long>
{

    @Query(value = "select COUNT(*)  from audit where ip= :ip AND  SERVICE_NAME = :serviceName  AND REQUEST_DATE >= DATE_SUB(NOW(),INTERVAL 24 HOUR) ", nativeQuery = true)
    public int  countBlockedIp(@Param("ip") String ip, @Param("serviceName") String serviceName);

    public Audit findByFileGuid(String guid);


}