package ua.dirproy.profelumno.teachermodification.controller;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.netty.handler.codec.http.HttpRequest;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import scala.util.parsing.json.JSONArray$;
import ua.dirproy.profelumno.common.models.Teacher;
import ua.dirproy.profelumno.teachermodification.view.html.*;
import ua.dirproy.profelumno.user.models.Subject;
import ua.dirproy.profelumno.user.models.User;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by francisco on 13/09/15.
 */
public class ModifyTeacher extends Controller {

    public static Result profileView() {
        return ok(teacherprofile.render());
    }

    public static Result getTeacher(){
        //final long userId=Long.parseLong(session("id"));
        long userId=1;
        Teacher teacher = Ebean.find(Teacher.class, userId);
        JsonNode json= Json.toJson(teacher);
        System.out.println(json);
        return ok(json);
    }

    public static Result saveTeacherInfo(){
        final long userId=Long.parseLong(session("id"));
        Teacher teacher = Ebean.find(Teacher.class, userId);
        return ok();
    }

    public static Result saveSubject(){
        Form<Teacher> form = Form.form(Teacher.class).bindFromRequest();
        if (form.hasErrors())
            return badRequest("Error in form");
        Teacher aux=form.get();
        final long userId=Long.parseLong(session("id"));
        Teacher teacher = Ebean.find(Teacher.class, userId);
        //teacher.setSubjects(aux.getSubjects());
        Ebean.save(teacher);
        return ok();
    }
    public static Result savePicture() {
       final Http.MultipartFormData body = request().body().asMultipartFormData();
        final Http.MultipartFormData.FilePart picture = body.getFile("fileInput");
        if (picture != null) {
            final String fileName = picture.getFilename();
            final String suffix = fileName.substring((fileName.length() - 4));
            System.out.println("suffix = " + suffix);
            if (suffix.equals(".jpg") || suffix.equals("jpeg") || suffix.equals(".png") || suffix.equals(".bmp")) {
                final String contentType = picture.getContentType();
                final File file = picture.getFile();
                if (contentType.contains("image")) {
                    final long userId = Long.parseLong(session("id"));
                    Teacher teacher = Ebean.find(Teacher.class, userId);
                    teacher.setProfilePicture(file.toString().getBytes());
                    Ebean.update(teacher);
                    return ok(file);
                }
            }
        }
        return ok("yfvygfvyg");
    }
}
