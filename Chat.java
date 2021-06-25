import java.net.*;
import java.util.*;
import java.io.*;

public class Chat {

	public static void main(String[] args) throws IOException {
		Random rand = new Random();
		int port = rand.nextInt(9000), newPort, connectID;
		InetAddress myip = InetAddress.getLocalHost();
		String choice, address, message;
		Scanner sc = new Scanner(System.in);
		boolean exit = false;
		Server myServer = new Server(myip.getHostAddress(),port);
		Thread serv = new Thread(myServer);
		serv.start();
		while(!exit) {
			choice = sc.next();
			switch(choice) {
				case "help": {
					System.out.println("1. 'help' Display information about the available user interface options or command manual.\n2. 'myip' Display the IP address of this process.\n"
							+ "3. 'myport' Display the port on which this process is listening for incoming connections.\n"
							+ "4. 'connect'  <destination>  <port  no>  : This  command establishes  a  new TCP  connection to  the  specified<destination> at the specified < port no>.\n"
							+ "5. 'list' Display a numbered list of all the connections this process is part of.\n6. 'terminate'  <connection  id.> This  command  will  terminate  the  connection"
							+ "  listed under  the  specifiednumber  when  LIST  is  used  to  display  all  connections.\n"
							+ "7. 'send'  <connection id.>  <message>\n8. 'exit' Close all connections and terminate this process.");
					break;
				}
				case "myip": {
					System.out.println(myip.getHostAddress());
					break;
				}
				case "myport":{
					System.out.println("Port: " + port);
					break;
				}
				case "connect" :{
					address = sc.next();
					newPort = sc.nextInt();
					if(address == myip.getHostAddress() && newPort == port) {
						System.out.println("You entered your own address.");
					}
					else {
						myServer.addClient(address, newPort);
					}
					break;
				}
				case "list": {
					myServer.clientListToString();
					break;
				}
				case "terminate": {
					//close client
					connectID = sc.nextInt();
					myServer.termClient(connectID);
					break;
				}
				case "send": {
					connectID = sc.nextInt();
					message = sc.nextLine();
					myServer.clients.get(connectID - 1).write(message);
					break;
				}
				case "exit": {
					//close clients
					myServer.close();
					exit = true;
					System.out.println("Stopped");
					break;
				}
				default: {
					System.out.println("You have entered an invalid command. Enter 'help' to get a list of commands");
				}
			}
		
		}
		sc.close();

	}

}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Client implements Runnable {
	Socket socket;
	public DataInputStream in = null; 
	public DataOutputStream out	 = null;
	Server myServer;
	boolean running = true;
	public String address;
	public int port;
	public int clientNum;
	Client cli = this;
	public Client(Socket socket, DataInputStream in, DataOutputStream out, Server server, int num, String address, int port) {
		this.in = in;
		this.out = out;
		this.address = address;
		this.port = port;
		this.socket = socket;
		this.myServer = server;
		this.clientNum = num;
	}
	public void run() {
		Thread readMessage = new Thread(new Runnable() {  //reads inputs from other servers, whether it be a command or the clients messaging each other
			@Override
			public void run() {
				while(running) {
					try {
						String serverMessage = in.readUTF();
						String[] buff = serverMessage.split(" ");
						switch (buff[0]) {
							case("5tht"): { //add client
								String newAddress = buff[1];
								System.out.println("The new address is: " + newAddress);
								int newPort = Integer.parseInt(buff[2]);
								myServer.addClient(newAddress, newPort);
								break;
							}
							case("4term78"): { //terminate
								myServer.disconnect(cli);
								running = false;
								socket.close();
								break;
							}
							case("gh123"): {
								System.out.println("Connection to "+ address+" " + port +" was successful");
								break;
							}
							default: {//print message received message
								System.out.println("Message recieved from " + address);
								System.out.println("Sender's Port: " + port);
								System.out.println("Message: " + serverMessage.substring(1));
								break;
							}
						}
					}
					catch (IOException e){
						System.out.println(e);
					}
				}
			}
		});
		readMessage.start();
	}
	public Socket getSocket() {
		return socket;
	}
	//add give method that takes a client
	public void write(String message) throws IOException {
		out.writeUTF(message);
		System.out.println("Message sent.");
	}
	public void remove() throws IOException {
		running = false;
		socket.close();
		
	}
	
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Server implements Runnable {
	//private Socket		 socket = null; 
	private ServerSocket server = null; 
	private DataInputStream in	 = null; 
	private DataOutputStream out = null;
	//Server theServer;
	private Thread client = null;
	public boolean running = true;
	Vector<Client> clients = new Vector<>();
	int clientCount = 0;
	Thread myServer;
	String address;

	private int port;

	public Server(String address, int port) throws IOException {
		this.port = port;
		this.address = address;
		server = new ServerSocket(port);
		myServer = new Thread(this);
		myServer.start();
	}
	public void close() throws IOException {
		while(!clients.isEmpty()) {
			this.termClient(1);
		}
		running = false;
		//socket.close();
		server.close();
		//myServer.stop();
	}
	
	public void addClient(String newAdd, int newPort) throws UnknownHostException, IOException {
		boolean isListed = false;
		for(int i = 0; i < clientCount; i++) {
			if (clients.get(i).address == newAdd && clients.get(i).port == newPort) {
				isListed = true;
			}
		}
		if (isListed) {
			System.out.println("This client has already been added.");
		} else {
			Socket socket = new Socket(newAdd, newPort);
			DataInputStream newIn = new DataInputStream(socket.getInputStream());
			DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
			newOut.writeUTF(address+":" + port);
			Client client = new Client(socket, newIn, newOut, this, clientCount, newAdd, newPort);
		//this will send all of the clients to the other server
			Thread cli = new Thread(client);
			clients.add(client);
			cli.start();
			clientCount++;
		}
	}
	
	public void run() {
		try {
			while(running) {
				Socket socket = server.accept();
				DataInputStream newIn = new DataInputStream(socket.getInputStream());
				DataOutputStream newOut = new DataOutputStream(socket.getOutputStream());
				String newAdd = newIn.readUTF();
				String[] sock = newAdd.split(":");
				System.out.println("IP: "+sock[0] + " Port: " + Integer.parseInt(sock[1]) + " has joined.");
				Client client = new Client(socket, newIn, newOut, this, clientCount, sock[0], Integer.parseInt(sock[1]));
				Thread cli = new Thread(client);
				clients.add(client);
				cli.start();
				newOut.writeUTF("gh123");//this tells the new client that the connection was successful, completing the TCP handshake
				clientCount++;
			}

		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
	public void clientListToString() {
		System.out.println("IP Address\t        Port");
		for(int i = 0; i < clients.size(); i++) {
			System.out.println((i+1) +". " +  clients.get(i).address + "\t" + clients.get(i).port);
		}
		
	}
	public int getPort() {
		return port;
	}
	public void termClient(int id) throws IOException {
		if (!clients.isEmpty()) {
			clients.get(id-1).out.writeUTF("4term78");
		}
		clients.get(id-1).remove();
		clients.remove(id - 1);
		clientCount--;
		System.out.println("User " + id + " has disconnected.");
	}
	public void disconnect(Client cli) {
		int id = clients.indexOf(cli);
		try {
			clients.get(id).remove();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clients.remove(id);
		clientCount--;
		System.out.println("User " + (id + 1) + " has disconnected.");
	}
	
}