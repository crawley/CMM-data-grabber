<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page session="false"%>
<html lang="en">
<head>
<%@ include file="/WEB-INF/jsp/commonHead.jspFrag"%>
<title>Data Grabber Queue Diagnostics</title>
</head>
<body>
	<%@ include file="/WEB-INF/jsp/commonHeader.jspFrag"%>
	<div class="container-fluid">
		<div class="row-fluid"><h1>Queue Diagnostics for ${facilityName}</h1></div>
		<div class="row-fluid">
			<table class="table table-bordered">
				<tr><td class="span3">Grabber status : </td><td class="span7">${status.status}</td></tr> 
			<tr><td class="span3">Grabber message : </td><td class="span7">${status.message}</td></tr> 
			<tr><td class="span3">Grabber LWM timestamp :</td><td class="span7"> 
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${status.grabberLWMTimestamp}"/></td></tr> 
			<tr><td class="span3">Grabber HWM timestamp : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${status.grabberHWMTimestamp}"/></td></tr> 
			<tr><td class="span3">Timestamp of first queued Dataset : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${analysis.qStart}"/></td></tr> 
			<tr><td class="span3">Timestamp of last queued Dataset : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${analysis.qEnd}"/></td></tr> 
			<tr><td class="span3">Timestamp of first folder Dataset : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${analysis.fStart}"/></td></tr> 
			<tr><td class="span3">Timestamp of last folder Dataset : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${analysis.fEnd}"/></td></tr> 
			<tr><td class="span3">LWM / HWM used in analysis : </td><td class="span7">
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${lwmTimestamp}"/> / 
				<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${hwmTimestamp}"/></td></tr> 
			</table>
		</div>
		<div class="row-fluid"><h2>Statistics</h2></div>
		<div class="row-fluid">
		<table class="table table-striped table-condensed">
			<thead>
				<tr><th class="span2">Timespan</th>
					<th class="span2">All Datasets in folder</th>
					<th class="span2">Unmatched Datasets in folder</th>
					<th class="span2">All Datasets in queues</th>
					<th class="span2">Dataset groups with multiples in queues</th>
					<th class="span2">Unmatched Dataset groups in queues</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>all</td>
					<td>${analysis.all.datasetsInFolder}</td>
					<td>${analysis.all.datasetsUnmatchedInFolder}</td>
					<td>${analysis.all.datasetsInDatabase}</td>
					<td>${analysis.all.groupsWithDuplicatesInDatabase}</td>
					<td>${analysis.all.groupsUnmatchedInDatabase}</td>
				</tr>
				<c:if test="${!empty analysis.beforeLWM}">
					<tr>
						<td>&lt; LWM</td>
						<td>${analysis.beforeLWM.datasetsInFolder}</td>
						<td>${analysis.beforeLWM.datasetsUnmatchedInFolder}</td>
						<td>${analysis.beforeLWM.datasetsInDatabase}</td>
						<td>${analysis.beforeLWM.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.beforeLWM.groupsUnmatchedInDatabase}</td>
					</tr>
					<tr>
						<td>between LWM &amp; HWM</td>
						<td>${analysis.intertidal.datasetsInFolder}</td>
						<td>${analysis.intertidal.datasetsUnmatchedInFolder}</td>
						<td>${analysis.intertidal.datasetsInDatabase}</td>
						<td>${analysis.intertidal.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.intertidal.groupsUnmatchedInDatabase}</td>
					</tr>
					<tr>
						<td>&gt; HWM</td>
						<td>${analysis.afterHWM.datasetsInFolder}</td>
						<td>${analysis.afterHWM.datasetsUnmatchedInFolder}</td>
						<td>${analysis.afterHWM.datasetsInDatabase}</td>
						<td>${analysis.afterHWM.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.afterHWM.groupsUnmatchedInDatabase}</td>
					</tr>
				</c:if>
				<c:if test="${!empty analysis.beforeQStart}">
					<tr>
						<td>&lt; queue start</td>
						<td>${analysis.beforeQStart.datasetsInFolder}</td>
						<td>${analysis.beforeQStart.datasetsUnmatchedInFolder}</td>
						<td>${analysis.beforeQStart.datasetsInDatabase}</td>
						<td>${analysis.beforeQStart.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.beforeQStart.groupsUnmatchedInDatabase}</td>
					</tr>
					<tr>
						<td>in queue time-range</td>
						<td>${analysis.inQueue.datasetsInFolder}</td>
						<td>${analysis.inQueue.datasetsUnmatchedInFolder}</td>
						<td>${analysis.inQueue.datasetsInDatabase}</td>
						<td>${analysis.inQueue.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.inQueue.groupsUnmatchedInDatabase}</td>
					</tr>
					<tr>
						<td>&gt; queue end</td>
						<td>${analysis.afterQEnd.datasetsInFolder}</td>
						<td>${analysis.afterQEnd.datasetsUnmatchedInFolder}</td>
						<td>${analysis.afterQEnd.datasetsInDatabase}</td>
						<td>${analysis.afterQEnd.groupsWithDuplicatesInDatabase}</td>
						<td>${analysis.afterQEnd.groupsUnmatchedInDatabase}</td>
					</tr>
				</c:if>
			</tbody>
		</table>
		</div>
		<c:if test="${analysis.problems.nosProblems > 0}">
				<div class="row-fluid"><h2>Problems</h2></div>
				<div class="row-fluid">
					<table class="table table-striped table-condensed">
						<thead>
							<tr>
								<th class="span4">Problem type</th>
								<th class="span2">Count</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>Metadata file missing</td>
								<td>${analysis.problems.metadataMissing}
							</tr>
							<tr>
								<td>Metadata file size wrong</td>
								<td>${analysis.problems.metadataSize}
							</tr>
							<tr>
								<td>Grabbed Datafile missing</td>
								<td>${analysis.problems.fileMissing}
							</tr>
							<tr>
								<td>Grabbed Datafile size doesn't match recorded size</td>
								<td>${analysis.problems.fileSize}
							</tr>
							<tr>
								<td>Grabbed Datafile size doesn't match current file size
									in S:</td>
								<td>${analysis.problems.fileSize2}
							</tr>
							<tr>
								<td>Grabbed Datafile hash doesn't match recorded hash</td>
								<td>${analysis.problems.fileHash}
							</tr>
							<tr>
								<td>Grabbed Datafile hash doesn't match current file hash
									in S:</td>
								<td>${analysis.problems.fileHash2}
							</tr>
							<tr>
								<td>Unexpected IO Errors</td>
								<td>${analysis.problems.ioError}
							</tr>
						</tbody>
					</table>
				</div>
				<div class="row-fluid">
					<table class="table table-striped table-condensed">
						<thead>
							<tr>
								<th class="span1">Dataset Id</th>
								<th class="span1">Problem Type</th>
								<th class="span5">Details</th>
								<th class="span2">&nbsp;</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach items="${analysis.problems.problems}" var="problem">
								<tr >
									<c:choose>
									    <c:when test="${empty prevId || prevId != problem.dataset.id}">
									    	<td><a href="/paul/datasets/${problem.dataset.id}">
									    		${problem.dataset.id}</a></td>
										</c:when>
										<c:otherwise><td>&nbsp;</td></c:otherwise>
									</c:choose>
									<td>${problem.type}</td>
									<td>${problem.details}</td>
									<c:choose>
									    <c:when test="${empty prevId || prevId != problem.dataset.id}">
									    	<td>
												<form class="well,form-horizontal"
													  style="margin: 0px 0px 0px" method="post"
												      action="/paul/datasets/${problem.dataset.id}">
													<input type="hidden" name="returnTo" 
												   		   value="/paul/queueDiagnostics/${facilityName}">
													<button class="btn" type="submit" name="regrab">
														Regrab Dataset
													</button>
													<button class="btn" type="submit" name="delete">Delete</button>
													<button class="btn" type="submit" name="archive">Archive</button>
												</form>
											</td>
										</c:when>
										<c:otherwise><td>&nbsp;</td></c:otherwise>
									</c:choose>
								</tr>
								<c:set var="prevId" value="${problem.dataset.id}"/>
							</c:forEach>
						</tbody>
					</table>
				</div>
		</c:if>
		<div class="row-fluid">
			<h2>Missing or duplicated Datasets (between LWM and HWM)</h2>
		</div>
		<div class="row-fluid">
			<div class="span4">
				<table class="table table-striped table-condensed">
					<thead>
						<tr>
							<th colspan="2">Datasets missing from queues</th>
						</tr>
						<tr>
							<th class="span4">Dataset Details</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach var="group" items="${analysis.grouped}">
							<c:if test="${empty group.allInDatabase}">
								<tr>
									<td>${group.inFolder.facilityFilePathnameBase} | 
										<fmt:formatDate
											pattern="yyyy-MM-dd'T'HH:mm:ss"
											value="${group.inFolder.firstFileTimestamp}" />
										<fmt:formatDate
											pattern="yyyy-MM-dd'T'HH:mm:ss"
											value="${group.inFolder.lastFileTimestamp}" />
										<fmt:formatDate
											pattern="yyyy-MM-dd'T'HH:mm:ss"
											value="${group.inFolder.captureTimestamp}" />
										<fmt:formatDate
											pattern="yyyy-MM-dd'T'HH:mm:ss"
											value="${group.inFolder.updateTimestamp}" />
									</td>
									<td>
										<form class="well,form-horizontal" style="margin: 0px 0px 0px"
											action="/paul/datasets/" method=post>
											<input type="hidden" name="pathnameBase"
												value="${group.inFolder.sourceFilePathnameBase}"> <input
												type="hidden" name="facilityName" value="${facilityName}">
											<input type="hidden" name="returnTo"
												value="/paul/queueDiagnostics/${facilityName}">
											<button class="btn" type="submit" name="grab">Grab
												Dataset</button>
										</form>
									</td>
								</tr>
							</c:if>
						</c:forEach>
					</tbody>
				</table>
			</div>
			<div class="span8">
				<table class="table table-striped table-condensed">
					<thead>
						<tr>
							<th colspan="2">Datasets in queues</th>
						</tr>
						<tr>
							<th class="span5">Dataset Details</th>
							<th class="span3">&nbsp;</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach var="group" items="${analysis.grouped}">
							<c:if test="${group.unmatchedInDatabase || group.duplicatesInDatabase}">
							    <c:set var="seenMatched" value=""/>
								<c:forEach var="dataset" items="${group.allDecorated}">
									<tr>
										<c:choose>
											<c:when test="${dataset.inFolder}">
												<td>In-folder version -
													${dataset.facilityFilePathnameBase} <br> 
													FF: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.firstFileTimestamp}" />,
													LF: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.lastFileTimestamp}" />,
													C: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.captureTimestamp}" />,
													U: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.updateTimestamp}" />
												</td>
											</c:when>
											<c:otherwise>
												<td>
													<c:choose>
														<c:when test="${dataset.unmatched}">Unmatched - </c:when>
														<c:when test="${empty seenMatched}">Matched - </c:when>
														<c:otherwise>Duplicate - </c:otherwise>
													</c:choose>
													<a href="/paul/datasets/${dataset.id}">Dataset
														#${dataset.id}</a> - ${dataset.facilityFilePathnameBase} <br>
													FF: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.firstFileTimestamp}" />,
													LF: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.lastFileTimestamp}" />,
													C: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.captureTimestamp}" />,
													U: <fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
														value="${dataset.updateTimestamp}" />
												</td>
											</c:otherwise>
										</c:choose>
										<c:choose>
											<c:when test="${dataset.inFolder}">
												<td>&nbsp;</td>
											</c:when>
											<c:when test="${dataset.unmatched}">
												<td>
													<form class="well,form-horizontal"
														style="margin: 0px 0px 0px"
														action="/paul/datasets/${dataset.id}" method=post>
														<input type="hidden" name="returnTo"
															value="/paul/queueDiagnostics/${facilityName}">
														<button class="btn" type="button"
														    ${!dataset.inFolder ? 'disabled' : ''}
															onClick="document.location = '/paul/datasets/${dataset.id}' + 
														             '?regrab&returnTo=/paul/queueDiagnostics/${facilityName}'">
															Regrab Dataset</button>
														<button class="btn" type="submit" name="delete">Delete</button>
														<button class="btn" type="submit" name="archive">Archive</button>
													</form>
												</td>
											</c:when>
											<c:when test="${group.duplicatesInDatabase}">
												<td><form class="well,form-horizontal"
														style="margin: 0px 0px 0px"
														action="/paul/datasets/${dataset.id}" method=post>
														<input type="hidden" name="returnTo"
															value="/paul/queueDiagnostics/${facilityName}">
														<button class="btn" type="submit" name="delete">Delete</button>
													 </form>
											    </td>
											    <c:set var="seenMatched" value="yes"/>
											</c:when>
											<c:otherwise>
												<td></td>
											</c:otherwise>
										</c:choose>
									</tr>
								</c:forEach>
							</c:if>
						</c:forEach>
					</tbody>
				</table>
			</div>
		</div>
		<div class="row-fluid"><h2>Actions</h2></div>
		<form action="/paul/facilities/${facilityName}" method="post">
			<input type="hidden" name="returnTo" value="/paul/queueDiagnostics/${facilityName}">
		    <div class="row-fluid">
				Check datafile hashes (slow!): 
				<input type="checkbox" name="checkHashes" value="true" ${checkHashes ? 'checked' : ''}>
			</div>
			<div class="row-fluid">
				<c:set var="isolwm">
					<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${lwmTimestamp}" />
				</c:set>
				New / proposed LWM: <input type="text" name="lwmTimestamp" value="${isolwm}">
			</div>
			<div class="row-fluid">
				<c:set var="isohwm">
					<fmt:formatDate pattern="yyyy-MM-dd'T'HH:mm:ss"
						value="${hwmTimestamp}" />
				</c:set>
				New / proposed  HWM: <input type="text" name="hwmTimestamp" value="${isohwm}">
			</div>
			<div class="row-fluid">
				<button type="submit" name="reanalyse">Reanalyse with
					proposed LWM / HWM</button>
				<button type="submit" name="setIntertidal">Change the LWM &amp; HWM timestamps</button>
			</div>
		</form>
	</div>
	<!-- /container -->
	<%@ include file="/WEB-INF/jsp/commonFooter.jspFrag"%>
</body>
</html>