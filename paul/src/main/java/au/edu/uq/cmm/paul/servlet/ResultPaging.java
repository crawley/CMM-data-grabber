/*
* Copyright 2013, CMM, University of Queensland.
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

/**
 * ResultPaging class holds result set paging state.  An instance is created
 * from the request parameters and delivered to the DAO / manager.  This 
 * fetches the required number of results, and updates the instance with
 * the new offset and hasMore flag.
 * 
 * @author steve
 */
public final class ResultPaging {
	private int pageSize;
	private int resultSetOffset;
	private boolean hasMore;
	
	public ResultPaging(int pageSize, int resultSetOffset) {
		super();
		this.pageSize = pageSize;
		this.resultSetOffset = resultSetOffset;
	}

	/**
	 * The number of results to deliver in a page.
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * The offset in the result set of the next result to be delivered
	 */
	public int getResultSetOffset() {
		return resultSetOffset;
	}

	/**
	 * If true, there are more results after this page.
	 */
	public boolean isHasMore() {
		return hasMore;
	}

	public void setResultSetOffset(int resultSetOffset) {
		this.resultSetOffset = resultSetOffset;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}
}
