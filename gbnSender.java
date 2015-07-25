import java.io.IOException;
import java.net.DatagramPacket;

public class gbnSender extends Sender {
  public static int WINDOW_SIZE = 10; 
  
  private long latestAckTime = 0;
  private int latestAck = -1;
  private int sequenceToSend = -1;
  
  public gbnSender(long timeout, String fileName) {
    super(timeout, fileName);
  }
  
  public boolean isTimedOut() {
    return (System.currentTimeMillis() - this.latestAckTime) > Sender.TIMEOUT; 
  }

  public void send() {
    for (sequenceToSend = 0; sequenceToSend < fileContents.size(); sequenceToSend++) {
      // Check latestAck > 0 just in case we get stuck in this loop 
      // at the very beginning (latestAck initialized to -1).
      while (sequenceToSend - latestAck > WINDOW_SIZE && latestAck > 0) {
        // If we haven't gotten an ACK in a while, then send from
        // the beginning of the window.
        if (this.isTimedOut()) {
          this.sequenceToSend = this.latestAck + 1;
        }
        
        System.out.println("sequenceToSend: " + sequenceToSend);
        System.out.println("latestAck: " + latestAck);
        
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {}
      }
      
      byte[] chunk = this.fileContents.get(sequenceToSend);
      
      // Package chunk in payload.
      CS456Packet packet = new CS456Packet(0, this.sequenceToSend, chunk);
      
      // Construct Datagram out of raw bytes.
      byte[] rawPacket = packet.getRaw();
      DatagramPacket formattedPacket = new DatagramPacket(rawPacket, rawPacket.length, this.host, this.port); 
      
      // Send to socket.
      try {
        this.socket.send(formattedPacket);
        packet.printLog(true);
      } catch (IOException e) {}
      
      this.sequenceToSend++;
    }
    
    // Send EOT packet.
    CS456Packet packetEOT = new CS456Packet(2, this.sequenceToSend, new byte[0]);
    byte[] rawEOT = packetEOT.getRaw();
    DatagramPacket formattedPacketEOT = new DatagramPacket(rawEOT, rawEOT.length, this.host, this.port);
    try {
      socket.send(formattedPacketEOT);
    } catch (IOException e) {}
  }
  
  public void listen() {
    DatagramPacket ackPacket = new DatagramPacket(new byte[512], 512);
    while (this.foundEOTPacket == false) {
      try {
        socket.receive(ackPacket);
      } catch (IOException e) {}

      CS456Packet parsedPacket = new CS456Packet(ackPacket.getData());
      parsedPacket.printLog(false);
      
      // The received ACK is valid if it's greater than or equal to our latest ACK.
      if (parsedPacket.getSequenceNumber() > this.latestAck) {
        this.latestAck = parsedPacket.getSequenceNumber();
        this.latestAckTime = System.currentTimeMillis(); 
      }
      
      if (parsedPacket.isEOT()) {
        this.foundEOTPacket = true;
      }
    }
  }
  
  @Override
  public void start() {
    Thread sender = new Thread() {
      public void run() {
        send();
      }
    };
    
    Thread listener = new Thread() {
      public void run() {
        listen();
      }
    };
    
    listener.start();
    sender.start();
  }
  
  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Expected 2 arguments: timeout(ms) infile");
      System.exit(0);
    }
    
    gbnSender sender = new gbnSender(Long.valueOf(args[0]).longValue(), args[1]);
    sender.start();
  }
}
