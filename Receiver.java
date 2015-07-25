import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Receiver {
  public static String SOCKET_FILE = "recvInfo";

  protected File outFile;
  protected FileOutputStream outFileStream;
  protected DatagramSocket socket;
  protected DatagramPacket receivedPacket = new DatagramPacket(new byte[512], 512);
  protected boolean foundEOTPacket = false;

  public Receiver(String outFileName) {
    // Create File
    this.outFile = new File(outFileName);
    if (!this.outFile.exists()) {
      try {
        this.outFile.createNewFile();
      } catch (IOException e) {}
    } else {
      try {
        this.outFile.delete();
        this.outFile.createNewFile();
      } catch (IOException e) {}
    }

    // Create FileWriter
    try {
      this.outFileStream = new FileOutputStream(outFile, true);
    } catch (FileNotFoundException e) {}

    // Creates socket and writes the hostname and portnumber into recvInfo
    createSocket();
  }

  public void createSocket() {
    try {
      this.socket = new DatagramSocket();
    } catch (SocketException e) {}

    // Get Socket info
    String socketInfo = "";
    try {
      socketInfo = InetAddress.getLocalHost().getHostName()
          + " " + this.socket.getLocalPort();
    } catch (UnknownHostException e1) {}

    // Write Socket Info to file.
    File socketFile = new File(SOCKET_FILE);
    FileOutputStream socketWriter = null;
    try {
      socketWriter = new FileOutputStream(socketFile, false);
      socketWriter.write(socketInfo.getBytes());
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    } finally {
      try {
        socketWriter.close();
      } catch (IOException e) {}
    }
  }
  
  public void sendAck(int sequenceNumber, InetAddress address, int port, boolean EOT) {
    // Signal ACK or EOT?
    int packetType = 1;
    if (EOT) {
      packetType = 2;
    }
    
    CS456Packet packet = new CS456Packet(packetType, sequenceNumber, new byte[0]);
    packet.printLog(true);
    
    // Construct Datagram out of raw bytes.
    byte[] rawPacket = packet.getRaw();
    DatagramPacket formattedPacket = new DatagramPacket(rawPacket, rawPacket.length, address, port); 
    
    // Send to socket.
    try {
      this.socket.send(formattedPacket);
    } catch (IOException e) {}
  }
  
  public void start() {
    while (this.foundEOTPacket == false) {
      try {
        socket.receive(receivedPacket);
      } catch (IOException e) {}

      CS456Packet parsedPacket = new CS456Packet(receivedPacket.getData());
      this.writeToFile(parsedPacket.getPayload());
      parsedPacket.printLog(false);
      this.sendAck(parsedPacket.getSequenceNumber(),
                                receivedPacket.getAddress(),
                                receivedPacket.getPort(),
                                parsedPacket.isEOT());
      
      if (parsedPacket.isEOT()) {
        this.end();
      }
    }
  }
  
  public void end() {
    try {
      this.outFileStream.close();
    } catch (IOException e) {}
    this.foundEOTPacket = true;
  }
  
  protected void writeToFile(String content) {
    try {
      this.outFileStream.write(content.getBytes());
    } catch (IOException e) {}
  }

  /**
   * arg1: filename - the filename where the transferred file is written.
   */
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Expected 1 argument: outfile");
      System.exit(0);
    }

    Receiver receiver = new Receiver(args[0]);
    receiver.start();
  }
}
