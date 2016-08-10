package utils.mail;

import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

public class BasicSMTPServer {

	private static SMTPServer smtpServer;

	public static void start(MessageHandlerFactory factory) {
		smtpServer = new SMTPServer(factory);
		smtpServer.setPort(9876);
		smtpServer.start();
	}

	public static void stop() {
		smtpServer.stop();
	}
}