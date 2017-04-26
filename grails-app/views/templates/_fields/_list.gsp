<ol class="property-list ${domainClass.propertyName}">
    <g:each in="${domainProperties}" var="p">
        <li class="fieldcontain">
            <span id="${p.name}-label" class="property-label"><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /></span>
            <div class="property-value" aria-labelledby="${p.name}-label">${body(p)}</div>
        </li>
    </g:each>
</ol>