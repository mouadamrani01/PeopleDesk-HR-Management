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
<a class="" href="${pageContext.request.contextPath}/schedule"><span class="nav-icon">▦</span>Planning</a>
<a class="active" href="${pageContext.request.contextPath}/profile"><span class="nav-icon">◎</span>Profile</a>
</nav>
<section class="sidebar-tip"><span class="tip-dot"></span><strong>A little more organized.</strong><p>One space for your team,<br>your time and your next step.</p></section>
<div class="sidebar-account"><span class="avatar">${me.role == 'ADMIN' ? 'AD' : 'EM'}</span><div><strong><c:out value="${me.name}"/></strong><small>${me.role == 'ADMIN' ? 'Administrator' : 'Employee'}</small></div></div>
<form method="post" action="${pageContext.request.contextPath}/logout"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><button class="quiet">↗ Sign out</button></form>
</header>
<div class="topbar"><span>Workspace <span class="breadcrumb-divider">/</span> <strong>People management</strong></span><span class="workspace-pill"><span class="status-dot"></span>${me.role == 'ADMIN' ? 'Admin workspace' : 'Employee workspace'}</span></div><main><c:if test="${not empty notice}"><p class="notice" role="status"><c:out value="${notice}"/></p></c:if>
<p class="eyebrow">ACCOUNT SETTINGS</p><h1>Your profile</h1><section class="card"><h2><c:out value="${me.name}"/></h2><p><c:out value="${me.email}"/> · <c:out value="${me.position}"/></p><p>Role: <c:out value="${me.role}"/></p></section><section class="card"><h2>Change password</h2><form class="grid" method="post" action="${pageContext.request.contextPath}/profile"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><label>Current password<input type="password" name="currentPassword" autocomplete="current-password" required maxlength="200"></label><label>New password<input type="password" name="password" autocomplete="new-password" required minlength="12" maxlength="200"></label><button>Update password</button></form></section></main><footer>PeopleDesk · A better day at work</footer></body></html>