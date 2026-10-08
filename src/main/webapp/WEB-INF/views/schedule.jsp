<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>HR Management</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css"></head><body>
<header class="workspace-header">
<a class="brand" href="${pageContext.request.contextPath}/dashboard"><span class="brand-symbol">p.</span><span>PeopleDesk<small>YOUR PEOPLE WORKSPACE</small></span></a>
<p class="nav-label">WORKSPACE</p>
<nav aria-label="Main navigation">
<a class="" href="${pageContext.request.contextPath}/dashboard"><span class="nav-icon">◈</span>Overview</a>
<c:if test="${me.role == 'ADMIN'}"><a class="" href="${pageContext.request.contextPath}/employees"><span class="nav-icon">♧</span>Employees</a></c:if>
<a class="" href="${pageContext.request.contextPath}/absences"><span class="nav-icon">◷</span>Absences</a>
<a class="" href="${pageContext.request.contextPath}/messages"><span class="nav-icon">✉</span>Messages</a>
<a class="active" href="${pageContext.request.contextPath}/schedule"><span class="nav-icon">▦</span>Planning</a>
<a class="" href="${pageContext.request.contextPath}/profile"><span class="nav-icon">◎</span>Profile</a>
</nav>
<section class="sidebar-tip"><span class="tip-dot"></span><strong>A little more organized.</strong><p>One space for your team,<br>your time and your next step.</p></section>
<div class="sidebar-account"><span class="avatar">${me.role == 'ADMIN' ? 'AD' : 'EM'}</span><div><strong><c:out value="${me.name}"/></strong><small>${me.role == 'ADMIN' ? 'Administrator' : 'Employee'}</small></div></div>
<form method="post" action="${pageContext.request.contextPath}/logout"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><button class="quiet">↗ Sign out</button></form>
</header>
<div class="topbar"><span>Workspace <span class="breadcrumb-divider">/</span> <strong>People management</strong></span><span class="workspace-pill"><span class="status-dot"></span>${me.role == 'ADMIN' ? 'Admin workspace' : 'Employee workspace'}</span></div><main><c:if test="${not empty notice}"><p class="notice" role="status"><c:out value="${notice}"/></p></c:if>
<p class="eyebrow">WEEKLY ASSIGNMENTS</p><h1>Planning</h1><section class="card"><form class="grid" method="get" action="${pageContext.request.contextPath}/schedule"><label>Week containing<input type="date" name="week" value="${week}" required></label><div><button>View week</button></div></form><c:if test="${me.role == 'ADMIN'}"><hr><h2>Generate or replace this week</h2><p>Assigns five tasks in a daily rotation to employees, Monday to Friday. Generation replaces this week's existing assignments. It does not account for absences or capacity constraints.</p><form class="grid" method="post" action="${pageContext.request.contextPath}/schedule"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="week" value="${week}"><label>Rotation offset<input name="offset" type="number" value="0" min="-99999999" max="99999999" required></label><div><button>Replace week with rotation</button></div></form></c:if></section><section class="card table"><h2>Week of ${week}</h2><table><thead><tr><th>Date</th><th>Employee</th><th>Task</th></tr></thead><tbody><c:forEach var="s" items="${slots}"><tr><td><c:out value="${s.work_date}"/></td><td><c:out value="${s.name}"/></td><td><c:out value="${s.task}"/></td></tr></c:forEach></tbody></table><c:if test="${empty slots}"><p>No assignments for this week.</p></c:if></section></main><footer>PeopleDesk · A better day at work</footer></body></html>