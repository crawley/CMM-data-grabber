/*
* Copyright 2012, CMM, University of Queensland.
*
* This file is part of Paul.
*
* Paul is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Paul is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Paul. If not, see <http://www.gnu.org/licenses/>.
*/

package au.edu.uq.cmm.paul.servlet;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.taglibs.standard.tag.common.core.CatchTag;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;
import org.joda.time.base.BaseSingleFieldPeriod;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ServletContextAware;

import au.edu.uq.cmm.aclslib.config.ConfigurationException;
import au.edu.uq.cmm.aclslib.config.FacilityConfig;
import au.edu.uq.cmm.aclslib.proxy.AclsAuthenticationException;
import au.edu.uq.cmm.aclslib.proxy.AclsInUseException;
import au.edu.uq.cmm.aclslib.service.Service;
import au.edu.uq.cmm.aclslib.service.Service.State;
import au.edu.uq.cmm.eccles.FacilitySession;
import au.edu.uq.cmm.eccles.UnknownUserException;
import au.edu.uq.cmm.eccles.UserDetails;
import au.edu.uq.cmm.eccles.UserDetailsManager;
import au.edu.uq.cmm.paul.Paul;
import au.edu.uq.cmm.paul.PaulConfiguration;
import au.edu.uq.cmm.paul.grabber.CatchupAnalyser;
import au.edu.uq.cmm.paul.grabber.DatafileMetadata;
import au.edu.uq.cmm.paul.grabber.DatasetMetadata;
import au.edu.uq.cmm.paul.queue.AtomFeed;
import au.edu.uq.cmm.paul.queue.QueueManager;
import au.edu.uq.cmm.paul.queue.QueueManager.Slice;
import au.edu.uq.cmm.paul.status.Facility;
import au.edu.uq.cmm.paul.status.FacilityStatus;
import au.edu.uq.cmm.paul.status.FacilityStatusManager;
import au.edu.uq.cmm.paul.watcher.FileWatcher;

/**
 * The MVC controller for Paul's web UI.  This supports the status and configuration
 * pages and also implements GET access to the files in the queue area.
 * 
 * @author scrawley
 */
@Controller
public class WebUIController implements ServletContextAware {
    public enum Status {
        ON, OFF, TRANSITIONAL
    }
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(WebUIController.class);

    private static DateTimeFormatter[] FORMATS = new DateTimeFormatter[] {
        ISODateTimeFormat.dateHourMinuteSecond(),
        ISODateTimeFormat.localTimeParser(),
        ISODateTimeFormat.localDateOptionalTimeParser(),
        ISODateTimeFormat.dateTimeParser()
    };
    
    @Autowired(required=true)
    Paul services;

    @Override
    public void setServletContext(ServletContext servletContext) {
        LOG.debug("Setting the timezone (" + TimeZone.getDefault().getID() + 
                ") in the servlet context");
        servletContext.setAttribute("javax.servlet.jsp.jstl.fmt.timeZone", 
                TimeZone.getDefault());
    }

    @RequestMapping(value="/control", method=RequestMethod.GET)
    public String control(Model model) {
        addStateAndStatus(model);
        return "control";
    }

    @RequestMapping(value="/control", method=RequestMethod.POST)
    public String controlAction(Model model, HttpServletRequest request) {
        processStatusChange(getFileWatcher(), request.getParameter("watcher"));
        processStatusChange(getAtomFeed(), request.getParameter("atomFeed"));
        addStateAndStatus(model);
        return "control";
    }
    
    private void processStatusChange(Service service, String param) {
        Service.State current = service.getState();
        if (param == null) {
            return;
        }
        Status target = Status.valueOf(param);
        if (target == stateToStatus(current) || 
                stateToStatus(current) == Status.TRANSITIONAL) {
            return;
        }
        if (target == Status.ON) {
            service.startStartup();
        } else {
            service.startShutdown();
        }
    }
    
    private void addStateAndStatus(Model model) {
        State ws = getFileWatcher().getState();
        model.addAttribute("watcherState", ws);
        model.addAttribute("watcherStatus", stateToStatus(ws));
        State as = getAtomFeed().getState();
        model.addAttribute("atomFeedState", as);
        model.addAttribute("atomFeedStatus", stateToStatus(as));
        model.addAttribute("resetRequired", getLatestConfig() != getConfig());
    }
    
