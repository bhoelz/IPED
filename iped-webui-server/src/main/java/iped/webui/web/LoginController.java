package iped.webui.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves the login page. Spring Security handles the POST to {@code /login}
 * itself via {@code UsernamePasswordAuthenticationFilter}; this controller
 * only needs to render the form on GET.
 */
@Controller
public class LoginController {

    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout) {
        return views.login.page.template(error != null, logout != null).render().toString();
    }
}
