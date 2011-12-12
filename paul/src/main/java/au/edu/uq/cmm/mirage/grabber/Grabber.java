package au.edu.uq.cmm.mirage.grabber;

import java.io.IOException;

import org.apache.log4j.Logger;

import au.edu.uq.cmm.aclslib.proxy.AclsProxy;
import au.edu.uq.cmm.aclslib.server.Configuration;
import au.edu.uq.cmm.aclslib.service.CompositeServiceBase;
import au.edu.uq.cmm.aclslib.service.ServiceException;
import au.edu.uq.cmm.mirage.status.FacilityStatusManager;

public class Grabber extends CompositeServiceBase {
    private static final String SMB_CONF_PATHNAME = "/etc/samba/smb.conf";
    private static final Logger LOG = Logger.getLogger(Grabber.class);
    private FileWatcher fileWatcher;
    private FacilityStatusManager statusManager;
    private AclsProxy proxy;
    private UncPathnameMapper uncNameMapper;
    
    public Grabber(Configuration config) throws IOException {
        this.proxy = new AclsProxy(config);
        this.statusManager = new FacilityStatusManager(proxy);
        // FIXME ... this should be pluggable.
        uncNameMapper = new SambaUncPathameMapper(SMB_CONF_PATHNAME);
        this.fileWatcher = new FileWatcher(config, uncNameMapper);
    }

    public static void main(String[] args) {
        String configFile = null;
        if (args.length > 0) {
            configFile = args[0];
        }
        try {
            Configuration config = Configuration.loadConfiguration(configFile);
            if (config == null) {
                LOG.info("Can't read/load configuration file");
                System.exit(2);
            }
            Grabber grabber = new Grabber(config);
            grabber.startup();
            grabber.awaitShutdown();
            LOG.info("Exitting normally");
            System.exit(0);
        } catch (Throwable ex) {
            LOG.error("Unhandled exception", ex);
            System.exit(1);
        }
    }
    
    protected void doShutdown() {
        fileWatcher.shutdown();
        proxy.shutdown();
    }

    protected void doStartup() throws ServiceException {
        proxy.startup();
        fileWatcher.startup();
    }
}
