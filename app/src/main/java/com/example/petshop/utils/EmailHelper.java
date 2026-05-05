package com.example.petshop.utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailHelper {
    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendOTP(String toEmail, String otp, EmailCallback callback) {
        final String senderEmail = "nguyenductho0411@gmail.com";
        final String senderPassword = "gfvl elnj hpzw lcpu";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        new Thread(() -> {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Mã xác thực đăng ký Petshop");
                message.setText("Mã OTP của bạn là: " + otp + "\n\nVui lòng không chia sẻ mã này cho bất kỳ ai.");

                Transport.send(message);
                if (callback != null) callback.onSuccess();
            } catch (MessagingException e) {
                e.printStackTrace();
                if (callback != null) callback.onFailure("Lỗi gửi email: " + e.getMessage());
            }
        }).start();
    }
}
