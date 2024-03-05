from locust import task, FastHttpUser

class HellowWorld(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0

    @task
    def hello(self):
        self.client.get("/hello")