    private Status stateToStatus(State state) {
        switch (state) {
        case STARTED:
           return Status.ON;
        case FAILED:
        case STOPPED:
        case INITIAL:
            return Status.OFF;
        default:
            return Status.TRANSITIONAL;
        }
    }
    
    @RequestMapping(value="/sessions", method=RequestMethod.GET)
    public String status(Model model) {
        model.addAttribute("sessions", getFacilityStatusManager().getLatestSessions());
        return "sessions";
    }
    
    @RequestMapping(value="/sessions", method=RequestMethod.POST,
            params={"endSession"})
    public String endSession(Model model, 
            @RequestParam String sessionUuid, 
            HttpServletResponse response, HttpServletRequest request) 
    throws IOException, AclsAuthenticationException {
        getFacilityStatusManager().logoutSession(sessionUuid);
        response.sendRedirect(response.encodeRedirectURL(
                request.getContextPath() + "/sessions"));
        return null;
    }
    
    @RequestMapping(value="/facilities", method=RequestMethod.GET)
    public String facilities(Model model) {
        Collection<FacilityConfig> facilities = getFacilities();
        for (FacilityConfig fc : facilities) {
            getFacilityStatusManager().attachStatus((Facility) fc);
        }
        model.addAttribute("facilities", facilities);
        return "facilities";
    }
    
    @RequestMapping(value="/facilities", method=RequestMethod.GET,
            params="newForm")
    public String newFacilityForm(Model model, HttpServletRequest request) {
        model.addAttribute("facility", new Facility());  // (just for the defaults ...)
        model.addAttribute("edit", true);
        model.addAttribute("create", true);
        model.addAttribute("message", 
                "Please fill in the form and click 'Save New Facility'");
        model.addAttribute("returnTo", inferReturnTo(request, "/facilities"));
        return "facility";
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}", method=RequestMethod.GET)
    public String facilityConfig(@PathVariable String facilityName, 
            Model model, HttpServletRequest request,
            @RequestParam(required=false) String edit) 
            throws ConfigurationException {
        Facility facility = lookupFacilityByName(facilityName);
        model.addAttribute("facility", facility);
        if (edit != null) {
            model.addAttribute("edit", true);
            model.addAttribute("message", 
                    "Please fill in the form and click 'Save Facility Changes'");
        }
        return "facility";
    }
    
    @RequestMapping(value="/facilities", method=RequestMethod.POST,
            params={"create"})
    public String createFacilityConfig(
            Model model, HttpServletRequest request) 
            throws ConfigurationException {
        ValidationResult<Facility> res = getConfigManager().
                createFacility(request.getParameterMap());
        model.addAttribute("returnTo", inferReturnTo(request, "/facilities"));
        if (!res.isValid()) {
            model.addAttribute("edit", true);
            model.addAttribute("create", true);
            model.addAttribute("facility", res.getTarget());
            model.addAttribute("diags", res.getDiags());
            model.addAttribute("message", "Please correct the errors and try again");
            return "facility";
        } else {
            model.addAttribute("message", "Facility configuration created");
            return "ok";
        }
    }
    
