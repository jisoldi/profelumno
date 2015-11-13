package ua.dirproy.profelumno.calendar.controllers;

import authenticate.Authenticate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import ua.dirproy.profelumno.calendar.views.html.calendar;
import ua.dirproy.profelumno.common.models.*;
import ua.dirproy.profelumno.user.models.User;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Authenticate({Teacher.class, Student.class})
public class Calendar extends Controller {

    public static Result calendarView() { return ok(calendar.render()); }

    public static Result getUserName() {
        User user = getUser();
        return user != null ? ok(user.getName() + " " + user.getSurname()) : badRequest();
    }

    private static User getUser() {
        return User.getUser(Long.parseLong(session("id")));
    }

    private static Teacher getTeacher() {
        List<Teacher> teachers = Teacher.list();
        for (int i = 0; i < teachers.size(); i++) {
            Teacher aux = teachers.get(i);
            if (aux.getUser().getId().equals(getUser().getId())) {
                return Teacher.getTeacher(aux.getId());
            }
        }
        return null;
    }

    private static Student getStudent(){
        List<Student> student = Student.list();
        for (int i = 0; i < student.size(); i++) {
            Student aux = student.get(i);
            if (aux.getUser().getId().equals(getUser().getId())) {
                return Student.getStudent(aux.getId());
            }
        }
        return null;
    }

    private static List<Lesson> myLessons(){
        User user = getUser();
        List<Lesson> lessons = Lesson.list();
        List<Lesson> myLessons = new ArrayList<>();
        for (Lesson aux : lessons) {
            if (aux.getTeacher().getUser().getId().equals(user.getId())){
                myLessons.add(aux);
            }
            else if (aux.getStudent().getUser().getId().equals(user.getId())){
                myLessons.add(aux);
            }
        }
        return myLessons;
    }

    private static List<Lesson> myLessons(Teacher teacher){
        List<Lesson> lessons = Lesson.list();
        List<Lesson> myLessons = new ArrayList<>();
        for (Lesson aux : lessons) {
            if (aux.getTeacher().getId().equals(teacher.getId())) {
                myLessons.add(aux);
            }
        }
        return myLessons;
    }

    public static Result getLessonAccepted(){
        List<Lesson> lessons = myLessons();
        List<Lesson> acceptLessons = new ArrayList<>();
        for (Lesson aux : lessons) {
            if (aux.getLessonState() == 1) {
                acceptLessons.add(aux);
            }
        }
        return ok(Json.toJson(acceptLessons));
    }

    public static Result getCalendar(){
        List<DayRange> calendar;
        ArrayNode result = Json.newArray();
        if (getTeacher() != null){
            calendar = getTeacher().getCalendar();
            for (DayRange day : calendar){

                ObjectNode obj = Json.newObject();
                obj.put("day", day.getDayEnum().getDayName());
                obj.put("fromHour", day.getFromHour().toString());
                obj.put("toHour", day.getToHour().toString());
                result.add(obj);

            }
        }

        return ok(Json.toJson(result));
    }


    public static Result updateCalendar() throws IOException {
        JsonNode updateJson = request().body().asJson();
        if(updateJson == null) {
            return badRequest("Expecting Json data");
        }
        else {
            Date date = deserialize(updateJson.get("date"));
            Date fromHour = deserialize(updateJson.get("fromHour"));
            Date toHour = deserialize(updateJson.get("toHour"));
            Teacher teacher;
            if (getTeacher() != null) {
                teacher = getTeacher();
                teacher.updateCalendar(date, fromHour, toHour);
                return ok();
            }
            else {
                return badRequest("Teacher == null");
            }
        }

    }

