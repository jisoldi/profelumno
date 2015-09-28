package ua.dirproy.profelumno.teachermodification.controller;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import ua.dirproy.profelumno.common.models.Teacher;
import ua.dirproy.profelumno.teachermodification.view.html.*;
import ua.dirproy.profelumno.user.models.Subject;
import ua.dirproy.profelumno.user.models.User;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Created by francisco on 13/09/15.
 */
public class ModifyTeacher extends Controller {

    public static Result profileView() {
        return ok(teacherprofile.render());
    }

    public static Result getTeacher(){
        final long userId=Long.parseLong(session("id"));
        User user = Ebean.find(User.class, userId);
        Teacher teacher =Teacher.finder.where().eq("user",user).findUnique();
        JsonNode json= Json.toJson(teacher);
        return ok(json);
    }

    public static Result saveTeacherInfo(){
        Form<Teacher> form = Form.form(Teacher.class).bindFromRequest();
        if (form.hasErrors()) {
//            return badRequest(register.render());
            return badRequest("Error in form");
        }
        Teacher tch = form.get();
        Teacher teacher = Ebean.find(Teacher.class, tch.getUser().getId());
        if ((tch.getUser().getEmail()).equalsIgnoreCase(teacher.getUser().getEmail())||
                User.validateEmailUnique(tch.getUser().getEmail())) {
            //teacher.setProfilePicture(tch.getProfilePicture());
            User teacherU=teacher.getUser();
            User tchU=tch.getUser();
            teacherU.setAddress(tchU.getAddress());
            teacherU.setBirthday(tchU.getBirthday());
            teacherU.setEmail(tchU.getEmail());
            teacherU.setGender(tchU.getGender());
            teacherU.setName(tchU.getName());
            teacherU.setPassword(tchU.getPassword());
            teacherU.setSurname(tchU.getSurname());
            Ebean.save(teacher);
            Ebean.save(teacher.getUser());
            System.out.println(Teacher.list().get(0).getUser().getName());
            return ok(Json.toJson(teacher));
        }else {
            return badRequest("Unique");
        }
    }

    public static Result getSubject(){
        List<Subject> subjects=Ebean.find(Subject.class).findList();
        JsonNode json=Json.toJson(subjects);
        return ok(json);
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

    public static Result getPicture(){
        final long userId=Long.parseLong(session("id"));
        User user = Ebean.find(User.class, userId);
        Teacher teacher =Teacher.finder.where().eq("user",user).findUnique();
        return ok(teacher.getUser().getProfilePicture());
    }

    public static Result savePicture() {
        final Http.MultipartFormData body = request().body().asMultipartFormData();
        final Http.MultipartFormData.FilePart picture = body.getFile("file");
        if (picture != null) {
            final String fileName = picture.getFilename();
            final String suffix = fileName.substring((fileName.length() - 4));
            if (suffix.equals(".jpg") || suffix.equals("jpeg") || suffix.equals(".png") || suffix.equals(".bmp")) {
                final String contentType = picture.getContentType();
                final File file = picture.getFile();
                if (contentType.contains("image")) {
                    final long userId = Long.parseLong(session("id"));
                    Teacher teacher = Ebean.find(Teacher.class, userId);
                    byte[] bfile=null;
                    try {
                        bfile=Files.toByteArray(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    teacher.getUser().setProfilePicture(Base64.getEncoder().encode(bfile));
                    Ebean.save(teacher);
                    Ebean.save(teacher.getUser());
                    return ok(teacher.getUser().getProfilePicture());
                }
            }
        }
        return ok("yfvygfvyg");
    }
}
