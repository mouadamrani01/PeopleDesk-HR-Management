package hr;

import java.nio.file.Files;
import org.apache.catalina.startup.Tomcat;

/** Local development launcher. Test-scoped Tomcat is not packaged in the WAR. */
public final class LocalServer {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat=new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("hr-tomcat-").toString());
        tomcat.setPort(Integer.parseInt(Database.setting("PORT","8080")));
        tomcat.getConnector();
        var context=tomcat.addWebapp("/hr-management",new java.io.File("src/main/webapp").getAbsolutePath());
        var resources = new org.apache.catalina.webresources.StandardRoot(context);
        resources.addPreResources(new org.apache.catalina.webresources.DirResourceSet(resources,"/WEB-INF/classes",new java.io.File("target/classes").getAbsolutePath(),"/"));
        context.setResources(resources);
        context.setParentClassLoader(LocalServer.class.getClassLoader());
        tomcat.start();
        System.out.println("HR Management: http://localhost:"+tomcat.getConnector().getLocalPort()+"/hr-management/");
        tomcat.getServer().await();
    }
}
