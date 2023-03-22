#HeartConnect

## 开发过程中遇到的问题

1. 服务端和客户端连接时，报ClassNotFoundException异常：反序列化ObjectOutputStream.readObject()出现ClassNotFoundException。

跨进程通信时，将序列化的对象的对象存入文件，两个进程通过读/写同一个文件来交换数据，达到通讯的效果。服务端和客户端通讯的过程中中间遇到一个问题，就是在另一个进程反序列化的时候，报ClassNotFoundException异常。

原因是两进程序列化类的包名不同，将两个类的包名改成一样的就可以解决了。

2. maven打包好之后，使用java -jar命令运行时，报错：xxx.jar中没有主清单属性。

默认情况下 maven 项目不会帮我们创建任何类，如果不指定我们类中的 main 方法作为入口点，那么在执行时会报如下错误。

解决的办法就是告诉 maven 去哪个类中找 main 方法并执行就行了；默认生成的配置文件是没有这个信息的，在pom文件中添加 build 结点配置maven 编译时的用到的信息就可以了。

```xml
<plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                            <mainClass>com.letcode.Solution</mainClass> <!-- 指定程序入口点所在类 -->
                        </manifest>
                    </archive>
                </configuration>

            </plugin>
</plugins>
```

3. 用maven打包为jar后，运行jar包时，无法识别javamail的jar包，因而无法调用javamail的功能。

当使用Maven打包为JAR文件时，Maven会将项目及其依赖项打包成一个JAR文件。但是，默认情况下，JAR文件不包括依赖项。这意味着，如果JavaMail JAR包不在JAR文件的classpath中，那么JVM将无法识别JavaMail库中的类和方法，从而无法实现JavaMail的功能。

解决方法：在打包JAR文件时，使用Maven的插件将依赖项打包到JAR文件中。例如，可以使用Maven的maven-assembly-plugin插件或maven-shade-plugin插件来创建一个“超级JAR”，该JAR包括应用程序和其所有依赖项。

