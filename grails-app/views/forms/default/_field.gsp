<div class="fieldcontain ${invalid ? 'error' : ''} ${required ? 'required' : ''}">
	<label for="${property}">${label}<g:if test="${required}"><span class="required-indicator">*</span></g:if></label>
	${widget}
</div>
