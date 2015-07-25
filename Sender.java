import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class Sender {
  public static String SOCKET_FILE = "channelInfo";
  public static int MAX_PAYLOAD_SIZE = 500;
  public static long TIMEOUT = 0;
  
  protected InetAddress host;
  protected int port;
  protected DatagramSocket socket;
  protected List<byte[]> fileContents;
  protected List<CS456Packet> packetQueue;
  protected boolean foundEOTPacket = false;
  
  public Sender(long timeout, String fileName) {
    Sender.TIMEOUT = timeout;
    this.fileContents = this.readFileChunks(fileName, MAX_PAYLOAD_SIZE);
    
    // Create packetQueue from fileContents.
    this.createPacketQueue();
    
    // Creates the socket from SOCKET_FILE
    createSocket();
  }
  
  public void createSocket() {
    byte[] channelInfo = this.readFile(SOCKET_FILE);
    String[] socketInfo = new String(channelInfo).split(" ");
    try {
      String hostName = socketInfo[0];
      if (hostName.equals("127.0.1.1")) {
        this.host = InetAddress.getLocalHost();
      } else {
        this.host = InetAddress.getByName(hostName);
      }
      
      this.port = Integer.valueOf(socketInfo[1]).intValue();
    } catch (UnknownHostException e1) {}
    try {
      this.socket = new DatagramSocket();
    } catch (SocketException e) {
      System.out.println("Failed to connect to port " + this.port + " " + e.toString());
    } 
  }
  
  public List<byte[]> readFileChunks(String fileName, int chunkSize) {
    List<byte[]> packetList = new ArrayList<byte[]>(); 
    
    // Open file.
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(fileName);
    } catch (FileNotFoundException e) {
      System.out.println("Could not find file " + fileName);
      return packetList;
    }
    
    byte[] payload = new byte[chunkSize];
    int payloadSize = -1;
    try {
      payloadSize = fileInputStream.read(payload);
    } catch (IOException e1) {}
    // Loop through file and add payload to our payload List.
    while (payloadSize != -1) {
      try {
        if (payloadSize < chunkSize) {
          payload = this.resizeByteArray(payload, payloadSize);
        }
        packetList.add(payload);
        payload = new byte[chunkSize];
        payloadSize = fileInputStream.read(payload);
      } catch (IOException e) {}
    }
    
    // Close file.
    try {
      fileInputStream.close();
    } catch (IOException e) {}
    
    return packetList;
  }
  
  public void createPacketQueue() {
    this.packetQueue = new ArrayList<CS456Packet>();
    
    // Shove file contents into packet queue.
    int count = 0;
    for (byte[] chunk : fileContents) {
      CS456Packet packet = new CS456Packet(0, count, chunk);
      this.packetQueue.add(packet);
      count++;
    }
  }
  
  public byte[] readFile(String fileName) {
    ByteArrayOutputStream fileContentByteBuffer = new ByteArrayOutputStream();
    
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(fileName);
    } catch (FileNotFoundException e1) {}
    
    try {
      int content;
      while ((content = inputStream.read()) != -1) {
        fileContentByteBuffer.write(content);
      }
    } catch (IOException e) {} finally {
        try {
          inputStream.close();
        } catch (IOException e) {}
    }
    
    return fileContentByteBuffer.toByteArray();
  }
  
  private byte[] resizeByteArray(byte[] bytes, int size) {
    byte[] newBytes = new byte[size];
    System.arraycopy(bytes, 0, newBytes, 0, size);
    return newBytes;
  }
  
  public void start() {
    int i = 0;
    for (byte[] chunk : this.fileContents) {
      // Package chunk in payload.
      CS456Packet packet = new CS456Packet(0, i, chunk);
      
      System.out.println("Sending " + packet.toString());
      
      // Construct Datagram out of raw bytes.
      byte[] rawPacket = packet.getRaw();
      DatagramPacket formattedPacket = new DatagramPacket(rawPacket, rawPacket.length, this.host, this.port); 
      
      // Send to socket.
      try {
        this.socket.send(formattedPacket);
      } catch (IOException e) {}
      
      i++;
    }
  }
  
  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Expected 2 arguments: timeout(ms) infile");
      System.exit(0);
    }
    
    Sender sender = new Sender(Long.valueOf(args[0]).longValue(), args[1]);
    sender.start();
  }
}
