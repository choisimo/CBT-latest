/*

package com.career_block.auth.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class errorController implements ErrorController {

    @RequestMapping("/errorPage")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("javax.servlet.error.status_code");

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("statusCode", statusCode);

            if (statusCode == 404) {
                return "notExist";  // src/main/resources/templates/notExist.html 템플릿 반환
            } else if (statusCode == 401) {
                return "unauthorized"; // src/main/resources/templates/unauthorized.html 템플릿 반환
            }
        }

        return "error"; // 다른 오류에는 기본 에러 페이지 반환 (src/main/resources/templates/error.html)

}
*/

