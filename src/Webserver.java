//Name: Jonathan Allen
//File: Webserver.java
//Use: Multithreaded web server implementation

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;

//Multi thread webserver
public class Webserver {
	private static ServerSocket serverSocket;

	public static void main(String[] args) throws IOException {
		try {
			serverSocket=new ServerSocket(2880);
			Configuration config = new Configuration( "/Users/jhallen/networks/hw4/conf/config.xml" ) ;

			while (true) {
				Socket s=serverSocket.accept();  // Wait for a client to connect
				new ClientHandler(s, config);  // Handle the client in a separate thread
			} 
		}catch (Exception x) {
			System.out.println(x);
		}
	}
}

// A ClientHandler reads an HTTP request and responds
class ClientHandler extends Thread {

	private Socket socket;  // The accepted socket from the Webserver
	private Configuration fig; //Accept config file

	// Start the thread in the constructor
	public ClientHandler(Socket s, Configuration c) {
		socket = s ;
		fig = c ;
		start();
	}

	public String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

	public void PrintHeader(PrintStream p, String status, String contentType, long l ) {

		String oStatus = "200 OK";

		if( status == "200" ) {
			oStatus = "200 OK";
		}else if (status == "400") {
			oStatus = "400 Bad Request";
		}else if (status == "404") {
			oStatus = "404 Not Found";
		}

		String pStatus = "HTTP/1.1 " + oStatus + "\r\n";

		p.print(
				pStatus +
				"Date: " + getServerTime() + "\r\n" +
				"Server: " + fig.getServerName() + "\r\n" +
				"Content-Type: " + contentType + "\r\n" +
				"Content-Length: " + l + "\r\n" +
				"Connection: close " + "\r\n\r\n"
				);
	}

	public void LogRequest(String request, String status, long len ) throws IOException {
		String currentHost = socket.getInetAddress().getHostName() + " ";
		String date = "[" + getServerTime() + "] ";
		String requestFormat = "\"" + request + "\"" ;
		
		String formattedLog = currentHost + date + requestFormat + " " + status + " " + len;
		try{
			FileWriter outLog = new FileWriter(fig.getLogFile(), true);
			outLog.write(formattedLog + "\n");
			outLog.close();
		}catch (IOException f){
			System.err.println(f);
		}
	}
	
	public void handle400(PrintStream pS, String s) throws IOException{
		String file400 = fig.getDocumentRoot() + "/conf/400.html" ;
		InputStream fin400= new FileInputStream(file400);
		File size400 = new File(file400);
		
		//print 404 header
		PrintHeader(pS, "400", "text/html", size400.length());
		
		//send 404.html
		byte[] a=new byte[1024];
		int n;
		while ((n=fin400.read(a))>0) {
			pS.write(a, 0, n);
		}
		
		//log 404
		LogRequest(s, "400", size400.length());
		System.out.println("400 Error - Redirect to conf/400.html");
		
		//close streams
		fin400.close();
		pS.close();
	}
	
	public void handle404(PrintStream pS, String s) throws IOException{
		String file404 = fig.getDocumentRoot() + "conf/404.html" ;
		InputStream fin404= new FileInputStream(file404);
		File size404 = new File(file404);
		
		//print 404 header
		PrintHeader(pS, "404", "text/html", size404.length());
		
		//send 404.html
		byte[] byte1=new byte[2048];
		int num;
		while ((num=fin404.read(byte1))>0) {
			pS.write(byte1, 0, num);
		}
		
		//log 404
		LogRequest(s, "404", size404.length());
		System.out.println("404 Error - Redirect to conf/404.html");
		
		//close streams
		fin404.close();
		pS.close();
	}

	// Read the HTTP request, respond, and close the connection
	public void run() {
		try {
			// Open connections to the socket
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintStream out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
			
			//Read request & log to console
			String s=in.readLine();
			System.out.println(s);

			//Try to send file, catch -> 404/400 if throws error
			String filename="";
			StringTokenizer st=new StringTokenizer(s);
			try {

				// Parse the filename from the GET command
				if (st.hasMoreElements() && st.nextToken().equalsIgnoreCase("GET") && st.hasMoreElements()) {
					filename=st.nextToken();
				}else{
					handle400(out,s);
					throw new Exception();
				}

				// Append trailing "/" with "index.html"
				if (filename.endsWith("/")) {
					filename="index.html";
				}

				// Remove leading / from filename
				while (filename.indexOf("/")==0) {
					filename=filename.substring(1);
				}

				// Replace "/" with "\" in path for PC-based servers
				filename=filename.replace('/', File.separator.charAt(0));

				// Check for illegal characters to prevent access to superdirectories
				if (filename.indexOf("..")>=0 || filename.indexOf(':')>=0 || filename.indexOf('|')>=0) {
					handle400(out,s);
					throw new Exception();
				}

				
				try {
					// Open the file, not found -> filenotfoundex -> conf/404.html
					InputStream f= new FileInputStream(fig.getDocumentRoot() + filename);
					File filesize = new File(fig.getDocumentRoot() + filename);
					
					// Determine the MIME type and print HTTP header
					String mimeType="text/plain";
					if (filename.endsWith(".html") || filename.endsWith(".htm")) {
						mimeType="text/html";
					}else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
						mimeType="image/jpeg";
					}else if (filename.endsWith(".gif")) {
						mimeType="image/gif";
					}

					//Send header & write to log
					PrintHeader(out, "200", mimeType, filesize.length());
					LogRequest(s, "200", filesize.length());
					
					// Send file contents to client, then close the connection
					byte[] a=new byte[1024];
					int n;
					while ((n=f.read(a))>0) {
						out.write(a, 0, n);
					}
					
					//close connections
					out.close();
					f.close();
					
				}catch (FileNotFoundException ex){
					handle404(out,s);
				}
								
			}catch (FileNotFoundException x) {
				out.close();
			}catch (Exception e){
				out.close();
			}
		}catch (IOException x) {
			System.out.println(x);
		}
	}
}
