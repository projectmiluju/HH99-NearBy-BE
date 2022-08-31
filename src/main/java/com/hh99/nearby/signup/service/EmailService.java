package com.hh99.nearby.signup.service;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EmailService {

    private JavaMailSender emailSender;

    public void sendSimpleMessage(String email, String nickname) throws MessagingException {
        String Text =
                "<img src='https://thumbnail6.coupangcdn.com/thumbnails/remote/492x492ex/image/retail/images/2021/06/09/17/3/20646f8a-d76c-47b2-869e-7b9f606bb13d.jpg'/>"+
                        "<h1><a href='https://sparta-omj.shop/api/email?nickname=" + nickname +
//                        UUID.randomUUID() +
                        "'>이메일 인증 확인</a></h1>";

        MimeMessage mail = emailSender.createMimeMessage();
        mail.setFrom("${spring.mail.username}");
        mail.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
        mail.setSubject("가입확인 메일입니다.");
        mail.setText(Text,"UTF-8","html");
        emailSender.send(mail);
    }
}
