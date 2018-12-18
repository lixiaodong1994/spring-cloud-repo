#spring cloud笔记
##zuul
###zuul的使用  
1.引入依赖

	<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-starter-zuul</artifactId>
			</dependency>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-starter-eureka</artifactId>
			</dependency>
	</dependencies>  
2.在启动类上加上注释  

	@SpringBootApplication
	@EnableZuulProxy
	public class ZuulApplication {
	  public static void main(String[] args) {
	    SpringApplication.run(ZuulApplication.class, args);
	  }
	}  
> 在application.yml文件中配置可以禁用反向代理    
> 
	zuul:
	  ignoredServices: '*'      #禁用掉所有的反向代理
	  routes:                   #配置的这个可以进行反向代理
	    microservice-provider-user: /user/**  
 
3.启动服务    
###注意事项

> zuul指定path+serviceId,在application.yml文件中配置    
>   
	 zuul:
	  routes:
	    abc:
	      path: /user-path/**
	      serviceId: microservice-provider-user 

  
> zuul指定path+url以及指定可用的服务点时如何进行负载均衡  
>
	zuul:
	  routes:
	    abc:
	      path: /user-url/**
	      url: http://192.168.85.1:7900/ 
>zuul配置解决负载均衡 
>   
	zuul:
	  routes:
	    abc:
	      path: /user-url/**
	      service-id: microservice-provider-user
	ribbon:
	  eureka:
	    enabled: false
	microservice-provider-user:     # 这边是ribbon要请求的微服务的serviceId
	  ribbon:
	    listOfServers: http://localhost:7900,http://localhost:7901
>zuul使用正则表达式指定路由规则
>在启动类中配置注解类，例如：microservice-provider-user-v1 配置正则表达式后会变成 v1/microservice-provider-user
> 
	 	@Bean
		  public PatternServiceRouteMapper serviceRouteMapper() {
		    return new PatternServiceRouteMapper("(?<name>^.+)-(?<version>v.+$)", "${version}/${name}");
		  }


###zuul的回退
   
新创建一个类，实现zuul的回退类  

	@Component
	public class MyFallbackProvider implements ZuulFallbackProvider {
	  @Override
	  public String getRoute() {
	    return "microservice-provider-user";
	  }
	
	  @Override
	  public ClientHttpResponse fallbackResponse() {
	    return new ClientHttpResponse() {
	      @Override
	      public HttpStatus getStatusCode() throws IOException {
	        return HttpStatus.BAD_REQUEST;
	      }
	
	      @Override
	      public int getRawStatusCode() throws IOException {
	        return HttpStatus.BAD_REQUEST.value();
	      }
	
	      @Override
	      public String getStatusText() throws IOException {
	        return HttpStatus.BAD_REQUEST.getReasonPhrase();
	      }
	
	      @Override
	      public void close() {
	      }
	
	      @Override
	      public InputStream getBody() throws IOException {
	        return new ByteArrayInputStream(("fallback" + " " + MyFallbackProvider.this.getRoute()).getBytes());
	      }
	
	      @Override
	      public HttpHeaders getHeaders() {
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        return headers;
	      }
	    };
	  }
	}

###sidecar
1.导入依赖  

		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-netflix-sidecar</artifactId>
			</dependency>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-starter-eureka</artifactId>
			</dependency>
		</dependencies>
2.在启动类上加上@EnableSidecar注解  
3.在application.yml文件中配置sidecar  
  
	sidecar:
	  port: 8060
	  health-uri: http://localhost:8060/health.json  
4.node-service.json文件，在控制台使用node node-service.json启动
  
	var http = require('http');
	var url = require("url");
	var path = require('path');
	
	// 创建server
	var server = http.createServer(function(req, res) {
	  // 获得请求的路径
	  var pathname = url.parse(req.url).pathname;  
	  res.writeHead(200, { 'Content-Type' : 'application/json; charset=utf-8' });
	  // 访问http://localhost:8060/，将会返回{"index":"欢迎来到首页"}
	  if (pathname === '/') {
	    res.end(JSON.stringify({ "index" : "欢迎来到首页" }));
	  }
	  // 访问http://localhost:8060/health，将会返回{"status":"UP"}
	  else if (pathname === '/health.json') {
	    res.end(JSON.stringify({ "status" : "UP" }));
	  }
	  // 其他情况返回404
	  else {
	    res.end("404");
	  }
	});
	// 创建监听，并打印日志
	server.listen(8060, function() {
	  console.log('listening on localhost:8060');
	});
  
5.在其中一个服务的controller中添加一个方法  
  
	 @GetMapping("/sidecar")
	  public String find() {
	    return this.restTemplate.getForObject("http://microservice-sidecar/", String.class);
	  }  
  
6.在浏览器中访问sidecar的请求的时候，也是可以访问node-service.json中的接口的。

###zuulFilter  
  1.写一个类，继承zuulFilter  

	public class PreZuulFilter extends ZuulFilter {
	  private static final Logger LOGGER = LoggerFactory.getLogger(PreZuulFilter.class);
	
	  @Override
	  public boolean shouldFilter() {
	    return true;
	  }
	
	  @Override
	  public Object run() {
	    HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
	    String host = request.getRemoteHost();
	    PreZuulFilter.LOGGER.info("请求的host:{}", host);
	    return null;
	  }
	
	  @Override
	  public String filterType() {
	    return "pre";
	  }
	
	  @Override
	  public int filterOrder() {
	    return 1;
	  }
	
	}  
  
2.将过滤器注入进去  
	  
	  /**
	   * 将zuulFilter注入
	   * @return
	   */
	  @Bean
	  public PreZuulFilter preZuulFilter() {
	    return new PreZuulFilter();
	  }

