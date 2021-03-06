<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<c:set var="uiid" value="${u:uiid()}"/>

<table class="data" style="width: 100%;" id="${uiid}">
	<tr>
		<td>Карта</td>
		<td>Период</td>
		<td>Пакет</td>
		<td>Статус</td>
	</tr>
	<c:forEach var="item" items="${form.response.data.list}">
		<tr>
			<td>${item.card}</td>
			<td nowrap="nowrap">${u:formatDate( item.dateFrom, 'ymd' )} - ${u:formatDate( item.dateTo, 'ymd' )}</td>
			<td>${item.packet}</td>
			<td>${item.status}</td>
		</tr>
	</c:forEach>
</table>