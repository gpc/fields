<ol class="property-list ${domainClass.propertyName}">
    <g:each in="${domainClass.persistentProperties}" var="p">
        <li class="fieldcontain">
            <span id="${p.name}-label" class="property-label"><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /></span>
            <span class="property-value" aria-labelledby="${p.name}-label">${body(p)}</span>
        </li>
    </g:each>
</ol>