##文件上传
###文件上传使用
1.导入依赖
	<!-- 引入spring boot的依赖 -->
	  <parent>
	    <groupId>org.springframework.boot</groupId>
	    <artifactId>spring-boot-starter-parent</artifactId>
	    <version>1.4.2.RELEASE</version>
	  </parent>
	
	  <properties>
	    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	    <java.version>1.8</java.version>
	  </properties>
	
	  <dependencies>
	    <dependency>
	      <groupId>org.springframework.boot</groupId>
	      <artifactId>spring-boot-starter-web</artifactId>
	    </dependency>
	    <dependency>
	      <groupId>org.springframework.cloud</groupId>
	      <artifactId>spring-cloud-starter-eureka</artifactId>
	    </dependency>
	    <dependency>
	      <groupId>org.springframework.boot</groupId>
	      <artifactId>spring-boot-starter-actuator</artifactId>
	    </dependency>
	  </dependencies>
	
	  <!-- 引入spring cloud的依赖 -->
	  <dependencyManagement>
	    <dependencies>
	      <dependency>
	        <groupId>org.springframework.cloud</groupId>
	        <artifactId>spring-cloud-dependencies</artifactId>
	        <version>Camden.SR2</version>
	        <type>pom</type>
	        <scope>import</scope>
	      </dependency>
	    </dependencies>
	  </dependencyManagement>
	
	  <!-- 添加spring-boot的maven插件 -->
	  <build>
	    <plugins>
	      <plugin>
	        <groupId>org.springframework.boot</groupId>
	        <artifactId>spring-boot-maven-plugin</artifactId>
	      </plugin>
	    </plugins>
	  </build>  
2.写html页面   

```    

  	<form method="POST" enctype="multipart/form-data" action="/upload">  
		File to upload:
		<input type="file" name="file">
		<input type="submit" value="Upload">  

	</form>  
```  

3.编写controller  
  
	@Controller
	public class FileUploadController {
	  /**
	   * 上传文件
	   * 测试方法：
	   * 有界面的测试：http://localhost:8050/index.html
	   * 使用命令：curl -F "file=@文件全名" localhost:8050/upload
	   * ps.该示例比较简单，没有做IO异常、文件大小、文件非空等处理
	   * @param file 待上传的文件
	   * @return 文件在服务器上的绝对路径
	   * @throws IOException IO异常
	   */
	  @RequestMapping(value = "/upload", method = RequestMethod.POST)
	  public @ResponseBody String handleFileUpload(@RequestParam(value = "file", required = true) MultipartFile file) throws IOException {
	    byte[] bytes = file.getBytes();
	    File fileToSave = new File(file.getOriginalFilename());
	    FileCopyUtils.copy(bytes, fileToSave);
	    return fileToSave.getAbsolutePath();
	  }
	}
