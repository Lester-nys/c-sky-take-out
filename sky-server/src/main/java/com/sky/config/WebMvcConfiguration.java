package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login")
                .excludePathPatterns("/admin/common/image/**");
    }

    /**
     * 通过knife4j生成接口文档
     */
    @Bean
    public Docket docket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters)
    {
        log.info("扩展消息转换器");
        // 创建消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 为消息转换器设置对象转换器 ， 对象转换器将java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        // 将自己的消息转换器加入容器中
        converters.add(0,converter);
    }



    /**
     * 注入用户端 JWT 拦截器
     *
     * @Autowired：
     * Spring 会从 IoC 容器中找到 JwtTokenUserInterceptor 对象，
     * 并将其注入到当前配置类中。
     *
     * 前提：
     * JwtTokenUserInterceptor 已经交给 Spring 管理，
     * 例如在类上添加 @Component 注解。
     */
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;
    /**
     * 注册自定义拦截器
     *
     * addInterceptors：
     * Spring MVC 提供的拦截器注册方法。
     *
     * registry：
     * 拦截器注册器，负责添加拦截器和配置拦截规则。
     */
    protected void addInterceptor(InterceptorRegistry registry)
    {
        log.info("开始注册自定义拦截器...");
        // ① 将我们编写的 JWT 拦截器注册到 Spring MVC 中
        registry.addInterceptor(jwtTokenUserInterceptor)
                // ② 拦截所有以 /user/ 开头的请求路径
                // ** 表示匹配零层或多层路径
                .addPathPatterns("/user/**")
                // ③ 排除用户登录接口，不进行 JWT 校验
                // 因为用户登录之前还没有 JWT Token
                .excludePathPatterns("/user/user/login")
                // ④ 排除获取店铺营业状态接口
                // 即使没有登录，也允许访问店铺营业状态
                .excludePathPatterns("/user/shop/status");
    }
}
