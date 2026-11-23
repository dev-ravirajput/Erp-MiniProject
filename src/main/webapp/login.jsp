<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.erp.dao.UserDAO,com.erp.dao.AppSettingsDAO,com.erp.model.User,java.util.Optional,java.sql.SQLException,jakarta.servlet.http.HttpSession,java.util.Map" %>

<%
Object activeUserValue = session.getAttribute("user");
if (!"1".equals(request.getParameter("logout")) && activeUserValue instanceof User) {
    User activeUser = (User) activeUserValue;
    String rolePath = activeUser.getRole().name().toLowerCase();
    response.sendRedirect(request.getContextPath() + "/" + rolePath + (("student".equals(rolePath)||"teacher".equals(rolePath)||"parent".equals(rolePath)) ? "/dashboard" : "/dashboard.jsp"));
    return;
}
String loginError = null;
Map<String,String> branding = new java.util.HashMap<>(); branding.put("site_name","EduERP");
try { branding.putAll(new AppSettingsDAO().getSettings()); } catch (Exception | LinkageError settingsError) { application.log("Website branding settings could not be loaded; using defaults", settingsError); }
String siteName = branding.get("site_name");
if (siteName == null || siteName.isBlank()) siteName = "EduERP";
String logo = branding.get("logo_url"), favicon = branding.get("favicon_url");
if (logo != null && logo.startsWith("/")) logo = request.getContextPath() + logo;
if (favicon != null && favicon.startsWith("/")) favicon = request.getContextPath() + favicon;

if ("POST".equalsIgnoreCase(request.getMethod())) {
    String email = request.getParameter("email");
    String password = request.getParameter("password");

    if (email == null || email.isBlank() || password == null || password.isBlank()) {
        loginError = "Email and password are required.";
    } else {
        try {
            Optional<User> result = new UserDAO().authenticate(email, password);

            if (result.isPresent()) {
                HttpSession oldSession = request.getSession(false);

                if (oldSession != null) {
                    oldSession.invalidate();
                }

                HttpSession authenticatedSession = request.getSession(true);
                authenticatedSession.setMaxInactiveInterval(1800);

                User user = result.get();
                authenticatedSession.setAttribute("user", user);

                response.sendRedirect(
                    request.getContextPath()
                    + "/"
                    + user.getRole().name().toLowerCase()
                    + ((user.getRole() == com.erp.model.Role.STUDENT || user.getRole() == com.erp.model.Role.TEACHER || user.getRole() == com.erp.model.Role.PARENT) ? "/dashboard" : "/dashboard.jsp")
                );

                return;
            }

            loginError = "Invalid email, password, or inactive account.";

        } catch (SQLException | IllegalStateException exception) {
            getServletContext().log("Login database failure", exception);
            loginError = "The database is unavailable. Check the MySQL configuration.";
        }
    }
}

if ("1".equals(request.getParameter("logout"))) {
    session.invalidate();
}
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title><%=com.erp.util.HtmlUtil.escape(siteName)%> | Sign in</title>
    <% if(favicon!=null&&!favicon.isBlank()) { %><link rel="icon" href="<%=com.erp.util.HtmlUtil.escape(favicon)%>"><% } %>

    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>

    <!-- Font Awesome -->
    <link
        rel="stylesheet"
        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"
    >

    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: [
                            "Inter",
                            "ui-sans-serif",
                            "system-ui",
                            "-apple-system",
                            "BlinkMacSystemFont",
                            "Segoe UI",
                            "sans-serif"
                        ]
                    }
                }
            }
        }
    </script>
</head>

<body class="min-h-screen bg-slate-100 font-sans text-slate-900">

