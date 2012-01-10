package test

class CustomController {

	def index() { }

	def login(String username, String password) {
		[message: "Logged in as $username with password $password"]
	}

}
