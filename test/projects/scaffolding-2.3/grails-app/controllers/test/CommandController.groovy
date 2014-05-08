package test

class CommandController {

	def index() {
		[command: new LoginCommand()]
	}
	
	def login(LoginCommand command) {
		[message: "Logged in as $command.username with password $command.password"]
	}
	
}

class LoginCommand {

	String username
	String password
	
	static constraints = {
		username blank: false
		password blank: false, password: true
	}
}