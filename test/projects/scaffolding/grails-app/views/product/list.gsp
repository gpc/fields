<%@ page import="test.Product" %>
<!doctype html>
<html>
<head>
	<meta name="layout" content="main">
	<g:set var="entityName" value="${message(code: 'product.label', default: 'Product')}" />
	<title><g:message code="default.list.label" args="[entityName]" /></title>
</head>
<body>
	<a href="#list-product" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
	<div class="nav" role="navigation">
		<ul>
			<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
			<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
		</ul>
	</div>
	<div id="list-product" class="content scaffold-list" role="main">
		<h1><g:message code="default.list.label" args="[entityName]" /></h1>
		<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
		</g:if>
		<table>
			<thead>
				<tr>
					<g:sortableColumn property="name" title="${message(code: 'product.name.label', default: 'Name')}" />
					<g:sortableColumn property="price" title="${message(code: 'product.price.label', default: 'Price')}" />
					<g:sortableColumn property="taxRate" title="${message(code: 'product.taxRate.label', default: 'Tax Rate')}" />
					<g:sortableColumn property="tax" title="${message(code: 'product.tax.label', default: 'Tax')}" />
				</tr>
			</thead>
			<tbody>
				<g:each in="${productInstanceList}" status="i" var="productInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                        <f:display bean="${productInstance}" property="name"><g:link action="show" id="${productInstance.id}">${value}</g:link></f:display>
						<f:display bean="${productInstance}" property="price"><g:formatNumber number="${value}" type="currency" currencyCode="GBP" minFractionDigits="2" maxFractionDigits="2"/></f:display>
						<f:display bean="${productInstance}" property="taxRate">${value}%</f:display>
						<f:display bean="${productInstance}" property="tax"><g:formatNumber number="${value}" type="currency" currencyCode="GBP" minFractionDigits="2" maxFractionDigits="2"/></f:display>
					</tr>
				</g:each>
			</tbody>
		</table>
		<div class="pagination">
			<g:paginate total="${productInstanceTotal}" />
		</div>
	</div>
</body>
</html>
