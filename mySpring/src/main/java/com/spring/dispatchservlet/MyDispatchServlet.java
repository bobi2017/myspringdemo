package com.spring.dispatchservlet;

import com.spring.annotation.MyAutowired;
import com.spring.annotation.MyController;
import com.spring.annotation.MyRequestMapping;
import com.spring.annotation.MyService;
import com.sun.jndi.toolkit.url.Uri;
import netscape.javascript.JSObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author yaodong.zhai
 */
public class MyDispatchServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    private List<String> classNameList = new ArrayList();

    private Map<String,Object> iocMap = new HashMap();

    private Map<String,Method> handlerMappingMap = new HashMap();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            dispatch(req, resp);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        Method method = this.handlerMappingMap.get(url);
        if (method == null) {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }
        Map<String,String[]> map = req.getParameterMap();
        String beanName = getBeanName(method.getDeclaringClass().getSimpleName());
        method.setAccessible(true);
        Object object = handlerMappingMap.get(url).invoke(iocMap.get(beanName),req,resp);
        resp.getWriter().write(object.toString());
    }

    @Override
    public void init(ServletConfig servletConfig) {
        try {
            //第一步加载配置
            doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));
            //第二步加载class
            doScanner(contextConfig.getProperty("scan-package"));
            //第三步初始化ioc
            initIoc();
            //依赖注入
            initDi();
            //初始化HandlerMapping
            initHandlerMapping();
            System.out.println("初始化spring容器完成");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 加载配置
     * @param servletConfig
     */
    private void doLoadConfig(String servletConfig){
        InputStream io = this.getClass().getClassLoader().getResourceAsStream(servletConfig);
        try {
            contextConfig.load(io);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (io != null) {
                try {
                    io.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描相关类
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        if (resourcePath == null) {
            return;
        }
        File filePath = new File(resourcePath.getFile());
        for (File file : filePath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith("class")) {
                    continue;
                } else {
                    String className = scanPackage + "." + file.getName().replace(".class","");
                    classNameList.add(className);
                }
            }
        }
    }

    /**
     * 初始化IOC容器
     */
    private void initIoc() throws Exception{
        if (classNameList.size() == 0) {
            return;
        }
        for (String classPath :classNameList) {
            Class clazz = Class.forName(classPath);
            String beanName ="";
            if (clazz.isAnnotationPresent(MyController.class)) {
                MyController annotationMyController = (MyController)clazz.getAnnotation(MyController.class);
                if ("".equals(annotationMyController.value())) {
                    beanName = getBeanName(clazz.getSimpleName());
                } else {
                    beanName = annotationMyController.value();
                }
                if (iocMap.get(beanName) == null) {
                    iocMap.put(beanName,clazz.newInstance());
                } else {
                    throw new Exception("配置的Controller已经存在");
                }
            } else if (clazz.isAnnotationPresent(MyService.class)) {
                MyService annotationMyService = (MyService)clazz.getAnnotation(MyService.class);
                if (("".equals(annotationMyService.value()))) {
                   beanName = getBeanName(clazz.getSimpleName());
                } else {
                    beanName = annotationMyService.value();
                }
                if (iocMap.get(beanName) == null) {
                    iocMap.put(beanName,clazz.newInstance());
                    // getInterfaces 此方法返回这个类中实现接口的数组
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist.");
                        }
                        // 接口不能实例化，接口存实现类的实例
                        iocMap.put(i.getName(), clazz.newInstance());
                    }
                } else {
                    throw new Exception("配置的Service Bean已经存在");
                }
            }

        }
    }

    private void initDi() {
        if (iocMap.isEmpty()) {
            return;
        }
        Set<String> set = iocMap.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = iocMap.get(key);
            Field[] fields = value.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                //private 访问
                field.setAccessible(true);
                try {
                    field.set(value,iocMap.get(beanName));
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void initHandlerMapping() {
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/" + baseUrl + "/" + myRequestMapping.value()).replaceAll("/+", "/");
                handlerMappingMap.put(url, method);
            }
        }
    }

    private String getBeanName(String className) {
            char[] charArray = className.toCharArray();
            charArray[0] += 32;
            return String.valueOf(charArray);
    }
}
