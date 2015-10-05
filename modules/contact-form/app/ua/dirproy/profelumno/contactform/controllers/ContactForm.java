package ua.dirproy.profelumno.contactform.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import ua.dirproy.profelumno.contactform.view.html.*;
import ua.dirproy.profelumno.mailsender.models.MailSenderUtil;

/**
 * Created by yankee on 28/09/15.
 */
public class ContactForm extends Controller {

    public static Result contactFormView() {
        return ok(contactform.render());
    }

    public static Result sendForm() {
        DynamicForm requestData = DynamicForm.form().bindFromRequest();
        String name, email, subject, message;
        name = requestData.get("name");
        email = requestData.get("email");
        subject = requestData.get("subject");
        message = requestData.get("message");
        final JsonNode result = Json.toJson("Mail enviado");
        /*result.put("name", name);
        result.put("email", email);
        result.put("subject", subject);
        result.put("message", message);*/

        String messageToSend = "New message from: " + name + "\nAt: " + email + "\n\n" + message;
        try {
            MailSenderUtil.send(new String[]{"emailToSendTo@email.com"}, subject, messageToSend);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ok(contactform.render());
    }
}
