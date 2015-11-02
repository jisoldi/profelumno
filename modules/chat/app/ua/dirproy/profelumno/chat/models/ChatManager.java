package ua.dirproy.profelumno.chat.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;
import ua.dirproy.profelumno.common.models.Lesson;
import ua.dirproy.profelumno.common.models.Student;
import ua.dirproy.profelumno.common.models.Teacher;
import ua.dirproy.profelumno.user.models.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private static Map<Long, WebSocket.Out<JsonNode>> map = new ConcurrentHashMap<>();
    private static Map<Long, WebSocket<JsonNode>> connections = new ConcurrentHashMap<>();
    private static ChatManager ourInstance = new ChatManager();

    public static ChatManager getInstance() {
        return ourInstance;
    }

    private ChatManager() {}

    public static void start(Long userId, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out,
                             WebSocket<JsonNode> connection){
        addConnection(userId, out, connection);

        in.onMessage(jsonNode -> {
            Long idUserFrom = jsonNode.findPath("idUserFrom").asLong();
            String message = jsonNode.findPath("message").asText();
            Long idChat = jsonNode.findPath("idChat").asLong();

            notifyMsg(idUserFrom, message, idChat);
        });

        in.onClose(() -> {
            notifyDisconnection(userId);
        });

        notifyUsersConnections(userId);
    }

    public static void addConnection(Long userId, WebSocket.Out<JsonNode> socketOut, WebSocket<JsonNode> socket){
        connections.put(userId, socket);
        map.put(userId, socketOut);
    }

    public static WebSocket<JsonNode> userSocket(Long userId){
        return connections.containsKey(userId) ? connections.get(userId) : null;
    }

    public static void notifyUsersConnections(Long userId){
        ObjectNode node = Json.newObject();
        List<User> connected = new ArrayList<>();
        List<User> disconnected = new ArrayList<>();

        for (Long aLong : getUsersRelated(userId)) {
            User temp = User.getUser(aLong);
            WebSocket.Out<JsonNode> out = map.get(temp.getId());
            if (out != null){
                connected.add(temp);
            } else {
                disconnected.add(temp);
            }
        }

        node.put("type", "users");
        node.put("connectedUsers", Json.toJson(connected));
        node.put("disconnectedUsers", Json.toJson(disconnected));

        map.get(userId).write(node);

        notifyIConnected(connected, User.getUser(userId));
    }

    public static void notifyIConnected(List<User> connected, User me){
        for (User user : connected){
            ObjectNode node = Json.newObject();
            node.put("type", "user");
            node.put("user", Json.toJson(me));
            node.put("connected", true);

            map.get(user.getId()).write(node);
        }
    }

    public static void notifyMsg(Long idUserFrom, String message, Long idChat){
        Chat chat = Chat.finder.where().eq("id", idChat).findUnique();
        chat.addMessage(message, User.getUser(idUserFrom));

        ObjectNode node = Json.newObject();
        node.put("type", "msg");
        node.put("message", Json.toJson(chat.getMessages().get(0)));

        map.get(chat.getStudent().getUser().getId()).write(node);
        map.get(chat.getTeacher().getUser().getId()).write(node);
    }

    public static void notifyDisconnection(Long userId){
        map.remove(userId);
        connections.remove(userId);

        Iterator<Long> relatedTo = getUsersRelated(userId).iterator();

        User me = User.getUser(userId);

        while (relatedTo.hasNext()){
            ObjectNode node = Json.newObject();
            node.put("type", "user");
            node.put("user", Json.toJson(me));
            node.put("connected", false);

            map.get(relatedTo.next()).write(node);
        }
    }

    private static Set<Long> getUsersRelated(Long userId){
        List<Long> users = new ArrayList<>();
        Iterator<Lesson> lessons;
        Teacher teacher = Teacher.finder.where().eq("user.id", userId).findUnique();
        if (teacher != null){
            lessons = Lesson.finder.where().eq("teacher", teacher).findList().iterator();
            while (lessons.hasNext()){
                users.add(lessons.next().getStudent().getUser().getId());
            }
        } else {
            Student student = Student.finder.where().eq("user.id", userId).findUnique();
            lessons = Lesson.finder.where().eq("student", student).findList().iterator();
            while (lessons.hasNext()){
                users.add(lessons.next().getTeacher().getUser().getId());
            }
        }

        return users.isEmpty() ? new HashSet<>() : new HashSet<>(users);
    }
}
