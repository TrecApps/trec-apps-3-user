package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.models.TcUser;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.UserStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.OffsetDateTime;

@Component
public class TrecEmailService
{
	@Autowired
	JavaMailSender mailSender;

	@Autowired
	UserStorageService userStorageService;

	@Autowired
	StateService stateService;

	public boolean validateEmail(TrecAccount account, String enteredCode) throws JsonProcessingException {
		TcUser user = userStorageService.retrieveUser(account.getId());
		if(user.getCodeExpiration() == null)
			return false;
		if(enteredCode.equals(user.getCurrentCode()) && OffsetDateTime.now().isBefore(user.getCodeExpiration()))
		{
			user.setEmailVerified(true);
			userStorageService.saveUser(user);
			return true;
		}
		return false;
	}

	public void sendValidationEmail(TrecAccount account) throws JsonProcessingException, MessagingException {
		TcUser user = userStorageService.retrieveUser(account.getId());

		String code = stateService.generateState();

		// Set Expired for ten Minutes from now
		OffsetDateTime now = OffsetDateTime.now().plusMinutes(10);

		user.setCurrentCode(code);
		user.setCodeExpiration(now);

		userStorageService.saveUser(user);

		sendValidationEmail(user.getEmail(), "Trec-Account: Email Validation", code);
	}
	
	void sendValidationEmail(String to,
            String subject,
            String code) throws MessagingException
	{
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
		
		String url = code;
		
		String htmlEmailContent = "<h1>Please enter the code to Verify your account</h1><br><h2>"+ url +"</h2>";
		
		helper.setSubject(subject);
		helper.setText(htmlEmailContent, true);
		helper.setTo(to);
		mailSender.send(mimeMessage);
	}
}
