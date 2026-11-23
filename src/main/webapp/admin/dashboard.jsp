
<%@ page import="java.util.Map,com.erp.model.User,com.erp.util.HtmlUtil" %>
<%
    if (request.getAttribute("dashboardMetrics") == null) {
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        return;
    }
    Map<String, Long> dashboardMetrics = (Map<String, Long>) request.getAttribute("dashboardMetrics");
    Map<String, String> appSettings = (Map<String, String>) request.getAttribute("appSettings");
    String dashboardSiteName = appSettings == null || appSettings.get("site_name") == null ? "EduERP" : appSettings.get("site_name");
    User user = (User) session.getAttribute("user");

    String adminName = (user != null && user.getName() != null)
            ? user.getName() : "Administrator";

    String initial = !adminName.isEmpty()
            ? adminName.substring(0, 1).toUpperCase() : "A";
    String adminDisplayName = adminName;
    String pageTitle = "Dashboard";
    request.setAttribute("activeAdminSection", "dashboard");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | <%= HtmlUtil.escape(dashboardSiteName) %></title>

    <!-- Tailwind CSS CDN (development) -->
    <script src="https://cdn.tailwindcss.com"></script>

    <!-- Font Awesome -->
    <link rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">

    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Inter', 'ui-sans-serif', 'system-ui']
                    }
                }
            }
        }
    </script>
</head>

<body class="bg-slate-100 font-sans text-slate-800">

