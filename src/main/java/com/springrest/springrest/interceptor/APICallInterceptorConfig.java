package com.springrest.springrest.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Component
public class APICallInterceptorConfig extends WebMvcConfigurationSupport {
	
	@Autowired
	APICallInterceptor apiCallIntercepter;

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiCallIntercepter).addPathPatterns("/courses/**");
	}
}
