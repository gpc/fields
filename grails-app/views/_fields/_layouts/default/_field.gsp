<%@ page defaultCodec="html" %>
<div class="form-group ${invalid ? 'error' : ''}">
	<label class="col-sm-2 control-label" for="${property}">${label} - from grails-fields-plugin</label>
	<div class="col-sm-10">
		<%= raw(widget) %>
        <g:if test="${required}">
            <span class="required-indicator">*</span>
        </g:if>
	</div>
</div>