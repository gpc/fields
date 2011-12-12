<div class="fieldcontain ${invalid ? 'error' : ''} ${required ? 'required' : ''}">
	<label for="${property}">${label}<% if (required) { %><span class="required-indicator">*</span><% } %></label>
	${widget}
</div>
