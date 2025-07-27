package com.example.monyormsauth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    /**
     * Email göndərmək üçün sadə və effektiv metod.
     * @param to      Kimə göndəriləcək
     * @param subject Mövzu
     * @param body    HTML və ya plain mətn body
     */
    @Async("taskExecutor") // paralel threaddə işləyir, mail gözlətmir
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // `true` HTML formatındadır

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email göndərilərkən xəta baş verdi: " + e.getMessage());
        }
    }
}
