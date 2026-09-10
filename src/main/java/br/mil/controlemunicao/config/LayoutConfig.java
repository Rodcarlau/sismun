package br.mil.controlemunicao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class LayoutConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response,
                                   Object handler, ModelAndView modelAndView) {
                if (modelAndView == null || modelAndView.getViewName() == null ||
                    modelAndView.getViewName().startsWith("redirect:") ||
                    modelAndView.getViewName().startsWith("forward:")) {
                    return;
                }

                String viewName = modelAndView.getViewName();
                if (!viewName.equals("login") && !viewName.equals("error") && !viewName.equals("layout/base")) {
                    modelAndView.addObject("contentTemplate", viewName);
                    modelAndView.addObject("currentPath", request.getRequestURI());
                    modelAndView.setViewName("layout/base");
                }
            }
        });
    }
}
