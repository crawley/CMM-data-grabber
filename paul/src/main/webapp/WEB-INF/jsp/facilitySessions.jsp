<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="false"%>
<html lang="en">
<head>
<%@ include file="/WEB-INF/jsp/commonHead.jspFrag"%>
<title>Data Grabber - Facility Session Log</title>
</head>
<body>
	<%@ include file="/WEB-INF/jsp/commonHeader.jspFrag"%>
	<div class="container-fluid">
		<h1>Facility Session Log for '${facilityName}</h1>
		<c:choose>
			<c:when test="${empty sessions}">
		    	There are no sessions recorded for this facility.
		    </c:when>
			<c:otherwise>
				<table class="table table-striped table-bordered table-condensed">
					<thead>
						<tr>
							<th>User name</th>
							<th>Account name</th>
							<th>Started</th>
							<th>Ended</th>
							<th>Uuid</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${sessions}" var="session">
							<tr>
								<td>${session.userName}</td>
								<td>${session.account}</td>
								<td>${session.loginTime}</td>
								<td>${empty session.logoutTime ? '-' : session.logoutTime}</td>
								<td>${session.sessionUuid}</td>
							</tr>
						</c:forEach>
					</tbody>
				</table>
			</c:otherwise>
		</c:choose>
	</div>
	<!-- /container -->
	<%@ include file="/WEB-INF/jsp/commonFooter.jspFrag"%>
</body>
</html>