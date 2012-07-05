<% import grails.persistence.Event %>
<%=packageName%>
<!doctype html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="\${message(code: '${domainClass.propertyName}.label', default: '${className}')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-${domainClass.propertyName}" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                                                  default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="\${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]"/></g:link></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="show-${domainClass.propertyName}" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="\${flash.message}">
        <div class="message" role="status">\${flash.message}</div>
    </g:if>

    <ol class="property-list ${domainClass.propertyName}">
        <f:all bean="${propertyName}" fieldTemplateName="showField" widgetTemplateName="show"/>
    </ol>

    <g:form>
        <fieldset class="buttons">
            <g:hiddenField name="id" value="\${${propertyName}?.id}"/>
            <g:link class="edit" action="edit" id="\${${propertyName}?.id}"><g:message code="default.button.edit.label"
                                                                                       default="Edit"/></g:link>
            <g:actionSubmit class="delete" action="delete"
                            value="\${message(code: 'default.button.delete.label', default: 'Delete')}"
                            onclick="return confirm('\${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
        </fieldset>
    </g:form>
</div>
</body>
</html>
