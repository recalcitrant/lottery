package utils.mail;


import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class StdoutMessageHandlerFactory implements MessageHandlerFactory {

	public MessageHandler create(MessageContext ctx) {
		return new Handler(ctx);
	}

	class Handler implements MessageHandler {

		MessageContext ctx;

		public Handler(MessageContext ctx) {
			this.ctx = ctx;
		}

		public void from(String from) throws RejectException {
			System.out.println("from      : " + from);
		}

		public void recipient(String recipient) throws RejectException {
			System.out.println("recipient : " + recipient);
		}

		public void data(InputStream data) throws IOException {
			try {
				MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()), data);
				System.out.println("subject   : " + msg.getSubject());
				System.out.println();
				System.out.println(msg.getContent());
			} catch (MessagingException e) {
				e.printStackTrace();
			}
			System.out.println("--------------------------------------------------------------");
			System.out.println("");
		}

		public void done() {
		}

	/*	public String convertStreamToString(InputStream is) {
			InputStream stream = null;
			try {
				stream = MimeUtility.decode(is, "quoted-printable");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
			java.util.Scanner s = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A");
			String back = s.hasNext() ? s.next() : "";
			try {
				back = MimeUtility.decodeWord(back);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return back;
		}*/
	}
}