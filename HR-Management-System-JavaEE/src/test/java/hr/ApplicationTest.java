package hr;

import org.junit.jupiter.api.*;
import org.apache.catalina.startup.Tomcat;
import java.net.*;
import java.net.http.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTest {
    static Tomcat server;
    static String base;
    static HttpClient admin,employee;
    static String adminToken,employeeToken;
    static Repository repo=new Repository();
    @BeforeAll static void start() throws Exception {
        System.setProperty("DB_URL","jdbc:h2:mem:hrtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("APP_DEMO","true");
        server=new Tomcat();server.setBaseDir(Files.createTempDirectory("hr-test-").toString());server.setPort(0);server.getConnector();
        var context=server.addWebapp("/hr",new java.io.File("src/main/webapp").getAbsolutePath());
        var resources = new org.apache.catalina.webresources.StandardRoot(context);
        resources.addPreResources(new org.apache.catalina.webresources.DirResourceSet(resources,"/WEB-INF/classes",new java.io.File("target/classes").getAbsolutePath(),"/"));
        context.setResources(resources);
        context.setParentClassLoader(ApplicationTest.class.getClassLoader());server.start();
        base="http://localhost:"+server.getConnector().getLocalPort()+"/hr";
        admin=client();employee=client();
        adminToken=signIn(admin,"admin@example.test","DemoAdmin!2026");
        employeeToken=signIn(employee,"employee@example.test","DemoEmployee!2026");
    }
    @AfterAll static void stop() throws Exception {if(server!=null){server.stop();server.destroy();}}
    static HttpClient client() {return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    static HttpResponse<String> get(HttpClient client,String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    static HttpResponse<String> post(HttpClient client,String path,Map<String,String> data) throws Exception {
        String body=data.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    static String token(String html) {
        var matcher=Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(html);assertTrue(matcher.find(),html);return matcher.group(1);
    }
    static String signIn(HttpClient client,String email,String password) throws Exception {
        var login=get(client,"/login");assertEquals(200,login.statusCode());
        assertEquals(302,post(client,"/login",Map.of("csrf",token(login.body()),"email",email,"password",password)).statusCode());
        var dashboard=get(client,"/dashboard");assertEquals(200,dashboard.statusCode(),dashboard.body());return token(dashboard.body());
    }
    @Test @Order(1) void authenticationAndAccess() throws Exception {
        var anon=client();assertEquals(302,get(anon,"/employees").statusCode());
        var login=get(anon,"/login");assertEquals(401,post(anon,"/login",Map.of("csrf",token(login.body()),"email","admin@example.test","password","incorrect-password")).statusCode());
        assertEquals(403,get(employee,"/employees").statusCode());
        assertEquals(403,post(admin,"/absences",Map.of("employee","2")).statusCode());
        assertEquals(405,get(admin,"/logout").statusCode());
        assertEquals(404,get(admin,"/WEB-INF/views/employees.jsp").statusCode());
        assertEquals(200,get(admin,"/assets/app.css").statusCode());
    }
    @Test @Order(2) void employeeCrudAndValidation() throws Exception {
        Map<String,String> form=new HashMap<>(Map.ofEntries(Map.entry("csrf",adminToken),Map.entry("action","save"),Map.entry("id","0"),Map.entry("name","<script>alert(1)</script>"),Map.entry("email","new@example.test"),Map.entry("phone","0600000000"),Map.entry("salary","3200.50"),Map.entry("position","Developer"),Map.entry("role","EMPLOYEE"),Map.entry("password","NewEmployee!2026")));
        assertEquals(302,post(admin,"/employees",form).statusCode());
        var added=repo.one("SELECT * FROM employee WHERE email=?","new@example.test");assertNotNull(added);
        assertTrue(Passwords.verify("NewEmployee!2026",(String)added.get("password_hash")));
        var listing=get(admin,"/employees");assertEquals(200,listing.statusCode());assertTrue(listing.body().contains("&lt;script&gt;"));assertFalse(listing.body().contains("<script>alert"));
        assertEquals(409,post(admin,"/employees",form).statusCode());
        form.put("id",added.get("id").toString());form.put("name","Updated Employee");form.put("password","");
        assertEquals(302,post(admin,"/employees",form).statusCode());
        assertEquals("Updated Employee",repo.one("SELECT name FROM employee WHERE id=?",added.get("id")).get("name"));
        form.put("salary","-1");assertEquals(400,post(admin,"/employees",form).statusCode());
        assertEquals(302,post(admin,"/employees",Map.of("csrf",adminToken,"action","delete","id",added.get("id").toString())).statusCode());
        assertNull(repo.one("SELECT id FROM employee WHERE id=?",added.get("id")));
        assertEquals(400,post(admin,"/employees",Map.of("csrf",adminToken,"action","delete","id","1")).statusCode());
    }
    @Test @Order(3) void absenceOwnershipAndValidation() throws Exception {
        assertEquals(302,post(admin,"/absences",Map.of("csrf",adminToken,"employee","2","date","2026-10-12","days","2","reason","Annual leave")).statusCode());
        assertEquals(302,post(admin,"/absences",Map.of("csrf",adminToken,"employee","1","date","2026-10-13","days","1","reason","ADMIN_ONLY_REASON")).statusCode());
        var view=get(employee,"/absences");assertEquals(200,view.statusCode());assertTrue(view.body().contains("Annual leave"));assertFalse(view.body().contains("ADMIN_ONLY_REASON"));
        assertEquals(403,post(employee,"/absences",Map.of("csrf",employeeToken,"employee","2","date","2026-10-12","days","2","reason","Forbidden")).statusCode());
        assertEquals(400,post(admin,"/absences",Map.of("csrf",adminToken,"employee","2","date","bad-date","days","2","reason","Invalid")).statusCode());
        var row=repo.one("SELECT id FROM absence WHERE employee_id=2");
        assertEquals(302,post(admin,"/absences",Map.of("csrf",adminToken,"action","delete","id",row.get("id").toString())).statusCode());
    }
    @Test @Order(4) void distinctMessagesRepliesAndOwnership() throws Exception {
        for(String subject:List.of("First subject","Second subject")) assertEquals(302,post(admin,"/messages",Map.of("csrf",adminToken,"recipient","2","subject",subject,"body","Message body")).statusCode());
        var inbox=get(employee,"/messages");assertEquals(200,inbox.statusCode());assertTrue(inbox.body().contains("First subject"));assertTrue(inbox.body().contains("Second subject"));
        var first=repo.one("SELECT id FROM message WHERE subject='First subject'");String messageId=first.get("id").toString();
        assertEquals(404,get(admin,"/messages?reply="+messageId).statusCode());
        assertEquals(200,get(employee,"/messages?reply="+messageId).statusCode());
        assertEquals(400,post(admin,"/messages",Map.of("csrf",adminToken,"action","delete","id",messageId)).statusCode());
        assertEquals(302,post(employee,"/messages",Map.of("csrf",employeeToken,"recipient","1","subject","Reply subject","body","A reply")).statusCode());
        assertTrue(get(admin,"/messages").body().contains("Reply subject"));
        assertEquals(302,post(employee,"/messages",Map.of("csrf",employeeToken,"action","delete","id",messageId)).statusCode());
    }
    @Test @Order(5) void weeklyRotationIsAtomicScopedAndDeterministic() throws Exception {
        var form=Map.of("csrf",adminToken,"week","2026-10-14","offset","0");
        assertEquals(302,post(admin,"/schedule",form).statusCode());
        assertEquals(5,repo.query("SELECT * FROM schedule_slot").size());
        String before=repo.one("SELECT task FROM schedule_slot WHERE work_date='2026-10-12'").get("task").toString();
        assertEquals(302,post(admin,"/schedule",Map.of("csrf",adminToken,"week","2026-10-12","offset","1")).statusCode());
        assertEquals(5,repo.query("SELECT * FROM schedule_slot").size());
        assertNotEquals(before,repo.one("SELECT task FROM schedule_slot WHERE work_date='2026-10-12'").get("task"));
        assertEquals(302,post(admin,"/schedule",Map.of("csrf",adminToken,"week","2026-10-19","offset","0")).statusCode());
        assertEquals(10,repo.query("SELECT * FROM schedule_slot").size());
        assertEquals(200,get(employee,"/schedule?week=2026-10-12").statusCode());
        assertEquals(403,post(employee,"/schedule",Map.of("csrf",employeeToken,"week","2026-10-12","offset","0")).statusCode());
    }
    @Test @Order(6) void profilePasswordAndLogout() throws Exception {
        assertEquals(200,get(employee,"/profile").statusCode());
        assertEquals(400,post(employee,"/profile",Map.of("csrf",employeeToken,"currentPassword","incorrect-old","password","ChangedPassword!2026")).statusCode());
        assertEquals(302,post(employee,"/profile",Map.of("csrf",employeeToken,"currentPassword","DemoEmployee!2026","password","ChangedPassword!2026")).statusCode());
        assertEquals(302,post(employee,"/logout",Map.of("csrf",employeeToken)).statusCode());
        assertEquals(302,get(employee,"/messages").statusCode());
        employeeToken=signIn(employee,"employee@example.test","ChangedPassword!2026");
    }
    @Test @Order(7) void passwordHashing() {
        String a=Passwords.hash("SamePassword!2026"),b=Passwords.hash("SamePassword!2026");
        assertNotEquals(a,b);assertTrue(Passwords.verify("SamePassword!2026",a));assertFalse(Passwords.verify("wrong",a));assertFalse(Passwords.verify("wrong","malformed"));
    }
}
