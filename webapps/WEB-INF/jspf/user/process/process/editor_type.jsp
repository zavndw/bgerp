<%@ page contentType="text/html; charset=UTF-8"%>
<%@ include file="/WEB-INF/jspf/taglibs.jsp"%>

<html:form action="/user/process">
   	<html:hidden property="typeId"/>  
   	<html:hidden property="id"/> 
	<input type="hidden" name="action" value="processTypeUpdate"/>	

	<h1>Выберите тип:</h1>
	<c:set var="uiid" value="${u:uiid()}"/>
	<div style="background-color: #ffffff; cursor: pointer;" id="${uiid}">
		<c:forEach var="node" items="${typeTreeRoot.childs}">
			<c:set var="node" value="${node}" scope="request"/>
			<c:set var="level" value="0" scope="request"/>
			<jsp:include page="../tree/process_type_tree_item.jsp"/>
		</c:forEach>
	</div>
  	<c:remove var="afterSelectCommand"/>
	<table style="width: 100%;">	
		<%@ include file="editor_save_cancel_tr.jsp"%>
	</table>
		<script>
			$(function()
			{
				var $selectedTitle = $("#${uiid} #${form.param.typeId}_title"); 
				var $parents = $selectedTitle.parents();
				
				var i = 0;
				var $span = $();
				do
   				{
   					var $span = $($parents[i]).find(" > span"); 
   					$span.click();
      				i += 2;
   				}
    			while( $span.length > 0 )
    		})
		</script>
</html:form>