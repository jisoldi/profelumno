package ua.dirproy.profelumno.register.models;

import com.avaje.ebean.Model;
import ua.dirproy.profelumno.user.models.User;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

/**
 * Created by javier
 * Date: 9/7/15
 * Project profelumno
 */

@Entity
public class Student extends Model {

    @Id private Long id;

    private String email;
    private String password;

    private static Finder<Long, User> finder = new Finder<>(User.class);

    public static List<User> list() { return finder.all(); }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
