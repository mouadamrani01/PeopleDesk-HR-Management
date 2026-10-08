package hr;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

@WebFilter("/*")
public final class SecurityFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req=(HttpServletRequest)request;
        HttpServletResponse res=(HttpServletResponse)response;
        req.setCharacterEncoding("UTF-8"); res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options","nosniff");
        res.setHeader("X-Frame-Options","DENY");
        res.setHeader("Referrer-Policy","same-origin");
        res.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        String path=req.getServletPath();
        if(path.startsWith("/assets/")) { chain.doFilter(req,res); return; }
        res.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession();
        if(session.getAttribute("csrf")==null) session.setAttribute("csrf",UUID.randomUUID().toString());
        if (!path.equals("/login") && session.getAttribute("userId")==null) {
            res.sendRedirect(req.getContextPath()+"/login"); return;
        }
        if (!path.equals("/login") && session.getAttribute("userId")!=null) {
            try {
                if(new Repository().one("SELECT id FROM employee WHERE id=?",session.getAttribute("userId"))==null) {
                    session.invalidate();res.sendRedirect(req.getContextPath()+"/login");return;
                }
            } catch(java.sql.SQLException ex) {throw new ServletException("Cannot verify current account",ex);}
        }
        if (req.getMethod().equals("POST") && !session.getAttribute("csrf").equals(req.getParameter("csrf"))) {
            res.sendError(403,"Invalid form token"); return;
        }
        chain.doFilter(req,res);
    }
}
