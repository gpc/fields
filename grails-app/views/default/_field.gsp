<g:set var="classes" value="fieldcontain "/>
<g:if test="${required}">
    <g:set var="classes" value="${classes + 'required '}"/>
</g:if>
<g:if test="${invalid}">
    <g:set var="classes" value="${classes + 'error '}"/>
</g:if>
<div class="${classes}">
    <label for="${prefix}${property}">${label}<g:if test="${required}"><span class="required-indicator">*</span></g:if></label>
    <%= widget %>
</div>