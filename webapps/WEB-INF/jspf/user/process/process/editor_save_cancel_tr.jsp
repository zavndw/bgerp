<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<tr>
	<td valign="top" class="pt1 pb1">
		<c:set var="closeEditor">$$.ajax.load('${form.returnUrl}', $('#${form.returnChildUiid}').parent());</c:set>	
		<c:set var="saveCommand">$$.ajax.post(this.form).done(() => {${closeEditor}})</c:set>
	
		<button class="btn-grey mr1" type="button" onclick="${saveCommand}">${l.l('ОК')}</button>
		<button class="btn-grey mr1" type="button" onclick="${closeEditor}">${l.l('Отмена')}</button>
	</td>
</tr>