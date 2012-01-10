<!doctype html>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
	<head>
		<title>Login</title>
	</head>

	<body>
		<g:form action="login">
			<f:field property="username"/>
			<f:field property="password">
				<input type="password" name="${property}">
			</f:field>
			<button type="submit">Login</button>
		</g:form>
	</body>
</html>