package com.itmuch.cloud;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName TestConfiguration
 * @Description 随机算法的负载均衡；注意：要是放在不和启动类一个目录，就不需要配置不扫描注解
 * @Author admin
 * @Date 2018/12/11 18:06
 **/
@Configuration
@ExcludeFromComponentScan
public class TestConfiguration {
    @Bean
    public IRule ribbonRule() {
        return new RandomRule();
    }
}