<div class="min-h-screen lg:flex">
    <%@ include file="admin-nav.jspf" %>
    <main class="min-w-0 flex-1 lg:ml-60">
        <%@ include file="admin-header.jspf" %>
        <!-- Dashboard Content -->
        <section class="p-4 sm:p-6 lg:p-8">

            <!-- Welcome -->
            <div class="mb-8 flex flex-col justify-between gap-4 sm:flex-row
                        sm:items-center">

                <div>
                    <h2 class="text-2xl font-bold text-slate-900">
                        Welcome back, <%= HtmlUtil.escape(adminName) %>!
                    </h2>
                    <p class="mt-2 text-sm text-slate-500">
                        Here's an overview of your institution's
                        academic management system.
                    </p>
                </div>

                <div class="flex items-center gap-2 self-start rounded-xl
                            border border-slate-200 bg-white px-4 py-3
                            text-sm text-slate-600 shadow-sm">
                    <i class="fa-regular fa-calendar text-indigo-600"></i>
                    Academic Dashboard
                </div>

            </div>

            <% if (request.getAttribute("dashboardError") != null) { %><div class="mb-6 border-l-4 border-amber-500 bg-amber-50 px-4 py-3 text-sm text-amber-800"><%= HtmlUtil.escape(request.getAttribute("dashboardError")) %></div><% } %>

            <!-- Statistics Cards -->
            <div class="mb-10 grid grid-cols-1 gap-5 sm:grid-cols-2
                        xl:grid-cols-4">

                <!-- Students -->
                <div class="rounded-2xl border border-slate-200 bg-white
                            p-6 shadow-sm transition hover:-translate-y-1
                            hover:shadow-md">

                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-slate-500">
                                Total Students
                            </p>
                            <h3 class="mt-3 text-3xl font-bold text-slate-900">
                                <%= dashboardMetrics.getOrDefault("students", 0L) %>
                            </h3>
                        </div>

                        <div class="flex h-14 w-14 items-center justify-center
                                    rounded-xl bg-violet-100 text-xl text-violet-600">
                            <i class="fa-solid fa-user-graduate"></i>
                        </div>
                    </div>

                    <p class="mt-5 text-xs text-slate-400">
                        Student enrollment overview
                    </p>
                </div>

                <!-- Teachers -->
                <div class="rounded-2xl border border-slate-200 bg-white
                            p-6 shadow-sm transition hover:-translate-y-1
                            hover:shadow-md">

                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-slate-500">
                                Total Teachers
                            </p>
                            <h3 class="mt-3 text-3xl font-bold text-slate-900">
                                <%= dashboardMetrics.getOrDefault("teachers", 0L) %>
                            </h3>
                        </div>

                        <div class="flex h-14 w-14 items-center justify-center
                                    rounded-xl bg-blue-100 text-xl text-blue-600">
                            <i class="fa-solid fa-chalkboard-user"></i>
                        </div>
                    </div>

                    <p class="mt-5 text-xs text-slate-400">
                        Teaching staff overview
                    </p>
                </div>

                <!-- Classes -->
                <div class="rounded-2xl border border-slate-200 bg-white
                            p-6 shadow-sm transition hover:-translate-y-1
                            hover:shadow-md">

                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-slate-500">
                                Total Classes
                            </p>
                            <h3 class="mt-3 text-3xl font-bold text-slate-900">
                                <%= dashboardMetrics.getOrDefault("classes", 0L) %>
                            </h3>
                        </div>

                        <div class="flex h-14 w-14 items-center justify-center
                                    rounded-xl bg-emerald-100 text-xl text-emerald-600">
                            <i class="fa-solid fa-school"></i>
                        </div>
                    </div>

                    <p class="mt-5 text-xs text-slate-400">
                        Classes and sections
                    </p>
                </div>

                <!-- Subjects -->
                <div class="rounded-2xl border border-slate-200 bg-white
                            p-6 shadow-sm transition hover:-translate-y-1
                            hover:shadow-md">

                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-slate-500">
                                Total Subjects
                            </p>
                            <h3 class="mt-3 text-3xl font-bold text-slate-900">
                                <%= dashboardMetrics.getOrDefault("subjects", 0L) %>
                            </h3>
                        </div>

                        <div class="flex h-14 w-14 items-center justify-center
                                    rounded-xl bg-orange-100 text-xl text-orange-600">
                            <i class="fa-solid fa-book-open"></i>
                        </div>
                    </div>

                    <p class="mt-5 text-xs text-slate-400">
                        Academic subjects
                    </p>
                </div>

            </div>

            <!-- Management Modules -->
            <div class="mb-5 flex items-center justify-between">
                <div>
                    <h2 class="text-xl font-bold text-slate-900">
                        Management Modules
                    </h2>
                    <p class="mt-1 text-sm text-slate-500">
                        Access and manage institution records.
                    </p>
                </div>
            </div>

            <div class="mb-10 grid grid-cols-1 gap-5 sm:grid-cols-2
                        xl:grid-cols-4">

                <!-- Student Module -->
                <a href="students"
                   class="group rounded-2xl border border-slate-200
                          bg-white p-6 shadow-sm transition hover:-translate-y-1
                          hover:border-indigo-200 hover:shadow-lg">

                    <div class="mb-5 flex h-12 w-12 items-center justify-center
                                rounded-xl bg-violet-100 text-xl text-violet-600
                                transition group-hover:bg-violet-600
                                group-hover:text-white">
                        <i class="fa-solid fa-user-graduate"></i>
                    </div>

                    <h3 class="font-bold text-slate-800">Students</h3>

                    <p class="mt-2 text-sm leading-6 text-slate-500">
                        Add, view, update and manage student
                        information and enrollment records.
                    </p>

                    <p class="mt-5 text-sm font-semibold text-indigo-600">
                        Manage Students
                        <i class="fa-solid fa-arrow-right ml-1 transition
                                  group-hover:translate-x-1"></i>
                    </p>
                </a>

                <!-- Teacher Module -->
                <a href="teachers"
                   class="group rounded-2xl border border-slate-200
                          bg-white p-6 shadow-sm transition hover:-translate-y-1
                          hover:border-indigo-200 hover:shadow-lg">

                    <div class="mb-5 flex h-12 w-12 items-center justify-center
                                rounded-xl bg-blue-100 text-xl text-blue-600
                                transition group-hover:bg-blue-600
                                group-hover:text-white">
                        <i class="fa-solid fa-chalkboard-user"></i>
                    </div>

                    <h3 class="font-bold text-slate-800">Teachers</h3>

                    <p class="mt-2 text-sm leading-6 text-slate-500">
                        Maintain teaching staff profiles,
                        qualifications and assignments.
                    </p>

                    <p class="mt-5 text-sm font-semibold text-indigo-600">
                        Manage Teachers
                        <i class="fa-solid fa-arrow-right ml-1 transition
                                  group-hover:translate-x-1"></i>
                    </p>
                </a>

                <!-- Classes Module -->
                <a href="classes"
                   class="group rounded-2xl border border-slate-200
                          bg-white p-6 shadow-sm transition hover:-translate-y-1
                          hover:border-indigo-200 hover:shadow-lg">

                    <div class="mb-5 flex h-12 w-12 items-center justify-center
                                rounded-xl bg-emerald-100 text-xl text-emerald-600
                                transition group-hover:bg-emerald-600
                                group-hover:text-white">
                        <i class="fa-solid fa-school"></i>
                    </div>

                    <h3 class="font-bold text-slate-800">
                        Classes & Sections
                    </h3>

                    <p class="mt-2 text-sm leading-6 text-slate-500">
                        Configure academic classes, sections
                        and student class allocations.
                    </p>

                    <p class="mt-5 text-sm font-semibold text-indigo-600">
                        Manage Classes
                        <i class="fa-solid fa-arrow-right ml-1 transition
                                  group-hover:translate-x-1"></i>
                    </p>
                </a>

                <!-- Subjects Module -->
                <a href="subjects"
                   class="group rounded-2xl border border-slate-200
                          bg-white p-6 shadow-sm transition hover:-translate-y-1
                          hover:border-indigo-200 hover:shadow-lg">

                    <div class="mb-5 flex h-12 w-12 items-center justify-center
                                rounded-xl bg-orange-100 text-xl text-orange-600
                                transition group-hover:bg-orange-600
                                group-hover:text-white">
                        <i class="fa-solid fa-book-open"></i>
                    </div>

                    <h3 class="font-bold text-slate-800">Subjects</h3>

                    <p class="mt-2 text-sm leading-6 text-slate-500">
                        Create subjects and assign them to
                        classes and teaching staff.
                    </p>

                    <p class="mt-5 text-sm font-semibold text-indigo-600">
                        Manage Subjects
                        <i class="fa-solid fa-arrow-right ml-1 transition
                                  group-hover:translate-x-1"></i>
                    </p>
                </a>

            </div>

            <!-- Bottom Panels -->
            <div class="grid grid-cols-1 gap-6 xl:grid-cols-2">

                <!-- System Overview -->
                <div class="rounded-2xl border border-slate-200
                            bg-white p-6 shadow-sm">

                    <h3 class="mb-5 font-bold text-slate-900">
                        <i class="fa-solid fa-chart-line mr-2 text-indigo-600"></i>
                        System Overview
                    </h3>

                    <div class="space-y-5">

                        <div class="flex items-start gap-4 border-b
                                    border-slate-100 pb-5">
                            <div class="flex h-10 w-10 shrink-0 items-center
                                        justify-center rounded-lg bg-violet-100
                                        text-violet-600">
                                <i class="fa-solid fa-users"></i>
                            </div>
                            <div>
                                <h4 class="text-sm font-semibold text-slate-800">
                                    Student Management
                                </h4>
                                <p class="mt-1 text-sm text-slate-500">
                                    Manage student admission and enrollment.
                                </p>
                            </div>
                        </div>

                        <div class="flex items-start gap-4 border-b
                                    border-slate-100 pb-5">
                            <div class="flex h-10 w-10 shrink-0 items-center
                                        justify-center rounded-lg bg-blue-100
                                        text-blue-600">
                                <i class="fa-solid fa-book"></i>
                            </div>
                            <div>
                                <h4 class="text-sm font-semibold text-slate-800">
                                    Academic Management
                                </h4>
                                <p class="mt-1 text-sm text-slate-500">
                                    Organize classes, subjects and schedules.
                                </p>
                            </div>
                        </div>

                        <div class="flex items-start gap-4">
                            <div class="flex h-10 w-10 shrink-0 items-center
                                        justify-center rounded-lg bg-emerald-100
                                        text-emerald-600">
                                <i class="fa-solid fa-user-tie"></i>
                            </div>
                            <div>
                                <h4 class="text-sm font-semibold text-slate-800">
                                    Attendance & Reporting
                                </h4>
                                <p class="mt-1 text-sm text-slate-500">
                                    Review daily attendance and institution reports.
                                </p>
                            </div>
                        </div>

                    </div>

                    <div class="mt-2 grid grid-cols-2 gap-3 border-t border-slate-100 pt-5">
                        <div class="rounded-lg bg-slate-50 p-4"><p class="text-xs text-slate-500">Attendance recorded today</p><p class="mt-1 text-xl font-bold text-slate-900"><%= dashboardMetrics.getOrDefault("attendance_today", 0L) %></p></div>
                        <div class="rounded-lg bg-slate-50 p-4"><p class="text-xs text-slate-500">Upcoming exam dates</p><p class="mt-1 text-xl font-bold text-slate-900"><%= dashboardMetrics.getOrDefault("upcoming_exams", 0L) %></p></div>
                    </div>
                </div>

                <!-- Quick Actions -->
                <div class="rounded-2xl border border-slate-200
                            bg-white p-6 shadow-sm">

                    <h3 class="mb-5 font-bold text-slate-900">
                        <i class="fa-solid fa-bolt mr-2 text-indigo-600"></i>
                        Quick Actions
                    </h3>

                    <div class="grid grid-cols-2 gap-4">

                        <a href="students"
                           class="rounded-xl border border-slate-200
                                  bg-slate-50 p-5 transition hover:border-indigo-300
                                  hover:bg-indigo-50">
                            <i class="fa-solid fa-user-plus mb-3 text-xl
                                      text-indigo-600"></i>
                            <p class="text-sm font-semibold text-slate-700">
                                Add Student
                            </p>
                        </a>

                        <a href="teachers"
                           class="rounded-xl border border-slate-200
                                  bg-slate-50 p-5 transition hover:border-indigo-300
                                  hover:bg-indigo-50">
                            <i class="fa-solid fa-chalkboard-user mb-3 text-xl
                                      text-blue-600"></i>
                            <p class="text-sm font-semibold text-slate-700">
                                Add Teacher
                            </p>
                        </a>

                        <a href="classes"
                           class="rounded-xl border border-slate-200
                                  bg-slate-50 p-5 transition hover:border-indigo-300
                                  hover:bg-indigo-50">
                            <i class="fa-solid fa-school mb-3 text-xl
                                      text-emerald-600"></i>
                            <p class="text-sm font-semibold text-slate-700">
                                Create Class
                            </p>
                        </a>

                        <a href="subjects"
                           class="rounded-xl border border-slate-200
                                  bg-slate-50 p-5 transition hover:border-indigo-300
                                  hover:bg-indigo-50">
                            <i class="fa-solid fa-book-open mb-3 text-xl
                                      text-orange-600"></i>
                            <p class="text-sm font-semibold text-slate-700">
                                Add Subject
                            </p>
                        </a>

                        <a href="<%= request.getContextPath() %>/admin/reports" class="rounded-xl border border-slate-200 bg-slate-50 p-5 transition hover:border-indigo-300 hover:bg-indigo-50">
                            <i class="fa-solid fa-chart-pie mb-3 text-xl text-indigo-600"></i><p class="text-sm font-semibold text-slate-700">View Reports</p>
                        </a>

                        <a href="<%= request.getContextPath() %>/admin/attendance" class="rounded-xl border border-slate-200 bg-slate-50 p-5 transition hover:border-indigo-300 hover:bg-indigo-50">
                            <i class="fa-solid fa-calendar-check mb-3 text-xl text-emerald-600"></i><p class="text-sm font-semibold text-slate-700">Manage Attendance</p>
                        </a>

                    </div>
                </div>

            </div>

        </section>

        <!-- Footer -->
        <footer class="border-t border-slate-200 bg-white px-6 py-5
                       text-center text-xs text-slate-400">
            &copy; <%= java.time.Year.now().getValue() %> <%= HtmlUtil.escape(dashboardSiteName) %> Management System. All rights reserved.
        </footer>

    </main>

</div>



</body>
</html>