<div class="min-h-screen flex items-center justify-center p-4 sm:p-6 lg:p-8">

    <main
        class="
            w-full max-w-5xl
            overflow-hidden
            rounded-3xl
            bg-white
            shadow-2xl shadow-slate-900/10
            ring-1 ring-slate-200
        "
    >

        <div class="grid min-h-[640px] lg:grid-cols-2">

            <!-- =========================================================
                 BRAND PANEL
                 ========================================================= -->

            <section
                class="
                    relative
                    hidden
                    overflow-hidden
                    bg-gradient-to-br
                    from-slate-950
                    via-slate-900
                    to-indigo-950
                    p-10
                    text-white
                    lg:flex
                    lg:flex-col
                    lg:justify-between
                    xl:p-14
                "
            >

                <!-- Decorative background -->
                <div
                    class="
                        pointer-events-none
                        absolute
                        -right-24
                        -top-24
                        h-72
                        w-72
                        rounded-full
                        bg-indigo-500/20
                        blur-3xl
                    "
                ></div>

                <div
                    class="
                        pointer-events-none
                        absolute
                        -bottom-32
                        -left-24
                        h-72
                        w-72
                        rounded-full
                        bg-sky-500/10
                        blur-3xl
                    "
                ></div>

                <div class="relative z-10">

                    <!-- Logo -->
                    <div
                        class="
                            mb-10
                            flex
                            h-14
                            w-14
                            items-center
                            justify-center
                            rounded-2xl
                            border
                            border-white/10
                            bg-white/10
                            text-xl
                            font-bold
                            shadow-lg
                            shadow-black/10
                            backdrop-blur
                        "
                    >
                        <% if(logo!=null&&!logo.isBlank()) { %><img src="<%=com.erp.util.HtmlUtil.escape(logo)%>" alt="<%=com.erp.util.HtmlUtil.escape(siteName)%>" class="h-full w-full rounded-2xl object-contain"><% } else { %><%=com.erp.util.HtmlUtil.escape(siteName.substring(0,1).toUpperCase())%><% } %>
                    </div>

                    <!-- Label -->
                    <div
                        class="
                            mb-5
                            flex
                            items-center
                            gap-2
                            text-xs
                            font-bold
                            uppercase
                            tracking-[0.18em]
                            text-indigo-300
                        "
                    >
                        <span
                            class="
                                h-2
                                w-2
                                rounded-full
                                bg-indigo-400
                                shadow-[0_0_0_5px_rgba(129,140,248,0.12)]
                            "
                        ></span>

                        School Management
                    </div>

                    <!-- Heading -->
                    <h1
                        class="
                            max-w-lg
                            text-4xl
                            font-bold
                            leading-tight
                            tracking-tight
                            xl:text-5xl
                        "
                    >
                        Everything your school needs,
                        <span class="text-indigo-400">
                            in one place.
                        </span>
                    </h1>

                    <!-- Description -->
                    <p
                        class="
                            mt-6
                            max-w-md
                            text-sm
                            leading-7
                            text-slate-300
                        "
                    >
                        Manage students, teachers, academics, attendance
                        and administration from a single ERP workspace.
                    </p>

                    <!-- Feature list -->
                    <div class="mt-10 space-y-4">

                        <div class="flex items-center gap-3 text-sm text-slate-300">
                            <span
                                class="
                                    flex
                                    h-9
                                    w-9
                                    items-center
                                    justify-center
                                    rounded-xl
                                    bg-white/10
                                    text-indigo-300
                                "
                            >
                                <i class="fa-solid fa-users"></i>
                            </span>

                            Student & teacher management
                        </div>

                        <div class="flex items-center gap-3 text-sm text-slate-300">
                            <span
                                class="
                                    flex
                                    h-9
                                    w-9
                                    items-center
                                    justify-center
                                    rounded-xl
                                    bg-white/10
                                    text-indigo-300
                                "
                            >
                                <i class="fa-solid fa-chart-line"></i>
                            </span>

                            Academic & attendance tracking
                        </div>

                        <div class="flex items-center gap-3 text-sm text-slate-300">
                            <span
                                class="
                                    flex
                                    h-9
                                    w-9
                                    items-center
                                    justify-center
                                    rounded-xl
                                    bg-white/10
                                    text-indigo-300
                                "
                            >
                                <i class="fa-solid fa-shield-halved"></i>
                            </span>

                            Secure administrative workspace
                        </div>

                    </div>
                </div>

                <!-- Footer -->
                <div
                    class="
                        relative
                        z-10
                        flex
                        items-center
                        gap-2
                        text-xs
                        text-slate-500
                    "
                >
                    <i class="fa-solid fa-lock text-indigo-400"></i>

                    ERP System

                    <span class="text-slate-700">•</span>

                    Secure workspace
                </div>

            </section>


            <!-- =========================================================
                 LOGIN PANEL
                 ========================================================= -->

            <section
                class="
                    flex
                    min-h-[640px]
                    items-center
                    bg-white
                    px-6
                    py-10
                    sm:px-10
                    lg:px-12
                    xl:px-16
                "
            >

                <div class="mx-auto w-full max-w-md">

                    <!-- Mobile logo -->
                    <div
                        class="
                            mb-8
                            flex
                            h-12
                            w-12
                            items-center
                            justify-center
                            rounded-2xl
                            bg-indigo-50
                            text-lg
                            font-bold
                            text-indigo-600
                            lg:hidden
                        "
                    >
                        E
                    </div>


                    <!-- Heading -->
                    <div class="mb-8">

                        <div
                            class="
                                mb-3
                                text-xs
                                font-bold
                                uppercase
                                tracking-[0.18em]
                                text-indigo-600
                            "
                        >
                            Welcome back
                        </div>

                        <h2
                            class="
                                text-3xl
                                font-bold
                                tracking-tight
                                text-slate-900
                            "
                        >
                            Sign in to ERP
                        </h2>

                        <p
                            class="
                                mt-2
                                text-sm
                                leading-6
                                text-slate-500
                            "
                        >
                            Enter your credentials to access your workspace.
                        </p>

                    </div>


                    <!-- =================================================
                         ERROR MESSAGE
                         ================================================= -->

                    <% if (loginError != null) { %>

                        <div
                            class="
                                mb-6
                                flex
                                items-start
                                gap-3
                                rounded-xl
                                border
                                border-red-200
                                bg-red-50
                                p-4
                                text-sm
                                text-red-700
                            "
                            role="alert"
                        >

                            <div
                                class="
                                    flex
                                    h-6
                                    w-6
                                    shrink-0
                                    items-center
                                    justify-center
                                    rounded-full
                                    bg-red-100
                                    text-xs
                                    text-red-600
                                "
                            >
                                <i class="fa-solid fa-exclamation"></i>
                            </div>

                            <div class="pt-0.5">
                                <%= loginError %>
                            </div>

                        </div>

                    <% } %>


                    <!-- =================================================
                         LOGIN FORM
                         ================================================= -->

                    <form
                        action="<%= request.getContextPath() %>/login.jsp"
                        method="post"
                        autocomplete="on"
                        class="space-y-5"
                    >

                        <!-- Email -->
                        <div>

                            <label
                                for="email"
                                class="
                                    mb-2
                                    block
                                    text-sm
                                    font-semibold
                                    text-slate-700
                                "
                            >
                                Email
                            </label>

                            <div class="relative">

                                <div
                                    class="
                                        pointer-events-none
                                        absolute
                                        inset-y-0
                                        left-0
                                        flex
                                        w-11
                                        items-center
                                        justify-center
                                        text-slate-400
                                    "
                                >
                                    <i class="fa-regular fa-envelope"></i>
                                </div>

                                <input
                                    id="email"
                                    type="email"
                                    name="email"
                                    placeholder="you@example.com"
                                    autocomplete="username"
                                    required
                                    class="
                                        h-12
                                        w-full
                                        rounded-xl
                                        border
                                        border-slate-200
                                        bg-slate-50
                                        pl-11
                                        pr-4
                                        text-sm
                                        text-slate-900
                                        outline-none
                                        transition
                                        placeholder:text-slate-400
                                        hover:border-slate-300
                                        focus:border-indigo-500
                                        focus:bg-white
                                        focus:ring-4
                                        focus:ring-indigo-500/10
                                    "
                                >

                            </div>

                        </div>


                        <!-- Password -->
                        <div>

                            <label
                                for="password"
                                class="
                                    mb-2
                                    block
                                    text-sm
                                    font-semibold
                                    text-slate-700
                                "
                            >
                                Password
                            </label>

                            <div class="relative">

                                <!-- Lock icon -->
                                <div
                                    class="
                                        pointer-events-none
                                        absolute
                                        inset-y-0
                                        left-0
                                        flex
                                        w-11
                                        items-center
                                        justify-center
                                        text-slate-400
                                    "
                                >
                                    <i class="fa-solid fa-lock"></i>
                                </div>

                                <input
                                    id="password"
                                    type="password"
                                    name="password"
                                    placeholder="Enter your password"
                                    autocomplete="current-password"
                                    required
                                    class="
                                        h-12
                                        w-full
                                        rounded-xl
                                        border
                                        border-slate-200
                                        bg-slate-50
                                        pl-11
                                        pr-12
                                        text-sm
                                        text-slate-900
                                        outline-none
                                        transition
                                        placeholder:text-slate-400
                                        hover:border-slate-300
                                        focus:border-indigo-500
                                        focus:bg-white
                                        focus:ring-4
                                        focus:ring-indigo-500/10
                                    "
                                >

                                <!-- Password toggle -->
                                <button
                                    id="passwordToggle"
                                    class="
                                        absolute
                                        right-1.5
                                        top-1/2
                                        flex
                                        h-9
                                        w-9
                                        -translate-y-1/2
                                        items-center
                                        justify-center
                                        rounded-lg
                                        text-slate-400
                                        transition
                                        hover:bg-slate-100
                                        hover:text-slate-700
                                        focus:outline-none
                                        focus:ring-2
                                        focus:ring-indigo-500/30
                                    "
                                    type="button"
                                    aria-label="Show password"
                                    aria-pressed="false"
                                >
                                    <i
                                        id="passwordIcon"
                                        class="fa-regular fa-eye"
                                    ></i>
                                </button>

                            </div>

                        </div>


                        <!-- Submit -->
                        <button
                            type="submit"
                            class="
                                group
                                mt-2
                                flex
                                h-12
                                w-full
                                items-center
                                justify-center
                                gap-2
                                rounded-xl
                                bg-indigo-600
                                text-sm
                                font-semibold
                                text-white
                                shadow-lg
                                shadow-indigo-600/20
                                transition
                                hover:bg-indigo-700
                                hover:shadow-indigo-600/30
                                focus:outline-none
                                focus:ring-4
                                focus:ring-indigo-500/20
                                active:scale-[0.99]
                            "
                        >
                            Sign in

                            <i
                                class="
                                    fa-solid
                                    fa-arrow-right
                                    text-xs
                                    transition-transform
                                    group-hover:translate-x-0.5
                                "
                            ></i>
                        </button>

                    </form>


                    <!-- Footer -->
                    <div
                        class="
                            mt-8
                            flex
                            items-center
                            justify-center
                            gap-2
                            text-xs
                            text-slate-400
                        "
                    >
                        <i class="fa-solid fa-shield-halved"></i>

                        <span>
                            Secure ERP workspace
                        </span>

                        <span>•</span>

                        <span>
                            © ERP System
                        </span>
                    </div>

                </div>

            </section>

        </div>

    </main>

</div>


<!-- =============================================================
     PASSWORD TOGGLE
     ============================================================= -->

<script>
    const passwordInput = document.getElementById("password");
    const passwordToggle = document.getElementById("passwordToggle");
    const passwordIcon = document.getElementById("passwordIcon");

    passwordToggle.addEventListener("click", function () {

        const isPassword = passwordInput.type === "password";

        passwordInput.type = isPassword ? "text" : "password";

        passwordToggle.setAttribute(
            "aria-label",
            isPassword ? "Hide password" : "Show password"
        );

        passwordToggle.setAttribute(
            "aria-pressed",
            String(isPassword)
        );

        passwordIcon.className = isPassword
            ? "fa-regular fa-eye-slash"
            : "fa-regular fa-eye";
    });
</script>

</body>
</html>
