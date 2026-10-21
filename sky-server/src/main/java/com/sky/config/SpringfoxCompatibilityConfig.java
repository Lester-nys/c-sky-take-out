package com.sky.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 解决Spring Boot 2.6+与Springfox 3.0.0的兼容性问题
 */
@Configuration
public class SpringfoxCompatibilityConfig {

    @Bean
    public static BeanPostProcessor springfoxBeanPostProcessor() {

        return new BeanPostProcessor() {

            @Override
            @SuppressWarnings("unchecked")
            public Object postProcessAfterInitialization(
                    Object bean, String beanName) throws BeansException {

                // 判断当前Bean是否为Springfox的请求处理器
                if (bean instanceof WebMvcRequestHandlerProvider) {

                    // 通过反射获取handlerMappings字段
                    Field field = ReflectionUtils.findField(
                            bean.getClass(), "handlerMappings");

                    if (field != null) {

                        // 允许访问该字段
                        ReflectionUtils.makeAccessible(field);

                        // 获取Springfox管理的请求映射列表
                        List<RequestMappingInfoHandlerMapping> mappings =
                                (List<RequestMappingInfoHandlerMapping>)
                                        ReflectionUtils.getField(field, bean);

                        if (mappings != null) {

                            // 过滤掉采用PathPatternParser的映射
                            // 避免Springfox解析这些映射时发生空指针异常
                            mappings.removeIf(
                                    mapping -> mapping.getPatternParser() != null);
                        }
                    }
                }

                return bean;
            }
        };
    }
}
