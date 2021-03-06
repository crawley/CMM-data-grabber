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

package au.edu.uq.cmm.paul.watcher;

import java.io.File;
import java.util.EventObject;

import au.edu.uq.cmm.aclslib.config.FacilityConfig;

@SuppressWarnings("serial")
public class FileWatcherEvent extends EventObject {
    private final File file;
    private final boolean create;
    private final long timestamp;
    private final boolean catchup;
    
    public FileWatcherEvent(FacilityConfig facility, File file, 
            boolean create, long timestamp, boolean catchup) {
        super(facility);
        this.file = file;
        this.create = create;
        this.timestamp = timestamp;
        this.catchup = catchup;
    }

    public FacilityConfig getFacility() {
        return (FacilityConfig) getSource();
    }

    public File getFile() {
        return file;
    }

    public boolean isCreate() {
        return create;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCatchup() {
        return catchup;
    }
}
