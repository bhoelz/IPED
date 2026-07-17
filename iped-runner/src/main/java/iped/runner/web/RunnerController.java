package iped.runner.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import views.runner.page;

/**
 * Serves the IPED Runner configuration page.
 *
 * <p>In dev mode ({@code runner.dev=true}) the template loads React + Babel from CDN and each JS
 * file individually via {@code type="text/babel"} — fast reload, no build step. In prod mode the
 * template loads the single Vite-compiled island bundle from {@code /runner/dist/runner-island.js}.
 */
@Controller
public class RunnerController {

  @Value("${runner.dev:false}")
  private boolean dev;

  @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String runner() {
    return page.template(dev).render().toString();
  }
}
