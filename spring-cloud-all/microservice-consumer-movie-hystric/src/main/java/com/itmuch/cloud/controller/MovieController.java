package com.itmuch.cloud.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.itmuch.cloud.entity.User;

@RestController
public class MovieController {
  @Autowired
  private RestTemplate restTemplate;

  @GetMapping("/movie/{id}")
  //配置commandProperties是想让下面两个方法处在同一个线程上
  @HystrixCommand(fallbackMethod = "findByIdFallBack",commandProperties = {
          @HystrixProperty(name="execution.isolation.strategy", value="SEMAPHORE")
  })
  public User findById(@PathVariable Long id) {
    return this.restTemplate.getForObject("http://localhost:7900/simple/" + id, User.class);
  }

  public User findByIdFallBack(Long id) {
    User user = new User();
    user.setId(0L);
    return user;
  }
}
