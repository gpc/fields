<table>
    <thead>
         <tr>
            <g:each in="${domainProperties}" var="p" status="i">
                <g:sortableColumn property="${p.name}" title="\${message(code: '${domainClass.propertyName}.${p.name}.label', default: '${p.naturalName}')}" />
            </g:each>
        </tr>
    </thead>
    <tbody>
        <g:each in="${collection}" var="bean" status="i">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <g:each in="${domainProperties}" var="p">
                     <td><f:display bean="${bean}" property="${p.name}" /></td>
                </g:each>
            </tr>
        </g:each>
    </tbody>
</table>