    @RequestMapping(value = "/facilities/{facilityName:.+}", method = RequestMethod.POST, 
            params = {"copy"})
    public String copyFacilityConfig(@PathVariable String facilityName,
            Model model, HttpServletRequest request) 
            throws ConfigurationException {
        Facility facility = lookupFacilityByName(facilityName);
        facility.setFacilityName(null);
        facility.setId(null);
        model.addAttribute("edit", true);
        model.addAttribute("create", true);
        model.addAttribute("facility", facility);
        model.addAttribute("diags", Collections.emptyMap());
        model.addAttribute("message", "Fill in the new facility name, "
                + "edit the other details and click 'Save New Facility'");
        model.addAttribute("returnTo", inferReturnTo(request, "/facilities"));
        return "facility";
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}", method=RequestMethod.POST,
            params={"update"})
    public String updateFacilityConfig(
            @PathVariable String facilityName, Model model,
            HttpServletRequest request) 
            throws ConfigurationException {
        ValidationResult<Facility> res = getConfigManager().
                updateFacility(facilityName, request.getParameterMap());
        model.addAttribute("returnTo", inferReturnTo(request, "/facilities"));
        if (!res.isValid()) {
            model.addAttribute("edit", true);
            model.addAttribute("facility", res.getTarget());
            model.addAttribute("diags", res.getDiags());
            model.addAttribute("message", "Please correct the errors and try again");
            return "facility";
        } else {
            model.addAttribute("message", "Facility configuration updated");
            return "ok";
        }
    }
    
    @RequestMapping(value = "/facilities/{facilityName:.+}", method = RequestMethod.POST, 
            params = {"delete"})
    public String deleteFacilityConfig(@PathVariable String facilityName,
            Model model, HttpServletRequest request,
            @RequestParam(required = false) String confirmed) {
        model.addAttribute("returnTo", inferReturnTo(request, "/facilities"));
        model.addAttribute("facilityName", facilityName);
        if (confirmed == null) {
            return "facilityDeleteConfirmation";
        } 
        Facility facility = lookupFacilityByName(facilityName);
        if (facility == null) {
            model.addAttribute("message", 
                    "Can't find facility configuration for '" + facilityName + "'");
            return "failed";
        }
        getFileWatcher().stopFileWatching(facility);
        getConfigManager().deleteFacility(facilityName);
        model.addAttribute("message", "Facility configuration deleted");
        return "ok";
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}",
            params={"sessionLog"})
    public String facilitySessions(@PathVariable String facilityName, Model model) 
            throws ConfigurationException {
        model.addAttribute("sessions", 
                getFacilityStatusManager().sessionsForFacility(facilityName));
        model.addAttribute("facilityName", facilityName);
        return "facilitySessions";
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}",
            params={"hwm"})
    public String facilityHWM(@PathVariable String facilityName, Model model,
            @RequestParam(required=false) String hwmTimestamp) 
            throws ConfigurationException {
        Facility facility = lookupFacilityByName(facilityName);
        FacilityStatus status = getFacilityStatusManager().getStatus(facility);
        Date catchupTimestamp = getQueueManager().getCatchupTimestamp(facility);
        model.addAttribute("facilityName", facilityName);
        model.addAttribute("status", status);
        Date hwm = status.getGrabberHWMTimestamp();
        if (!tidy(hwmTimestamp).isEmpty()) {
            DateTime tmp = parseTimestamp(hwmTimestamp);
            if (tmp != null) {
                hwm = tmp.toDate();
            }
        }
        model.addAttribute("hwmTimestamp", hwm);
        model.addAttribute("catchupTimestamp", catchupTimestamp);
        model.addAttribute("analysis", 
                new CatchupAnalyser(services, facility).analyse(hwm));
        return "catchupControl";
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}", params={"setHWM"})
    public String setFacilityHWM(@PathVariable String facilityName, Model model,
            @RequestParam String hwmTimestamp) 
            throws ConfigurationException {
        Facility facility = lookupFacilityByName(facilityName);
        Date oldHwm = getFacilityStatusManager().getStatus(facility).getGrabberHWMTimestamp();
        Date hwm = null;
        if (!tidy(hwmTimestamp).isEmpty()) {
            DateTime tmp = parseTimestamp(hwmTimestamp);
            if (tmp != null) {
                hwm = tmp.toDate();
            }
        }
        if (hwm != null) {
            getFacilityStatusManager().updateHWMTimestamp(facility, hwm);
            model.addAttribute("message", "Moved HWM for '" + facilityName + "' from " +
                    oldHwm + " to " + hwm);
            return "ok";
        } else {
            model.addAttribute("message", "Bad HWM value");
            return "failed";
        }
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}", method=RequestMethod.POST, 
            params={"start"})
    public String startWatcher(@PathVariable String facilityName, Model model,
            HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        Facility facility = lookupFacilityByName(facilityName);
        if (facility != null) {
            getFileWatcher().startFileWatching(facility);
        }
        String returnTo = request.getParameter("returnTo");
        if (returnTo == null || returnTo.isEmpty()) {
            returnTo = request.getContextPath() + "/facilities";
        } else if (returnTo.startsWith("/")) {
            returnTo = request.getContextPath() + returnTo;
        }
        response.sendRedirect(returnTo);
        return null;
    }
    
    @RequestMapping(value="/facilities/{facilityName:.+}", method=RequestMethod.POST, 
            params={"stop"})
    public String stopWatcher(@PathVariable String facilityName, Model model,
            HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        Facility facility = lookupFacilityByName(facilityName);
        if (facility != null) {
            getFileWatcher().stopFileWatching(facility);
        }
        String returnTo = request.getParameter("returnTo");
        if (returnTo == null || returnTo.isEmpty()) {
            returnTo = request.getContextPath() + "/facilities";
        } else if (returnTo.startsWith("/")) {
            returnTo = request.getContextPath() + returnTo;
        }
        response.sendRedirect(returnTo);
        return null;
    }
    
    @RequestMapping(value="/mirage", method=RequestMethod.GET)
    public String mirage(Model model, HttpServletResponse response) 
            throws IOException {
        response.sendRedirect(getConfig().getPrimaryRepositoryUrl());
        return null;
    }
    
    @RequestMapping(value="/acls", method=RequestMethod.GET)
    public String acls(Model model, HttpServletResponse response) 
            throws IOException {
        response.sendRedirect(getConfig().getAclsUrl());
        return null;
    }
    
    @RequestMapping(value="/facilitySelect", method=RequestMethod.GET)
    public String facilitySelector(Model model,
            HttpServletRequest request, 
            @RequestParam String next,
            @RequestParam(required=false) String slice) {
        model.addAttribute("next", next);
        model.addAttribute("slice", slice);
        model.addAttribute("returnTo", inferReturnTo(request));
        model.addAttribute("facilities", getFacilities());
        return "facilitySelect";
    }
    
    @RequestMapping(value="/facilitySelect", method=RequestMethod.POST)
    public String facilitySelect(Model model, 
            @RequestParam String next,
            @RequestParam(required=false) String slice,
            @RequestParam(required=false) String zz,
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required=false) String facilityName) 
    throws UnsupportedEncodingException, IOException {
        if (facilityName == null) {
            model.addAttribute("slice", slice);
            model.addAttribute("returnTo", inferReturnTo(request));
            model.addAttribute("facilities", getFacilities());
            model.addAttribute("message", "Select a facility from the pulldown");
            model.addAttribute("next", next);
            return "facilitySelect";
        } else {
            response.sendRedirect(request.getContextPath() + 
                    "/" + next +
                    "?facilityName=" + URLEncoder.encode(facilityName, "UTF-8") + 
                    "&slice=" + slice +
                    "&returnTo=" + inferReturnTo(request));
            return null;
        }
    }

    @RequestMapping(value="/facilityLogout")
    public String facilityLogout(Model model, HttpServletRequest request,
            @RequestParam String facilityName) {
        model.addAttribute("returnTo", inferReturnTo(request));
        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
        if (principal == null || !principal.hasRole("ROLE_ACLS_USER")) {
            model.addAttribute("message", "I don't know your ACLS userName");
            return "failed";
        }
        String userName = principal.getName();
        FacilityStatusManager fsm = getFacilityStatusManager();
        FacilitySession session = fsm.getLoginDetails(facilityName, System.currentTimeMillis());
        if (session == null || !session.getUserName().equals(userName)) {
            model.addAttribute("message", "You are not logged in on '" + facilityName + "'");
            return "failed";
        }
        try {
            fsm.logoutSession(session.getSessionUuid());
            model.addAttribute("message", "Your session has been logged out");
            return "ok";
        } catch (AclsAuthenticationException ex) {
            LOG.error("Session logout failed", ex);
            model.addAttribute("message", "Session logout failed due to an internal error");
            return "failed";
        }
    }
    
    @RequestMapping(value="/facilityLogin")
    public String facilityLogin(@RequestParam String facilityName, 
            @RequestParam(required=false) String startSession,  
            @RequestParam(required=false) String endOldSession, 
            @RequestParam(required=false) String userName, 
            @RequestParam(required=false) String account,
            Model model, HttpServletResponse response, HttpServletRequest request) 
                    throws IOException {
        FacilityStatusManager fsm = getFacilityStatusManager();
        facilityName = tidy(facilityName);
        model.addAttribute("facilityName", facilityName);
        model.addAttribute("facilities", getFacilities());
        model.addAttribute("returnTo", inferReturnTo(request));


        userName = tidy(userName);
        String password = tidy(request.getParameter("password"));
        if (startSession == null) {
            GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
            if (principal != null && principal.hasRole("ROLE_ACLS_USER") && 
                    principal.getPassword() != null &&
                    !principal.getPassword().isEmpty()) {
                userName = principal.getName();
                password = principal.getPassword();
            }
        }
        model.addAttribute("userName", userName);
        model.addAttribute("password", password);
        if (userName.isEmpty() || password.isEmpty()) {
            // Phase 1 - user must fill in user name and password
            model.addAttribute("message", "Fill in the username and password fields");
            return "facilityLogin";
        }
        try {
            if (account == null) {
                // Phase 2 - validate user credentials and get accounts list
                List<String> accounts = null;
                if (endOldSession != null) {
                    LOG.debug("Attempting old session logout");
                    fsm.logoutFacility(facilityName);
                    LOG.debug("Logout succeeded");
                }
                LOG.debug("Attempting login");
                accounts = fsm.login(facilityName, userName, password);
                LOG.debug("Login succeeded");
                // If there is only one account, select immediately.
                if (accounts != null) {
                    if (accounts.size() == 1) {
                        fsm.selectAccount(facilityName, userName, accounts.get(0));
                        LOG.debug("Account selection succeeded");;
                        return "facilityLoggedIn";
                    } else {
                        model.addAttribute("accounts", accounts);
                        model.addAttribute("message", 
                                "Select an account to complete the login");
                    }
                }
            } else {
                // Phase 3 - after user has selected an account
                fsm.selectAccount(facilityName, userName, account);
                LOG.debug("Account selection succeeded");
                return "facilityLoggedIn";
            }
        } catch (AclsAuthenticationException ex) {
            model.addAttribute("message", "Login failed: " + ex.getMessage());
        } catch (AclsInUseException ex) {
            model.addAttribute("message", 
                    "Instrument " + ex.getFacilityName() + 
                    " is currently logged in under the name of " + ex.getUserName());
            model.addAttribute("inUse", true);
        }
        return "facilityLogin";
    }
    
    @RequestMapping(value="/login", method=RequestMethod.GET)
    public String login(Model model) {
        return "login";
    }
    
    @RequestMapping(value="/loginFailed")
    public String loginFailed(Model model) {
        return "loginFailed";
    }
    
    @RequestMapping(value="/loggedIn", method=RequestMethod.GET)
    public String loggedIn(Model model) {
        return "loggedIn";
    }
    
    @RequestMapping(value="/logout", method=RequestMethod.GET)
    public String logout(Model model) {
        return "logout";
    }
    
    @RequestMapping(value="/admin", method=RequestMethod.GET)
    public String admin(Model model) {
        return "admin";
    }
    
    @RequestMapping(value="/noAccess", method=RequestMethod.GET)
    public String noAccess(Model model) {
        return "noAccess";
    }
    
    @RequestMapping(value="/unavailable", method=RequestMethod.GET)
    public String unavailable(Model model) {
        return "unavailable";
    }
    
    @RequestMapping(value="/config", method=RequestMethod.GET)
    public String config(Model model) {
        model.addAttribute("config", getLatestConfig());
        return "config";
    }
    
    @RequestMapping(value="/config", method=RequestMethod.POST, params={"reset"})
    public String configReset(Model model, HttpServletRequest request,
            @RequestParam(required=false) String confirmed) {
        model.addAttribute("returnTo", inferReturnTo(request, "config"));
        if (confirmed == null) {
            return "resetConfirmation";
        } else {
            getConfigManager().resetConfiguration();
            model.addAttribute("message", 
                    "Configuration reset succeeded.  " +
                    "Please restart the webapp to use the updated configs");
            return "ok";
        }
    }
    
    @RequestMapping(value="/queue/held", method=RequestMethod.GET)
    public String held(Model model) {
        model.addAttribute("queue", 
                getQueueManager().getSnapshot(Slice.HELD));
        return "held";
    }
    
    @RequestMapping(value="/queue/ingestible", method=RequestMethod.GET)
    public String queue(Model model) {
        model.addAttribute("queue", 
                getQueueManager().getSnapshot(Slice.INGESTIBLE));
        return "queue";
    }
    
    @RequestMapping(value="/claimDatasets", method=RequestMethod.GET)
    public String showClaimDatasets(Model model, 
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam String facilityName) 
    throws IOException {
        model.addAttribute("facilityName", facilityName);
        model.addAttribute("returnTo", inferReturnTo(request));
        model.addAttribute("datasets", 
                getQueueManager().getSnapshot(Slice.HELD, facilityName));
        return "claimDatasets";
    }
    
    @RequestMapping(value="/claimDatasets", 
            method=RequestMethod.POST, params={"claim"})
    public String claimDatasets(Model model,
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required=false) String[] ids,
            @RequestParam String facilityName) 
    throws IOException {
        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
        if (principal == null) {
            LOG.error("No principal ... can't proceed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        model.addAttribute("returnTo", inferReturnTo(request));
        if (ids == null) {
            model.addAttribute("facilityName", facilityName);
            model.addAttribute("datasets", 
                    getQueueManager().getSnapshot(Slice.HELD, facilityName));
            model.addAttribute("message", "Check the checkboxes for the " +
                    "Datasets you want to claim");
            return "claimDatasets";
        }
        if (!principal.hasRole("ROLE_ACLS_USER")) {
                model.addAttribute("message", "You must be logged in using " +
                        "ACLS credentials to claim files");
                return "failed";
            }
        String userName = principal.getName();
        try {
            int nosChanged = getQueueManager().changeUser(ids, userName, false);
            model.addAttribute("message", 
                    verbiage(nosChanged, "dataset", "datasets", "claimed"));
            return "ok";
        } catch (NumberFormatException ex) {
            LOG.debug("Rejected request with bad entry id(s)");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }
    
    @RequestMapping(value="/manageDatasets", method=RequestMethod.GET)
    public String showManageDatasets(Model model, 
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required=false) String slice,
            @RequestParam String facilityName) 
    throws IOException {
        model.addAttribute("facilityName", facilityName);
        model.addAttribute("returnTo", inferReturnTo(request));
        Slice s = inferSlice(slice, Slice.ALL);
        model.addAttribute("slice", s);
        model.addAttribute("datasets", 
                getQueueManager().getSnapshot(s, facilityName));
        model.addAttribute("userNames", getUserDetailsManager().getUserNames());
        return "manageDatasets";
    }
    
    private Slice inferSlice(String sliceName, Slice dflt) {
        if (sliceName == null) {
            return dflt;
        } else {
            try {
                return Slice.valueOf(sliceName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                LOG.debug("unrecognized slice - ignoring");
                return dflt;
            }
        }
    }
    
    @RequestMapping(value="/manageDatasets", method=RequestMethod.POST)
    public String manageDatasets(Model model,
            HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required=false) String[] ids,
            @RequestParam(required=false) String userName,
            @RequestParam(required=false) String slice,
            @RequestParam(required=false) String confirmed,
            @RequestParam String action,
            @RequestParam(required=false) String facilityName) 
    throws IOException {
        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();
        if (principal == null) {
            LOG.error("No principal ... can't proceed");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        if (!principal.hasRole("ROLE_ADMIN")) {
            model.addAttribute("message", "Only an administrator can manage datasets");
            return "failed";
        }
        model.addAttribute("facilityName", facilityName);
        model.addAttribute("slice", slice);
        model.addAttribute("returnTo", inferReturnTo(request));
        if (action.equals("deleteAll")) {
            return deleteAll(model, request, slice, facilityName, true, confirmed);
        } else if (action.equals("archiveAll")) {
            return deleteAll(model, request, slice, facilityName, false, confirmed);
        } else if (action.equals("expire")) {
            return expire(model, request, slice, facilityName, confirmed);
        }
        QueueManager qm = getQueueManager();
        if (ids == null) {
            model.addAttribute("datasets", 
                    qm.getSnapshot(inferSlice(slice, Slice.ALL), facilityName));
            model.addAttribute("userNames", getUserDetailsManager().getUserNames());
            model.addAttribute("message", 
                    "Check the checkboxes for the Datasets you want to manage");
            return "manageDatasets";
        } 
        try {
            int nosChanged;
            switch (action) {
            case "archive":
                nosChanged = qm.delete(ids, false);
                model.addAttribute("message", 
                        verbiage(nosChanged, "dataset", "datasets", "archived"));
                return "ok";
            case "delete":
                nosChanged = qm.delete(ids, true);
                model.addAttribute("message",
                        verbiage(nosChanged, "dataset", "datasets", "deleted"));
                return "ok";
            case "assign":
                nosChanged = qm.changeUser(ids, userName, true);
                model.addAttribute("message", 
                        verbiage(nosChanged, "dataset", "datasets", "assigned"));
                return "ok";
            default:
                LOG.debug("Rejected request with unrecognized action");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        } catch (NumberFormatException ex) {
            LOG.debug("Rejected request with bad entry id(s)");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }
    
    private String verbiage(int count, String singular, String plural, String verbed) {
        if (count == 0) {
            return "No " + plural + " " + verbed;
        } else if (count == 1) {
            return "1 " + singular + " " + verbed;
        } else {
            return count + " " + plural + " " + verbed;
        }
    }

    private String deleteAll(Model model, HttpServletRequest request, 
            String sliceName, String facilityName, boolean discard, String confirmed) {
        if (confirmed == null) {
            model.addAttribute("discard", discard);
            return "queueDeleteConfirmation";
        }
        Slice slice = inferSlice(sliceName, null);
        int count = getQueueManager().deleteAll(discard, facilityName, slice);
        model.addAttribute("message", 
                verbiage(count, "queue entry", "queue entries", 
                        discard ? "deleted" : "archived"));
        return "ok";
    }
    
    private String expire(Model model, HttpServletRequest request, 
            String sliceName, String facilityName, String confirmed) {
        String mode = request.getParameter("mode");
        String olderThan = request.getParameter("olderThan");
        String age = request.getParameter("age");
        Date cutoff = determineCutoff(model, tidy(olderThan), tidy(age));
        if (cutoff == null || confirmed == null) {
            return "queueExpiryForm";
        }
        QueueManager.Slice slice = inferSlice(sliceName, null);
        int count = getQueueManager().expireAll(
                mode.equals("discard"), facilityName, slice, cutoff);

        model.addAttribute("message", 
                verbiage(count, "queue entry", "queue entries", "expired"));
        return "ok";
    }

    @RequestMapping(value="/datasets/{entry:.+}", 
            method=RequestMethod.GET)
    public String queueEntry(@PathVariable String entry, Model model, 
            HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        long id;
        try {
            id = Long.parseLong(entry);
        } catch (NumberFormatException ex) {
            LOG.debug("Rejected request with bad entry id");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        DatasetMetadata metadata = fetchMetadata(id);
        if (metadata == null) {
            LOG.debug("Rejected request for unknown entry");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        model.addAttribute("entry", metadata);
        model.addAttribute("returnTo", inferReturnTo(request));
        return "dataset";
    }
    
    @RequestMapping(value="/files/{fileName:.+}", method=RequestMethod.GET)
    public String file(@PathVariable String fileName, Model model, 
            HttpServletResponse response) 
            throws IOException {
        LOG.debug("Request to fetch file " + fileName);
        // This aims to prevent requests from reading files outside of the queue directory.
        // FIXME - this assumes that the directory for the queue is flat ...
        if (fileName.contains("/") || fileName.equals("..")) {
            LOG.debug("Rejected request for security reasons");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        File file = new File(getConfig().getCaptureDirectory(), fileName);
        DatafileMetadata metadata = fetchMetadata(file);
        if (metadata == null) {
            LOG.debug("No metadata for file " + fileName);
        } else {
            LOG.debug("Found metadata for file " + fileName);
        }
        model.addAttribute("file", file);
        model.addAttribute("contentType", 
                metadata == null ? "application/octet-stream" : metadata.getMimeType());
        return "fileView";
    }
    
    @RequestMapping(value="/users", method=RequestMethod.GET)
    public String users(Model model) {
        model.addAttribute("userNames", getUserDetailsManager().getUserNames());
        return "users";
    }

    @RequestMapping(value="/users/{userName:.+}", method=RequestMethod.GET)
    public String user(@PathVariable String userName, Model model,
            HttpServletResponse response) 
            throws IOException {
        try {
            UserDetails userDetails = getUserDetailsManager().lookupUser(userName, true);
            model.addAttribute("user", userDetails);
        } catch (UnknownUserException e) {
            LOG.debug("Rejected request for security reasons");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        return "user";
    }
    
    private String tidy(String str) {
        return str == null ? "" : str.trim();
    }
    
    private String inferReturnTo(HttpServletRequest request) {
        return inferReturnTo(request, "");
    }
    
    private String inferReturnTo(HttpServletRequest request, String dflt) {
        String param = request.getParameter("returnTo");
        if (param == null) {
            param = dflt;
        } else {
            param = param.trim();
        }
        if (param.startsWith(request.getContextPath())) {
            return param;
        } else {
            return request.getContextPath() + "/" + param;
        }
    }
    
    private DatafileMetadata fetchMetadata(File file) {
        EntityManager entityManager = createEntityManager();
        try {
            TypedQuery<DatafileMetadata> query = entityManager.createQuery(
                    "from DatafileMetadata d where d.capturedFilePathname = :pathName", 
                    DatafileMetadata.class);
            query.setParameter("pathName", file.getAbsolutePath());
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } finally {
            entityManager.close();
        }
    }

    private DatasetMetadata fetchMetadata(long id) {
        EntityManager entityManager = createEntityManager();
        try {
            TypedQuery<DatasetMetadata> query = entityManager.createQuery(
                    "from DatasetMetadata d where d.id = :id", 
                    DatasetMetadata.class);
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        } finally {
            entityManager.close();
        }
    }
    
    private FileWatcher getFileWatcher() {
        return services.getFileWatcher();
    }
    
    private FacilityStatusManager getFacilityStatusManager() {
        return services.getFacilityStatusManager();
    }
    
    private QueueManager getQueueManager() {
        return services.getQueueManager();
    }
    
    private UserDetailsManager getUserDetailsManager() {
        return services.getUserDetailsManager();
    }
    
    private AtomFeed getAtomFeed() {
        return services.getAtomFeed();
    }
    
    private ConfigurationManager getConfigManager() {
        return services.getConfigManager();
    }
    
    private PaulConfiguration getLatestConfig() {
        return getConfigManager().getLatestConfig();
    }
    
    private PaulConfiguration getConfig() {
        return getConfigManager().getActiveConfig();
    }
    
    private Facility lookupFacilityByName(String facilityName) {
        return (Facility) services.getFacilityMapper().lookup(null, facilityName, null);
    }
    
    private Collection<FacilityConfig> getFacilities() {
        return services.getFacilityMapper().allFacilities();
    }
    
    private EntityManager createEntityManager() {
        return services.getEntityManagerFactory().createEntityManager();
    }
    
    private Date determineCutoff(Model model, String olderThan, String age) {
        
        if (olderThan.isEmpty() && age.isEmpty()) {
            model.addAttribute("message", 
                    "Either an expiry date or an age must be supplied");
            return null;
        }
        String[] parts = age.split("\\s", 2);
        DateTime cutoff;
        if (olderThan.isEmpty()) {
            int value;
            try {
                value = Integer.parseInt(parts[0]);
            } catch (NumberFormatException ex) {
                model.addAttribute("message", "Age quantity is not an integer");
                return null;
            }
            BaseSingleFieldPeriod p;
            switch (parts.length == 1 ? "" : parts[1]) {
            case "minute" : case "minutes" :
                p = Minutes.minutes(value);
                break;
            case "hour" : case "hours" :
                p = Hours.hours(value);
                break;
            case "day" : case "days" :
                p = Days.days(value);
                break;
            case "week" : case "weeks" :
                p = Weeks.weeks(value);
                break;
            case "month" : case "months" :
                p = Months.months(value);
                break;
            case "year" : case "years" :
                p = Years.years(value);
                break;
            default :
                model.addAttribute("message", "Unrecognized age time-unit");
                return null;
            }
            cutoff = DateTime.now().minus(p);
        } else {
            cutoff = parseTimestamp(olderThan);
            if (cutoff == null) {
                model.addAttribute("message", "Unrecognizable expiry date");
                return null;
            }
        }
        if (cutoff.isAfter(new DateTime())) {
            model.addAttribute("message", "Supplied or computed expiry date is in the future!");
            return null;
        }
        model.addAttribute("computedDate", FORMATS[0].print(cutoff));
        return cutoff.toDate();
    }

    private DateTime parseTimestamp(String stamp) {
        for (DateTimeFormatter format : FORMATS) {
            try {
                return format.parseDateTime(stamp);
            } catch (IllegalArgumentException ex) {
                continue;
            }
        }
        return null;
    }
}
