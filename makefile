JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
  $(JC) $(JFLAGS) $*.java

CLASSES = \
  CS456Packet.java \
  Receiver.java \
  gbnReceiver.java \
  srReceiver.java \
  Sender.java \
  gbnSender.java \
  srSender.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
  $(RM) *.class *.log