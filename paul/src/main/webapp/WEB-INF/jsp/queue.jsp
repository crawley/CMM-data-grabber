<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="false"%>
<html lang="en">
<head>
<%@ include file="/WEB-INF/jsp/commonHead.jspFrag"%>
<title>Data Grabber Ingestion Queue Admin</title>
</head>
<body>
	<%@ include file="/WEB-INF/jsp/commonHeader.jspFrag"%>
	<div class="container">
		<h1>Data Grabber Ingestion Queue Admin</h1>
		<c:if test="${!empty queue}">
			<form method="POST" action="ingestible">
				<button type="submit" name="deleteAll">Delete/Archive all
					entries</button>
				<button type="submit" name="expire">Expire entries</button>
			</form>
			<ul>
				<c:forEach items="${queue}" var="entry">
					<li><a href="ingestible/${entry.id}">Entry # ${entry.id}</a> -
						facility ${entry.facilityName}, user : ${entry.userName},
						captured: ${entry.captureTimestamp}
						<form method="POST" action="ingestible/${entry.id}">
							<button type="submit" name="mode" value="discard">Delete</button>
							<button type="submit" name="mode" value="archive">Archive</button>
							<input type="hidden" name="confirmed"> <input
								type="hidden" name="delete">
						</form></li>
				</c:forEach>
			</ul>
		</c:if>
		<c:if test="${empty queue}">
            Ingestion queue is empty
        </c:if>
	</div>
	<!-- /container -->
	<%@ include file="/WEB-INF/jsp/commonFooter.jspFrag"%>
</body>
</html>