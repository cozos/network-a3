import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class gbnSender extends Sender {
  private int latestAck = 0;
  private long latestAckTime = System.currentTimeMillis();
  private Lock lock = new ReentrantLock();
  
  public gbnSender(long timeout, String fileName) {
    super(timeout, fileName);
  }
  
  public boolean isTimedOut() {
    return (System.currentTimeMillis() - this.latestAckTime) > Sender.TIMEOUT; 
  }

  public void send() {
    
    while (this.latestAck < this.packetQueue.size() - 1) {
      lock.lock();
      try {
        int upperBound = Math.min(latestAck + 1 + CS456Packet.WINDOW_SIZE, packetQueue.size());
        for (int i = latestAck; i < upperBound; i++) {
          // Get packet from queue.
          CS456Packet packet = this.packetQueue.get(i);
          DatagramPacket datagram = packet.getDatagram(this.host, this.port);
          
          // Send to socket.
          try {
            this.socket.send(datagram);
            packet.printLog(true);
          } catch (IOException e) {}
        }
      } finally {
        lock.unlock();
      }
      
      while (true) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {}
        
        if (this.isTimedOut()) {
          break;
        }
      }
    }
    
    // Send EOT Packet
    CS456Packet packetEOT = new CS456Packet(2, this.latestAck + 1, new byte[0]);
    this.packetQueue.add(packetEOT);
    try {
      this.socket.send(packetEOT.getDatagram(this.host, this.port));
      packetEOT.printLog(true);
    } catch (IOException e) {}
  }
  
  public void listen() {
    DatagramPacket ackPacket = new DatagramPacket(new byte[512], 512);
    while (this.foundEOTPacket == false) {
      try {
        socket.receive(ackPacket);
      } catch (IOException e) {}

      lock.lock();
      try {
        CS456Packet parsedAckPacket = new CS456Packet(ackPacket.getData());
        parsedAckPacket.printLog(false);
        
        if (parsedAckPacket.isEOT()) {
          this.foundEOTPacket = true;
        } else {
          this.latestAckTime = System.currentTimeMillis();
          
          // If we wrapped over the modulo (for example the ack is 0 and we're at 255, then skip.
          if (parsedAckPacket.getSequenceNumber() < (this.latestAck%CS456Packet.SEQUENCE_MODULO) - CS456Packet.WINDOW_SIZE) {
            System.out.println("Can't support over 256 packets. Oops.");
          }
          
          this.latestAck = parsedAckPacket.getSequenceNumber();
        }
      } finally {
        lock.unlock();
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
