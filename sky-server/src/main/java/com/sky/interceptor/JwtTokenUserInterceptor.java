package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验JWT
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        // 判断当前拦截到的是Controller 方法 还是其他资源
        if(!(handler instanceof HandlerMethod))
            return true;   // 当前拦截到的不是动态方法（即不是Controller方法），直接放行

        // 1. 从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        // 2. 校验令牌
        try
        {
            log.info("jwt校验：{}",token);  // 打印当前请求携带额JWT token
            // 解析token 并校验签名 和 过期时间
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(),token);
            // 解析成功后获得JWT中的claims（载荷信息）

            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户的id：{}",userId);
            BaseContext.setCurrentId(userId);  // 在日志中输出当前用户的ID，并且保存在当前线程中
            log.info("JWT解析用户ID：{}", userId);
            log.info("BaseContext中的用户ID：{}", BaseContext.getCurrentId());
            // 3. 通过放行
            return true;
        }catch (Exception exception)
        {
            // 4. 不通过 响应HTTP响应状态码401
            response.setStatus(401);;
            return false;
        }

    }
}
