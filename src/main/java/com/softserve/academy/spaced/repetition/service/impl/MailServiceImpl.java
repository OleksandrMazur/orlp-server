package com.softserve.academy.spaced.repetition.service.impl;

import com.softserve.academy.spaced.repetition.domain.Account;
import com.softserve.academy.spaced.repetition.domain.Person;
import com.softserve.academy.spaced.repetition.domain.User;
import com.softserve.academy.spaced.repetition.security.JwtTokenForMail;
import com.softserve.academy.spaced.repetition.service.MailService;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Service
public class MailServiceImpl implements MailService {
    @Value("${app.origin.url}")
    private String url;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private JwtTokenForMail jwtTokenForMail;
    @Autowired
    @Qualifier("freemarkerConf")
    private Configuration freemarkerConfiguration;

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    private final String REGISTRATION_VERIFICATION_MAIL_TEMPLATE = "registrationVerificationMailTemplate.html";
    private final String ACTIVATION_ACCOUNT_MAIL_TEMPLATE = "activationAccountMailTemplate.html";
    private final String CHANGE_PASSWORD_MAIL_TEMPLATE = "changePasswordMailTemplate.html";
    private final String RESTORE_PASSWORD_MAIL_TEMPLATE = "restorePasswordMailTemplate.html";

    @Override
    public void sendConfirmationMail(User user) {
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setSubject("Confirmation registration");
            Person person = user.getPerson();
            Account account = user.getAccount();
            String email = account.getEmail();
            helper.setTo(email);
            Map<String, Object> model = new HashMap<>();
            String token = jwtTokenForMail.generateTokenForMail(email);
            model.put("person", person);
            model.put("token", token);
            model.put("url", url);
            String text = getTemplateContentForMail(REGISTRATION_VERIFICATION_MAIL_TEMPLATE, model);
            helper.setText(text, true);
        };
        mailSender.send(preparator);
    }

    @Override
    public void sendActivationMail(String email) {
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setSubject("Activation account");
            helper.setTo(email);
            Map<String, Object> model = new HashMap<>();
            String token = jwtTokenForMail.generateTokenForMail(email);
            model.put("token", token);
            model.put("url", url);
            String text = getTemplateContentForMail(ACTIVATION_ACCOUNT_MAIL_TEMPLATE, model);
            helper.setText(text, true);
        };
        mailSender.send(preparator);
    }

    @Override
    public void sendPasswordNotificationMail(User user) {
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setSubject("Change password notification");
            helper.setTo(user.getAccount().getEmail());
            Map<String, Object> model = new HashMap<>();
            model.put("person", user.getPerson());
            model.put("datachange", Calendar.getInstance().getTime().toString());
            model.put("url", url);
            String text = getTemplateContentForMail(CHANGE_PASSWORD_MAIL_TEMPLATE, model);
            helper.setText(text, true);
        };
        mailSender.send(preparator);
    }

    @Override
    public void sendPasswordRestoreMail(String accountEmail) {
        LOGGER.debug("Send mail for reset password to email: {}", accountEmail);
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setSubject("Password restore");
            helper.setTo(accountEmail);
            Map<String, Object> model = new HashMap<>();
            String token = jwtTokenForMail.generateTokenForMail(accountEmail);
            model.put("token", token);
            model.put("url", url);
            String text = getTemplateContentForMail(RESTORE_PASSWORD_MAIL_TEMPLATE, model);
            helper.setText(text, true);
        };
        mailSender.send(preparator);
    }

    @Override
    public String getTemplateContentForMail(String mailTemplate, Map<String, Object> model) {
        StringBuilder content = new StringBuilder();
        try {
            content.append(FreeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfiguration.getTemplate(mailTemplate), model));
            return content.toString();
        } catch (IOException | TemplateException e) {
            LOGGER.error("Couldn't generate email content.", e);
        }
        return "";
    }

}
