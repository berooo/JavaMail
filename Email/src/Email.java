import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Security;
import java.sql.*;

/**
 * @author bero-
 *
 */
public class Email {
		
		private static final String host="smtp.sina.com";
		private static final String home_address="diamond_bero@sina.com";
		private static final String password="a13992128496";
		private static StringBuffer bodytext=new StringBuffer();		//用于保存收取的邮件的文本
		private static ArrayList<String> strArray=new ArrayList<String>();
			
		public static void connection(){		//连接数据库的函数
			//驱动程序名
			String driver="com.mysql.jdbc.Driver";
			//URL指向要访问的数据库名email
			String url="jdbc:mysql://127.0.0.1:3306/email";	
			//Mysql配置时的用户名
			String user="root";
			//Mysql配置时的密码
			String password="1819";
			
			try{
				//加载驱动程序
				Class.forName(driver);
				//连接数据库
				Connection conn=DriverManager.getConnection(url,user,password);
				
				if(!conn.isClosed())
					System.out.println("Succeeded connection to the Database!");
				
				//statement用来执行SQL语句
				Statement statement=conn.createStatement();
				//要执行的sql语句
				String sql="select * from emails";
				
				//结果集
				ResultSet rs=statement.executeQuery(sql);
				
				String name=null;
				
				while(rs.next())
				{
					name=rs.getString("address");
					name=new String(name.getBytes("ISO-8859-1"),"GB2312");
					System.out.println(rs.getString("id")+"\t"+name);
					strArray.add(name);
				}
				
				rs.close();
				conn.close();
			}catch(ClassNotFoundException e)
			{
				System.out.println("Sorry,can't find the driver.");
				e.printStackTrace();
			}catch(SQLException e)
			{
				e.printStackTrace();
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		//发送邮件的函数
		public static void sendMail(String target_address) throws FileNotFoundException, IOException, InterruptedException
		{
			
			Properties props=System.getProperties();		//设置属性
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.auth", "true");
			Session session=Session.getDefaultInstance(props,new Authenticator(){		//创建一个session
				@Override
				public PasswordAuthentication getPasswordAuthentication(){
					return new PasswordAuthentication(home_address,password);
				}
			});
			
			try{
				MimeMessage message=new MimeMessage(session);		//邮件消息头
				message.setFrom(new InternetAddress(home_address));			//设置发件人
				message.addRecipient(Message.RecipientType.TO,new InternetAddress(target_address));			//设置邮件收件人
				
				message.setSubject("java");			//设置主题
				message.setText("Do you like me?");		//设置文本
				
				MimeMultipart msgMultipart=new MimeMultipart("mixed");			//描述数据关系
				message.setContent(msgMultipart);
				
				MimeMultipart allpart=new MimeMultipart("mixed");		//描述数据关系
				
				
				//附件部分
				MimeBodyPart attachmentpart=new MimeBodyPart();
				FileDataSource fds_1=new FileDataSource("text.txt");
				attachmentpart.setDataHandler(new DataHandler(fds_1));
				attachmentpart.setFileName(fds_1.getName());
				
				//用于保存最终正文部分
				
				MimeBodyPart contentBody=new MimeBodyPart();
				//用于组合文本和图片，“related”型的MimeBodyPart对象
				MimeMultipart contentMulti=new MimeMultipart("related");
				
				//正文的图片部分
				MimeBodyPart jpgBody=new MimeBodyPart();
				FileDataSource fds_2=new FileDataSource("picture.jpg");
				jpgBody.setDataHandler(new DataHandler(fds_2));
				jpgBody.setContentID("picture_jpg");
				contentMulti.addBodyPart(jpgBody);
				
				//正文的文本部分
				MimeBodyPart textBody=new MimeBodyPart();
				textBody.setContent("Do you like me?<img src='cid:picture_jpg'>","text/html;charset=gbk");
				contentMulti.addBodyPart(textBody);
						
				contentBody.setContent(contentMulti);
				
				allpart.addBodyPart(contentBody);
				allpart.addBodyPart(attachmentpart);
				
				message.setContent(allpart);			//设置消息内容
				message.saveChanges();					//保存修改
				Transport.send(message);				//发送消息
			}catch(MessagingException e)
			{
				e.printStackTrace();
			}
			System.out.println("Email sending succeeded!");
		}
		public static void received() throws Exception		//接收邮件的函数
		{
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			final String SSL_FACTORY="javax.net.ssl.SSLSocketFactory";
			
			Properties props=System.getProperties();			//设置一些属性
			props.setProperty("mail.pop3.host", "pop3.sina.com");
			props.setProperty("mail.pop3.socketFactory.fallback", "false");
			props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
			props.setProperty("mail.pop3.port", "995");
			props.setProperty("mail.pop3.socketFactory.port", "995");
			props.setProperty("mail.pop3.auth", "true");
			Session session=Session.getInstance(props, null);
			//设置连接邮件仓库的环境
			
			URLName url=new URLName("pop3","pop.sina.com",995,null,home_address,password);
			Store store=null;
			Folder folder=null;			//文件夹
			try{
				store=session.getStore(url);
				store.connect(home_address, password);			//连接
				
				folder=store.getFolder("INBOX");				//打开文件夹
				folder.open(Folder.READ_ONLY);
				
				int size=folder.getMessageCount();
				Message message=folder.getMessage(size);
				
				getMailContent((Part)message);					//获取第一封邮件内容
				System.out.println(bodytext.toString());
				
			}catch(NoSuchProviderException e)
			{
				e.printStackTrace();
			}catch(MessagingException e)
			{
				e.printStackTrace();
			}
			finally{
				if(folder!=null)
				{
					folder.close(false);
				}
				if(store!=null)
				{
					store.close();
				}
			}
			System.out.println("接收完毕");
		}
		public static void getMailContent(Part part)throws Exception		//获取邮件内容
		{
			String contenttype=part.getContentType();
			int nameindex=contenttype.indexOf("name");
			boolean conname=false;
			if(nameindex!=-1)
				conname=true;
			//分类讨论处理
			if(part.isMimeType("text/plain")&&!conname){
				bodytext.replace(0, bodytext.length(), (String)part.getContent());
			}else if(part.isMimeType("multipart/*")){
				Multipart multipart=(Multipart)part.getContent();
				int counts=multipart.getCount();
				for(int i=0;i<counts;i++){
					getMailContent(multipart.getBodyPart(i));
				}
			}else{}
		}
		public static void analyze()			//对邮件内容进行简单分析
		{
			String string=bodytext.toString();
			String sub_1="Yes";
			String sub_2="No";
			
			int a=string.indexOf(sub_1);
			int b=string.indexOf(sub_2);
			
			if(a>=0){
				System.out.println("The meaning of the response:He likes you.");
			}else if(b>=0){
				System.out.println("The meaning of the response:He doesn't like you.");
			}
			
		}
		public static void main(String args[]) throws Exception
		{
		Thread t=new Thread(){					//开启读取邮件线程
				@Override
				public void run(){
					while(true){
						try {
							received();
							System.out.println("..");
							analyze();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			t.start();
			connection();					//连接数据库
			for(int i=0;i<5;i++)
			{
				for(int j=0;j<strArray.size();j++)
					{
					sendMail(strArray.get(j));			//发邮件
					}
			}
	}
}
