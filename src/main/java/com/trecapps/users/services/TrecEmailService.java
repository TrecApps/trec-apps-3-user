package com.trecapps.users.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.trecapps.auth.common.models.TcUser;
import com.trecapps.auth.common.models.primary.TrecAccount;
import com.trecapps.auth.webflux.services.IUserStorageServiceAsync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@Slf4j
public class TrecEmailService
{
	@Autowired
	JavaMailSender mailSender;

	@Autowired
	IUserStorageServiceAsync userStorageService;

	@Autowired
	StateService stateService;

	public Mono<Boolean> validateEmail(TrecAccount account, String enteredCode) throws JsonProcessingException {

		return userStorageService.getAccountById(account.getId())
				.map((Optional<TcUser> optUser) -> {
					if(optUser.isEmpty())
					{
						log.info("User Not Found");
						return false;
					}
					TcUser user = optUser.get();
					if(user.getCodeExpiration() == null)
						return false;
					if(enteredCode.equals(user.getCurrentCode()) && OffsetDateTime.now().isBefore(user.getCodeExpiration()))
					{
						user.setEmailVerified(true);
						userStorageService.saveUser(user);
						return true;
					}
					return false;
				});
	}

	boolean isPast1(TcUser user){
		OffsetDateTime exp = user.getCodeExpiration();
		if(exp == null)
			return false;

		return OffsetDateTime.now().isBefore(exp.minusMinutes(9));
	}

	public Mono<Boolean> sendValidationEmail(TrecAccount account) throws JsonProcessingException, MessagingException {

		return userStorageService.getAccountById(account.getId())
				.map((Optional<TcUser> optUser) -> {
					if (optUser.isEmpty()) {
						log.info("User Not Found");
						return false;
					}
					TcUser user = optUser.get();
					if(isPast1(user))
						return false;

					String code = stateService.generateState();

					// Set Expired for ten Minutes from now
					OffsetDateTime now = OffsetDateTime.now().plusMinutes(10);

					user.setCurrentCode(code);
					user.setCodeExpiration(now);

					userStorageService.saveUser(user);

                    try {
                        sendValidationEmail(user.getEmail(), "Trec-Account: Email Validation", code);
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }

                    return true;
				});



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