4.编写application.yml文件,注意：需要在启动类上添加@EnableEurekaClient注解  

	server:
	  port: 8050
	eureka:		#集成eureka
	  client:
	    serviceUrl:
	      defaultZone: http://user:password123@localhost:8761/eureka/
	  instance:
	    prefer-ip-address: true
	spring:
	  application:
	    name: microservice-file-upload
	  http:
	    multipart:
	      max-file-size: 2000Mb      # Max file size，默认1M
	      max-request-size: 2500Mb   # Max request size，默认10M


##spring cloud config
###统一管理配置
####Config Server
1.添加依赖  
  
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-config-server</artifactId>
		</dependency>
	</dependencies>  
  
2.在启动类上添加@EnableConfigServer注解  
  
3.application.yml配置文件配置git  
  
	server:
	  port: 8080
	spring:
	  cloud:
	    config:
	      server:
	        git:
	          uri: https://github.com/lixiaodong1994/spring-cloud-repo  
  
4.在浏览器中访问http://localhost:8080/abc-default.yml  
  
####Config client  
1.添加依赖    

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
	</dependencies>
  
2.创建一个controller类  
  
	@RestController
	public class ConfigClientController {
	
	  @Value("${profile}")
	  private String profile;
	
	  @GetMapping("/profile")
	  public String getProfile() {
	    return this.profile;
	  }
	}  
  
3.创建一个bootstrap.yml文件  
  
	spring:
	  cloud:
	    config:
	      uri: http://localhost:8080
	      profile: dev
	      label: master   # 当configserver的后端存储是Git时，默认就是master 
	  application:
	    name: foobar
	
	    #foobar-dev.yml  
  
> 这里如果不创建bootstrap.yml文件，就会出现8888端口问题
      
4.application.yml文件配置  
  
	server:
	  port: 8081   
  
5.在浏览器中访问：http://localhost:8081/profile可以直接访问github中对应的profile的内容。
  
####spring cloud加解密  
#####对称加解密
1.yml文件中，'{cipher}FKSAJDFGYOS8F7GLHAKERGFHLSAJ'需要填入类似这样一串字符串  
2.properties中， {cipher}FKSAJDFGYOS8F7GLHAKERGFHLSAJ 需要填类似这样的一串字符串，不需要加引号。  
#####非对称加解密
1.生成密钥    

	keytool -genkeypair -alias mytestkey -keyalg RSA \
	  -dname "CN=Web Server,OU=Unit,O=Organization,L=City,S=State,C=US" \
	  -keypass changeme -keystore server.jks -storepass letmein
2.将生成的密钥放到classpath目录下，然后修改application.yml文件  
  
	encrypt:
	  keyStore:
	    location: classpath:/server.jks
	    password: letmein
	    alias: mytestkey
	    secret: changeme  
3.重复对称加解密步骤
####配置刷新
#####自动更新
######client-客户端
1.加入依赖  
  
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bus-amqp</artifactId>
		</dependency>
	</dependencies>  
  
2.创建一个controller
  
	@RestController
	@RefreshScope
	public class ConfigClientController {
	
	  @Value("${profile}")
	  private String profile;
	
	  @GetMapping("/profile")
	  public String getProfile() {
	    return this.profile;
	  }
	}  

3.application文件配置  

	spring:
	  cloud:
	    config:
	      uri: http://localhost:8080
	      profile: dev
	      label: master   # 当configserver的后端存储是Git时，默认就是master 
	    bus:
	      trace:
	        enabled: true
	  application:
	    name: foobar
	  rabbitmq:  ##配置rabbitmq
	    host: localhost
	    port: 5672
	    username: guest
	    password: guest
  
######server-服务端  
1.加入依赖  

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-config-server</artifactId>
		</dependency>
	</dependencies>  
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-bus-amqp</artifactId>
	</dependency>
2.在启动类中加入@EnableConfigServer注解  
3.application配置文件配置  

	server:
	  port: 8080
	spring:
	  cloud:
	    config:
	      server:
	        git:
	          uri: https://github.com/lixiaodong1994/spring-cloud-repo 
  	rabbitmq:  ##配置rabbitmq
	    host: localhost
	    port: 5672
	    username: guest
	    password: guest  

4.启动rabbitmq  
5.修改文件上传到github  
6.发送curl -X POST http://localhost:8081/bus/refresh请求。  
> 想要实现springcloud自动刷新，需要在github上面进行配置
