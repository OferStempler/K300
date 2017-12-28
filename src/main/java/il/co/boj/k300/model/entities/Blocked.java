package il.co.boj.k300.model.entities;

/**
 * Created by ofer on 28/12/17.
 */

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "BLOCKED")
@Data
public class Blocked   {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "ID")
    private String id;

    @Column(name = "IP")
    private String ip;

    @Column(name = "BLOCKED")
    private String blocked;

    @Column(name = "UPDATED_DATE")
    private String updatedate;

    @Column(name = "CHANNEL_REQUEST_NAME")
    private String channelRequestName;

    @Column(name = "SERVICE_NAME")
    private String serviceName;


}