<!doctype html>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
	<head>
		<title>Login</title>
	</head>

	<body>
		<g:form action="login">
			<form:field bean="command" property="username"/>
			<form:field bean="command" property="password"/>
			<button type="submit">Login</button>
		</g:form>
	</body>
</html>