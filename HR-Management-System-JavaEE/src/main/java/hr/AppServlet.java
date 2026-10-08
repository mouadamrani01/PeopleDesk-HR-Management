package hr;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet({"/login","/logout","/dashboard","/employees","/absences","/messages","/schedule","/profile"})
public final class AppServlet extends HttpServlet {
    private final Repository repo=new Repository();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        try {
            String path=req.getServletPath();
            if(path.equals("/login")) { view(req,res,"login"); return; }
            if(path.equals("/logout")) { res.sendError(405); return; }
            int user=id(req); boolean admin=admin(req);
            req.setAttribute("me",repo.one("SELECT id,name,email,phone,position,role FROM employee WHERE id=?",user));
            switch(path) {
                case "/dashboard":
                    req.setAttribute("employeeCount",repo.one("SELECT COUNT(*) AS total FROM employee").get("total"));
                    req.setAttribute("absenceCount",repo.one("SELECT COUNT(*) AS total FROM absence WHERE employee_id=?",user).get("total"));
                    req.setAttribute("messageCount",repo.one("SELECT COUNT(*) AS total FROM message WHERE recipient_id=?",user).get("total"));
                    break;
                case "/employees":
                    if(!admin) { res.sendError(403); return; }
                    req.setAttribute("employees",repo.query("SELECT id,name,email,phone,salary,position,role FROM employee ORDER BY id"));
                    if(req.getParameter("edit")!=null) {
                        var employee=repo.one("SELECT id,name,email,phone,salary,position,role FROM employee WHERE id=?",positive(req,"edit"));
                        if(employee==null) { res.sendError(404); return; }
                        req.setAttribute("editing",employee);
                    }
                    break;
                case "/absences":
                    req.setAttribute("absences",repo.query("SELECT a.*,e.name FROM absence a JOIN employee e ON e.id=a.employee_id"+
                        (admin?"":" WHERE employee_id=?")+" ORDER BY start_date DESC",admin?new Object[]{}:new Object[]{user}));
                    req.setAttribute("employees",repo.query("SELECT id,name FROM employee ORDER BY name"));
                    break;
                case "/messages":
                    req.setAttribute("messages",repo.query("SELECT m.*,e.name AS sender FROM message m JOIN employee e ON e.id=m.sender_id WHERE recipient_id=? ORDER BY m.id DESC",user));
                    req.setAttribute("employees",repo.query("SELECT id,name FROM employee WHERE id<>? ORDER BY name",user));
                    if(req.getParameter("reply")!=null) {
                        var message=repo.one("SELECT sender_id,subject FROM message WHERE id=? AND recipient_id=?",positive(req,"reply"),user);
                        if(message==null) {res.sendError(404);return;}
                        req.setAttribute("reply",message);
                    }
                    break;
                case "/schedule":
                    LocalDate monday=monday(req);
                    req.setAttribute("week",monday);
                    req.setAttribute("slots",repo.query("SELECT s.*,e.name FROM schedule_slot s JOIN employee e ON e.id=s.employee_id WHERE work_date BETWEEN ? AND ?"+
                        (admin?"":" AND employee_id=?")+" ORDER BY work_date,e.name",admin?new Object[]{monday,monday.plusDays(4)}:new Object[]{monday,monday.plusDays(4),user}));
                    break;
                case "/profile": break;
                default: res.sendError(404);return;
            }
            view(req,res,path.substring(1));
        } catch(IllegalArgumentException ex) {res.sendError(400,ex.getMessage());}
        catch(SQLException ex) {failure(req,res,ex);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        try {
            String path=req.getServletPath();
            if(path.equals("/login")) {login(req,res);return;}
            if(path.equals("/logout")) {req.getSession().invalidate();redirect(req,res,"/login");return;}
            int user=id(req); boolean admin=admin(req);
            switch(path) {
                case "/employees":
                    if(!admin) {res.sendError(403);return;}
                    String action=text(req,"action",10);
                    if(action.equals("delete")) {
                        int target=positive(req,"id");
                        if(target==user) throw new IllegalArgumentException("You cannot delete your own account");
                        affected(repo.update("DELETE FROM employee WHERE id=?",target));
                    } else if(action.equals("save")) {
                        int target=Integer.parseInt(req.getParameter("id"));
                        if(target<0) throw new IllegalArgumentException("Invalid employee id");
                        String name=text(req,"name",100), email=email(req), phone=text(req,"phone",30), position=text(req,"position",100),role=text(req,"role",10);
                        if(!Set.of("ADMIN","EMPLOYEE").contains(role)) throw new IllegalArgumentException("Invalid role");
                        BigDecimal salary=new BigDecimal(text(req,"salary",20));
                        if(salary.signum()<0 || salary.compareTo(new BigDecimal("9999999999.99"))>0 || salary.scale()>2) throw new IllegalArgumentException("Invalid salary");
                        if(target==user && !role.equals("ADMIN")) throw new IllegalArgumentException("You cannot remove your own admin role");
                        String password=req.getParameter("password");
                        if(target==0) {
                            checkPassword(password);
                            repo.update("INSERT INTO employee(name,email,phone,salary,position,role,password_hash) VALUES(?,?,?,?,?,?,?)",name,email,phone,salary,position,role,Passwords.hash(password));
                        } else {
                            if(password!=null && !password.isBlank()) checkPassword(password);
                            affected(repo.update("UPDATE employee SET name=?,email=?,phone=?,salary=?,position=?,role=? WHERE id=?",name,email,phone,salary,position,role,target));
                            if(password!=null && !password.isBlank()) repo.update("UPDATE employee SET password_hash=? WHERE id=?",Passwords.hash(password),target);
                        }
                    } else throw new IllegalArgumentException("Unknown action");
                    break;
                case "/absences":
                    if(!admin) {res.sendError(403);return;}
                    if("delete".equals(req.getParameter("action"))) affected(repo.update("DELETE FROM absence WHERE id=?",positive(req,"id")));
                    else {
                        int days=positive(req,"days"); if(days>365) throw new IllegalArgumentException("Days must be between 1 and 365");
                        repo.update("INSERT INTO absence(employee_id,start_date,days,reason) VALUES(?,?,?,?)",positive(req,"employee"),LocalDate.parse(text(req,"date",10)),days,text(req,"reason",500));
                    }
                    break;
                case "/messages":
                    if("delete".equals(req.getParameter("action"))) affected(repo.update("DELETE FROM message WHERE id=? AND recipient_id=?",positive(req,"id"),user));
                    else repo.update("INSERT INTO message(sender_id,recipient_id,subject,body) VALUES(?,?,?,?)",user,positive(req,"recipient"),text(req,"subject",200),text(req,"body",4000));
                    break;
                case "/schedule":
                    if(!admin) {res.sendError(403);return;}
                    int offset=Integer.parseInt(text(req,"offset",9));
                    repo.generateWeek(monday(req),offset);
                    redirect(req,res,"/schedule?week="+monday(req));return;
                case "/profile":
                    String password=secret(req,"password"); checkPassword(password);
                    var account=repo.one("SELECT password_hash FROM employee WHERE id=?",user);
                    if(!Passwords.verify(secret(req,"currentPassword"),(String)account.get("password_hash"))) throw new IllegalArgumentException("Current password is incorrect");
                    repo.update("UPDATE employee SET password_hash=? WHERE id=?",Passwords.hash(password),user);
                    req.changeSessionId();
                    break;
                default:res.sendError(405);return;
            }
            req.getSession().setAttribute("notice","Changes saved successfully.");
            redirect(req,res,path);
        } catch(IllegalArgumentException|DateTimeException ex) {res.sendError(400,ex.getMessage());}
        catch(SQLException ex) {
            if(ex.getSQLState()!=null && ex.getSQLState().startsWith("23")) res.sendError(409,"Email already exists or referenced employee no longer exists");
            else failure(req,res,ex);
        }
    }
    private void login(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException,SQLException {
        HttpSession old=req.getSession();
        Integer attempts=(Integer)old.getAttribute("attempts");
        Long start=(Long)old.getAttribute("attemptStart");
        long now=System.currentTimeMillis();
        if(start==null || now-start>60000) {attempts=0;old.setAttribute("attemptStart",now);}
        if(attempts!=null && attempts>=10) {res.sendError(429,"Please wait one minute before trying again");return;}
        old.setAttribute("attempts",attempts==null?1:attempts+1);
        String email=text(req,"email",254),password=secret(req,"password");
        var account=repo.one("SELECT id,password_hash FROM employee WHERE email=?",email);
        if(account==null || !Passwords.verify(password,(String)account.get("password_hash"))) {
            req.setAttribute("error","Invalid email or password.");res.setStatus(401);view(req,res,"login");return;
        }
        old.invalidate(); HttpSession session=req.getSession(true);
        session.setAttribute("userId",((Number)account.get("id")).intValue());
        session.setAttribute("csrf",UUID.randomUUID().toString());
        session.setMaxInactiveInterval(1800);
        redirect(req,res,"/dashboard");
    }
    private boolean admin(HttpServletRequest req) throws SQLException {
        var row=repo.one("SELECT role FROM employee WHERE id=?",id(req));
        return row!=null && "ADMIN".equals(row.get("role"));
    }
    private static int id(HttpServletRequest req) {return (Integer)req.getSession().getAttribute("userId");}
    private static int positive(HttpServletRequest req,String key) {
        int value=Integer.parseInt(text(req,key,10));if(value<1) throw new IllegalArgumentException("Invalid "+key);return value;
    }
    private static String text(HttpServletRequest req,String key,int max) {
        String value=req.getParameter(key);
        if(value==null || value.isBlank() || value.length()>max) throw new IllegalArgumentException("Invalid "+key);
        return value.trim();
    }
    private static String secret(HttpServletRequest req,String key) {
        String value=req.getParameter(key);
        if(value==null || value.isBlank() || value.length()>200) throw new IllegalArgumentException("Invalid "+key);
        return value;
    }
    private static String email(HttpServletRequest req) {
        String value=text(req,"email",254);
        if(!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Invalid email");return value;
    }
    private static void checkPassword(String password) {
        if(password==null || password.length()<12 || password.length()>200) throw new IllegalArgumentException("Password must contain 12 to 200 characters");
    }
    private static LocalDate monday(HttpServletRequest req) {
        String date=req.getParameter("week");
        return (date==null?LocalDate.now():LocalDate.parse(date)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    private static void affected(int count) {if(count!=1) throw new IllegalArgumentException("Record not found or access denied");}
    private static void redirect(HttpServletRequest req,HttpServletResponse res,String path) throws IOException {res.sendRedirect(req.getContextPath()+path);}
    private static void view(HttpServletRequest req,HttpServletResponse res,String page) throws ServletException,IOException {
        if(!page.equals("login")) {
            req.setAttribute("notice",req.getSession().getAttribute("notice"));req.getSession().removeAttribute("notice");
        }
        req.getRequestDispatcher("/WEB-INF/views/"+page+".jsp").forward(req,res);
    }
    private void failure(HttpServletRequest req,HttpServletResponse res,SQLException ex) throws IOException {
        getServletContext().log("Database operation failed at "+req.getServletPath(),ex);res.sendError(500,"Database operation failed. See server logs.");
    }
}
