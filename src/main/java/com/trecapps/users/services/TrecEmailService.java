package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.models.TcUser;
import com.trecapps.auth.models.primary.TrecAccount;
import com.trecapps.auth.services.core.IUserStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;

@Component
public class TrecEmailService
{
	@Autowired
	JavaMailSender mailSender;

	@Autowired
	IUserStorageService userStorageService;

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

	boolean isPast1(TcUser user){
		OffsetDateTime exp = user.getCodeExpiration();
		if(exp == null)
			return false;

		return OffsetDateTime.now().isBefore(exp.minusMinutes(9));
	}

	public boolean sendValidationEmail(TrecAccount account) throws JsonProcessingException, MessagingException {
		TcUser user = userStorageService.retrieveUser(account.getId());

		if(isPast1(user))
			return false;

		String code = stateService.generateState();

		// Set Expired for ten Minutes from now
		OffsetDateTime now = OffsetDateTime.now().plusMinutes(10);

		user.setCurrentCode(code);
		user.setCodeExpiration(now);

		userStorageService.saveUser(user);

		sendValidationEmail(user.getEmail(), "Trec-Account: Email Validation", code);

		return true;
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
