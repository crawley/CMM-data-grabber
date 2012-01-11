package au.edu.uq.cmm.paul;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.GenericGenerator;

import au.edu.uq.cmm.aclslib.server.Configuration;
import au.edu.uq.cmm.aclslib.server.ConfigurationBase;
import au.edu.uq.cmm.aclslib.server.FacilityConfig;
import au.edu.uq.cmm.paul.status.Facility;

/**
 * This class represents the configuration details of a DataGrabber instance.
 * The details are persisted using Hibernate / JPA.
 * 
 * @author scrawley
 */
@Entity
@Table(name = "CONFIGURATION")
public class DynamicConfiguration extends ConfigurationBase implements Configuration {
    private static final Logger LOG = Logger.getLogger(DynamicConfiguration.class);

    private Long id;
    private Map<String, Facility> facilityMap = new HashMap<String, Facility>();
    
    
    public static DynamicConfiguration load(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.
                    createQuery("from DynamicConfiguration",
                            DynamicConfiguration.class).
                    getSingleResult();
        } finally {
            entityManager.close();
        }
    }
    
    @Transient
    public Collection<FacilityConfig> getFacilities() {
        return new ArrayList<FacilityConfig>(facilityMap.values());
    }

    public Facility lookupFacilityByAddress(InetAddress addr) {
        Facility facility = facilityMap.get(addr.getHostAddress());
        if (facility == null) {
            facility = facilityMap.get(addr.getHostName());
        }
        return facility;
    }

    public Facility lookupFacilityById(String id) {
        for (Facility f : facilityMap.values()) {
            if (id.equals(f.getFacilityId())) {
                return f;
            }
        }
        return null;
    }

    @Transient
    public boolean isEmpty() {
        return facilityMap.isEmpty();
    }

    public void merge(EntityManagerFactory entityManagerFactory,
            ConfigurationBase staticConfig) {
        LOG.info("Merging details from static Configuration");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            setProxyHost(staticConfig.getProxyHost());
            setServerHost(staticConfig.getServerHost());
            setProxyPort(staticConfig.getProxyPort());
            setServerPort(staticConfig.getServerPort());
            for (FacilityConfig facilityConfig: staticConfig.getFacilities()) {
                if (!facilityMap.containsKey(facilityConfig.getFacilityId())) {
                    Facility facility = new Facility(facilityConfig);
                    // entityManager.persist(facility);
                    facilityMap.put(facilityConfig.getFacilityId(), facility);
                }
            }
            entityManager.persist(this);
            entityManager.getTransaction().commit();
        } finally {
            entityManager.close();
        }
    }

    @OneToMany(mappedBy="configuration", cascade=CascadeType.ALL)
    @MapKey(name="facilityId")
    public Map<String, Facility> getFacilityMap() {
        return facilityMap;
    }

    public void setFacilityMap(Map<String, Facility> facilityMap) {
        this.facilityMap = facilityMap;
    }

    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
