package com.gridpulse.core.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// azure: the angular build sits inside the jar (static/), so site + api share one address
// a page link like /login or /admin/dashboard is no file -> index.html, angular's router shows the page
// local run: there is no static/ folder, nothing changes
@Configuration
public class WebsiteConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new AngularPageResolver());
    }

    private static class AngularPageResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource file = super.getResource(resourcePath, location);
            // a real file, or an unknown /api/.. url (stays a 404)
            if (file != null || resourcePath.startsWith("api/")) {
                return file;
            }
            Resource index = location.createRelative("index.html");
            return index.isReadable() ? index : null;
        }
    }
}
