package au.edu.uq.cmm.paul.status;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;


@Entity
@Table(name = "SESSIONS")
public class FacilitySession {
    private String userName;
    private String account;
    private Facility facility;
    private Date loginTime;
    private Date logoutTime;
    private Long sessionId;
    
    public FacilitySession() {
        super();
    }
    
    public FacilitySession(String userName, String account, Facility facility,
            Date loginTime) {
        super();
        this.userName = userName;
        this.account = account;
        this.facility = facility;
        this.loginTime = loginTime;
    }

    public Date getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(Date logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getUserName() {
        return userName;
    }

    public String getAccount() {
        return account;
    }

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name="facility_id")
    public Facility getFacility() {
        return facility;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }
}
