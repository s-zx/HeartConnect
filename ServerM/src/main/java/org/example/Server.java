package org.example;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;



/**
 * C/S架构的服务端对象。
 */
public class Server {

    /**
     * 要处理客户端发来的对象，并返回一个对象，可实现该接口。
     */
    public interface ObjectAction{
        Object doAction(Object rev, Server server);
    }

    public static final class DefaultObjectAction implements ObjectAction{
        public Object doAction(Object rev,Server server) {
            System.out.println("处理并返回："+rev);
            return rev;
        }
    }

    public static void main(String[] args) {
        int port = 9379;
        Server server = new Server(port);
        server.start();
    }

    private int port;
    private volatile boolean running=false;
    private long receiveTimeDelay=3000;
    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class,ObjectAction>();
    private Thread connWatchDog;

    public Server(int port) {
        this.port = port;
    }

    public void start(){
        if(running)return;
        running=true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    @SuppressWarnings("deprecation")
    public void stop(){
        if(running)running=false;
        if(connWatchDog!=null)connWatchDog.stop();
    }

    public void addActionMap(Class<Object> cls,ObjectAction action){
        actionMapping.put(cls, action);
    }

    class ConnWatchDog implements Runnable{
        public void run(){
            try {
                ServerSocket ss = new ServerSocket(port,5);
                while(running){
                    Socket s = ss.accept();
                    new Thread(new SocketAction(s)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Server.this.stop();
            }

        }
    }



    class SocketAction implements Runnable{
        Socket s;
        boolean run=true;
        long lastReceiveTime = System.currentTimeMillis();
        public SocketAction(Socket s) {
            this.s = s;
        }
        public void run() {
            while(running && run){
                if(System.currentTimeMillis()-lastReceiveTime>receiveTimeDelay){
                    sendEmailAlert();
                    overThis();
                }else{
                    try {
                        InputStream in = s.getInputStream();
                        if(in.available()>0){
                            ObjectInputStream ois = new ObjectInputStream(in);
                            Object obj = ois.readObject();
                            System.out.println(obj);
                            lastReceiveTime = System.currentTimeMillis();
                            System.out.println("接收：\t"+obj);
                            ObjectAction oa = actionMapping.get(obj.getClass());
                            oa = oa==null?new DefaultObjectAction():oa;
                            Object out = oa.doAction(obj,Server.this);
                            if(out!=null){
                                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                                oos.writeObject(out);
                                oos.flush();
                            }
                        }else{
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        overThis();
                    }
                }
            }
        }

        private void sendEmailAlert() {
            // 创建邮件会话
            String SMTP_HOST = "smtp.qq.com"; // SMTP服务器地址
            String FROM_EMAIL = "***********@qq.com"; // 发件人邮箱
            String FROM_EMAIL_PASSWORD = "*********"; // 授权码
            String TO_EMAIL = "**********@qq.com"; // 收件人邮箱
            String SUBJECT = "服务端心跳异常报警"; // 邮件主题
            String content = "服务端已经 " + (receiveTimeDelay / 1000) + " 秒没有收到客户端心跳了，请及时处理！"; // 邮件内容

            Properties props = new Properties();
            props.setProperty("mail.smtp.host", SMTP_HOST);
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.port", "587"); // 主机端口号
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_EMAIL_PASSWORD);
                }
            });

            MimeMessage message = new MimeMessage(session);
            try {
                message.setFrom(new InternetAddress(FROM_EMAIL));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(TO_EMAIL));
                message.setSubject(SUBJECT);
                message.setText(content);
                Transport.send(message); // 发送邮件
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        private void overThis() {
            if(run)run=false;
            if(s!=null){
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("关闭："+s.getRemoteSocketAddress());
        }
    }

}