    private static Date deserialize(JsonNode jsonparser) throws IOException{

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date = jsonparser.asText();
        try {
            return format.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

    public static Result teacherAvailableTimeWithNoClass(){

        Form<Teacher> form = Form.form(Teacher.class).bindFromRequest();
        Long teacherId = Long.parseLong(form.data().get("teacherId"));
        final Teacher teacher = Teacher.getTeacher(teacherId);

        List<DayRange> calendar = teacher.getCalendar();

        //get acceptedLesson from teacher
        List<Lesson> lessons = myLessons(teacher);
        List<Lesson> acceptLessons = new ArrayList<>();
        for (Lesson aux : lessons) {
            if (aux.getLessonState() == 1) {
                acceptLessons.add(aux);
            }
        }

        Lesson auxLesson = new Lesson();
        auxLesson.setDateTime(new Date(new Date().getTime() + 600000));
        auxLesson.setDuration(Duration.ofHours(1));
        boolean emptyLessonsFlag = false;

        if (acceptLessons.isEmpty()) {
            acceptLessons.add(auxLesson);
            emptyLessonsFlag = true;
        }

        List<Day> dayList = new ArrayList<>();
        //voy sacando del calendario del teacher los horarios que ya tienen clases
        Date today = new Date();
        for (Lesson aux : acceptLessons){
            final DayEnum dayEnum = auxiliaryMethod(aux.getDateTime());
            int durationOfClass = (int) (aux.getDuration().getSeconds() / 3600);
            int fromHour = aux.getDateTime().getHours();
            int toHour = fromHour + durationOfClass;
            if (aux.getDateTime().after(today)) {
                for (DayRange dr : calendar) {
                    if (dr.getDayEnum() == dayEnum) {
                        if (!dayList.isEmpty()) {
                            for (Day auxDay : dayList) {
                                if (auxDay.getDay().getYear() == aux.getDateTime().getYear() && auxDay.getDay().getMonth() == aux.getDateTime().getMonth() && auxDay.getDay().getDay() == aux.getDateTime().getDay()) {
                                    List<Range> newRangeList = new ArrayList<>();
                                    List<Range> rangeList = auxDay.getRangeList();
                                    for (Range x : rangeList) {
                                        if (fromHour > x.getFrom() && toHour < x.getTo()) {
                                            Range rangeFrom = new Range();
                                            rangeFrom.setFrom(x.getFrom());
                                            rangeFrom.setTo(fromHour);
                                            Range rangeTo = new Range();
                                            rangeTo.setFrom(toHour);
                                            rangeTo.setTo(x.getTo());
                                            newRangeList.add(rangeFrom);
                                            newRangeList.add(rangeTo);
                                        } else if (fromHour == x.getFrom() && toHour < x.getTo()) {
                                            Range rangeFrom = new Range();
                                            rangeFrom.setFrom(toHour);
                                            rangeFrom.setTo(x.getTo());
                                            newRangeList.add(rangeFrom);
                                        } else if (fromHour > x.getFrom() && toHour == x.getTo()) {
                                            Range rangeFrom = new Range();
                                            rangeFrom.setFrom(x.getFrom());
                                            rangeFrom.setTo(fromHour);
                                            newRangeList.add(rangeFrom);
                                        }
                                    }

                                    auxDay.setRangeList(newRangeList);
                                    break;
                                } else {
                                    Day day = new Day();
                                    day.setDay(aux.getDateTime());
                                    if (fromHour > dr.getFromHour().getHours() && toHour < dr.getToHour().getHours()) {
                                        Range rangeFrom = new Range();
                                        rangeFrom.setFrom(dr.getFromHour().getHours());
                                        rangeFrom.setTo(fromHour);
                                        Range rangeTo = new Range();
                                        rangeTo.setFrom(toHour);
                                        rangeTo.setTo(dr.getToHour().getHours());
                                        day.addRange(rangeFrom);
                                        day.addRange(rangeTo);
                                        ;
                                    } else if (fromHour == dr.getFromHour().getHours() && toHour < dr.getToHour().getHours()) {
                                        Range rangeTo = new Range();
                                        rangeTo.setFrom(toHour);
                                        rangeTo.setTo(dr.getToHour().getHours());
                                        day.addRange(rangeTo);
                                    } else if (fromHour > dr.getFromHour().getHours() && toHour == dr.getToHour().getHours()) {
                                        Range rangeFrom = new Range();
                                        rangeFrom.setFrom(dr.getFromHour().getHours());
                                        rangeFrom.setTo(fromHour);
                                        day.addRange(rangeFrom);

                                    }
                                    dayList.add(day);
                                    break;
                                }
                            }
                        } else {
                            Day day = new Day();
                            day.setDay(aux.getDateTime());
                            if (fromHour > dr.getFromHour().getHours() && toHour < dr.getToHour().getHours()) {
                                Range rangeFrom = new Range();
                                rangeFrom.setFrom(dr.getFromHour().getHours());
                                rangeFrom.setTo(fromHour);
                                Range rangeTo = new Range();
                                rangeTo.setFrom(toHour);
                                rangeTo.setTo(dr.getToHour().getHours());
                                day.addRange(rangeFrom);
                                day.addRange(rangeTo);
                                ;
                            } else if (fromHour == dr.getFromHour().getHours() && toHour < dr.getToHour().getHours()) {
                                Range rangeTo = new Range();
                                rangeTo.setFrom(toHour);
                                rangeTo.setTo(dr.getToHour().getHours());
                                day.addRange(rangeTo);
                            } else if (fromHour > dr.getFromHour().getHours() && toHour == dr.getToHour().getHours()) {
                                Range rangeFrom = new Range();
                                rangeFrom.setFrom(dr.getFromHour().getHours());
                                rangeFrom.setTo(fromHour);
                                day.addRange(rangeFrom);

                            }
                            dayList.add(day);
                            break;
                        }
                    }
                }
            }
        }

        Date auxDate =new Date();
        java.util.Calendar cal = new GregorianCalendar();
        cal.setTime(auxDate);
        for (int i = 0; i <60 ; i++) {
            for (Day auxDay : dayList){
                if (auxDay.getDay().getYear() != cal.get(java.util.Calendar.YEAR) || auxDay.getDay().getMonth() != cal.get(java.util.Calendar.MONTH) || auxDay.getDay().getDay() != cal.get(java.util.Calendar.DAY_OF_YEAR)){
                    final DayEnum dayEnum = auxiliaryMethod(cal.getTime());
                    for (DayRange dr : calendar) {
                        if (dr.getDayEnum() == dayEnum) {
                            Day day = new Day();
                            day.setDay(cal.getTime());
                            Range range = new Range();
                            range.setFrom(dr.getFromHour().getHours());
                            range.setTo(dr.getToHour().getHours());
                            day.addRange(range);
                            dayList.add(day);
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                            break;
                        }
                    }
                }
                break;

            }

        }

        if (emptyLessonsFlag) acceptLessons.remove(auxLesson);


        ArrayNode arrayNode = Json.newArray().add(Json.toJson(acceptLessons)).add(Json.toJson(dayList));

        return ok(Json.toJson(arrayNode));
    }



    private static DayEnum auxiliaryMethod(Date date){
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int day = cal.get(java.util.Calendar.DAY_OF_WEEK);
        DayEnum dayEnum = null;
        if (java.util.Calendar.MONDAY == day){
            dayEnum = DayEnum.MONDAY;
        }
        if (java.util.Calendar.TUESDAY == day){
            dayEnum = DayEnum.TUESDAY;
        }
        if (java.util.Calendar.WEDNESDAY == day){
            dayEnum = DayEnum.WEDNESDAY;
        }
        if (java.util.Calendar.THURSDAY == day){
            dayEnum = DayEnum.THURSDAY;
        }
        if (java.util.Calendar.FRIDAY == day){
            dayEnum = DayEnum.FRIDAY;
        }
        if (java.util.Calendar.SUNDAY == day){
            dayEnum = DayEnum.SUNDAY;
        }
        if (java.util.Calendar.SATURDAY == day){
            dayEnum = DayEnum.SATURDAY;
        }
        return dayEnum;
    }
}
