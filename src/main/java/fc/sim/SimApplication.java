package fc.sim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimApplication {

	public static Workbench workbench;
	public static RobotRunner robotRunner;

	public static void main(String[] args) {
		SpringApplication.run(SimApplication.class, args);
		workbench = new Workbench();
		robotRunner = new RobotRunner();
		Thread robotThread = new Thread(robotRunner);
		robotThread.start();
	}

}
