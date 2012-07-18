
<%@ page import="test.Product" %>
<!doctype html>
<html>
<head>
	<meta name="layout" content="main">
	<g:set var="entityName" value="${message(code: 'product.label', default: 'Product')}" />
	<title><g:message code="default.show.label" args="[entityName]" /></title>
</head>
<body>
	<a href="#show-product" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
	<div class="nav" role="navigation">
		<ul>
			<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
			<li><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
			<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
		</ul>
	</div>
	<div id="show-product" class="content scaffold-show" role="main">
		<h1><g:message code="default.show.label" args="[entityName]" /></h1>
		<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
		</g:if>
		<ol class="property-list product">
			<f:display bean="productInstance" property="name"/>
			<f:display bean="productInstance" property="price"><g:formatNumber number="${value}" type="currency" currencyCode="GBP" minFractionDigits="2" maxFractionDigits="2"/></f:display>
			<f:display bean="productInstance" property="taxRate">${value}%</f:display>
			<f:display bean="productInstance" property="tax"><g:formatNumber number="${value}" type="currency" currencyCode="GBP" minFractionDigits="2" maxFractionDigits="2"/></f:display>
		</ol>
		<g:form>
			<fieldset class="buttons">
				<g:hiddenField name="id" value="${productInstance?.id}" />
				<g:link class="edit" action="edit" id="${productInstance?.id}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
				<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
			</fieldset>
		</g:form>
	</div>
</body>
</html>
