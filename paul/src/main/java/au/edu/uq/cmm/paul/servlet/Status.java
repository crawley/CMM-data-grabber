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

import au.edu.uq.cmm.aclslib.service.Service.State;

public enum Status {
    ON, OFF, TRANSITIONAL;

    public static Status forState(State state) {
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
}