package controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;
import org.jnetpcap.packet.JRegistry;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import model.ARP;
import model.Util;

public class Controller implements Initializable {

	@FXML
	private ListView<String> networkListView;
	
	@FXML
	private TextArea textArea;
	
	@FXML
	private Button pickButton;
	
	@FXML
	private TextField myIP;
	
	@FXML
	private TextField senderIP;
	
	@FXML
	private TextField targetIP;

	@FXML
	private Button getMACButton;
	
	ObservableList<String> networkList = FXCollections.observableArrayList();
	
	private ArrayList<PcapIf> allDevs = null;
	
    @Override
    public void initialize(URL location, ResourceBundle resources) {
		allDevs = new ArrayList<PcapIf>();
		StringBuilder errbuf = new StringBuilder();
		int r = Pcap.findAllDevs(allDevs, errbuf);
		if (r == Pcap.NOT_OK || allDevs.isEmpty()) {
			textArea.appendText("��Ʈ��ũ ��ġ�� ã�� �� �����ϴ�.\n" + errbuf.toString() + "\n");
			return;
		}
		textArea.appendText("��Ʈ��ũ ��ġ�� ã�ҽ��ϴ�.\n���Ͻô� ��ġ�� �������ּ���.\n");
		for (PcapIf device : allDevs) {
			networkList.add(device.getName() + " " +
					((device.getDescription() != null) ? device.getDescription() : "No Description"));
		}
    	networkListView.setItems(networkList);
    }
    
    public void networkPickAction() {
    	if(networkListView.getSelectionModel().getSelectedIndex() < 0) {
    		return;
    	}
		Main.device = allDevs.get(networkListView.getSelectionModel().getSelectedIndex());
		networkListView.setDisable(true);
		pickButton.setDisable(true);

		int snaplen = 64 * 1024;
		int flags = Pcap.MODE_PROMISCUOUS;
		int timeout = 1;

		StringBuilder errbuf = new StringBuilder();
		Main.pcap = Pcap.openLive(Main.device.getName(), snaplen, flags, timeout, errbuf);

		if (Main.pcap == null) {
			textArea.appendText("��Ʈ��ũ ��ġ�� �� �� �����ϴ�.\n" + errbuf.toString() + "\n");
			return;
		}
		textArea.appendText("��ġ ����: " + Main.device.getName() + "\n");
		textArea.appendText("��Ʈ��ũ ��ġ�� Ȱ��ȭ�߽��ϴ�.\n");
    }

    public void getMACAction() {
    	if(!pickButton.isDisable()) {
    		textArea.appendText("��Ʈ��ũ ��ġ�� ���� �������ּ���.\n");
    		return;
    	}
    	
    	ARP arp = new ARP();
		Ethernet eth = new Ethernet();
		PcapHeader header = new PcapHeader(JMemory.POINTER);
		JBuffer buf = new JBuffer(JMemory.POINTER);
		ByteBuffer buffer = null;
		
		int id = JRegistry.mapDLTToId(Main.pcap.datalink());
		try {
			Main.myMAC = Main.device.getHardwareAddress();
			Main.myIP = InetAddress.getByName(myIP.getText()).getAddress();
			Main.senderIP = InetAddress.getByName(senderIP.getText()).getAddress();
			Main.targetIP = InetAddress.getByName(targetIP.getText()).getAddress();
		} catch (IOException e) {
			textArea.appendText("IP �ּҰ� �߸��Ǿ����ϴ�.\n");
		}
	
		myIP.setDisable(true);
		senderIP.setDisable(true);
		targetIP.setDisable(true);
		getMACButton.setDisable(true);
		
		arp = new ARP();
		arp.makeARPRequest(Main.myMAC, Main.myIP, Main.targetIP);
		buffer = ByteBuffer.wrap(arp.getPacket());
		if (Main.pcap.sendPacket(buffer) != Pcap.OK) {
			System.out.println(Main.pcap.getErr());
		}
		textArea.appendText("Ÿ�ٿ��� APR Request�� ���½��ϴ�.\n" +
				Util.bytesToString(arp.getPacket()) + "\n");
		
		Main.targetMAC = new byte[6];
		while (Main.pcap.nextEx(header, buf) == Pcap.NEXT_EX_OK) {
			PcapPacket packet = new PcapPacket(header, buf);
			packet.scan(id);
			byte[] sourceIP = new byte[4];
			System.arraycopy(packet.getByteArray(0, packet.size()), 28, sourceIP, 0, 4);
			if(packet.getByte(12) == 0x08 && packet.getByte(13) == 0x06
						&& packet.getByte(20) == 0x00 && packet.getByte(21) == 0x02
						&& Util.bytesToString(sourceIP).equals(Util.bytesToString(Main.targetIP))
						&& packet.hasHeader(eth)) {
				Main.targetMAC = eth.source();
				break;
			} else {
				continue;
			}
		}
		
		textArea.appendText("Ÿ�� �� �ּ�: " +
				Util.bytesToString(Main.targetMAC) + "\n");
		
		arp = new ARP();
		arp.makeARPRequest(Main.myMAC, Main.myIP, Main.senderIP);
		buffer = ByteBuffer.wrap(arp.getPacket());
		if (Main.pcap.sendPacket(buffer) != Pcap.OK) {
			System.out.println(Main.pcap.getErr());
		}
		textArea.appendText("�������� APR Request�� ���½��ϴ�.\n" +
				Util.bytesToString(arp.getPacket()) + "\n");
		
		Main.senderMAC = new byte[6];
		while (Main.pcap.nextEx(header, buf) == Pcap.NEXT_EX_OK) {
			PcapPacket packet = new PcapPacket(header, buf);
			packet.scan(id);
			byte[] sourceIP = new byte[4];
			System.arraycopy(packet.getByteArray(0, packet.size()), 28, sourceIP, 0, 4);
			if(packet.getByte(12) == 0x08 && packet.getByte(13) == 0x06
						&& packet.getByte(20) == 0x00 && packet.getByte(21) == 0x02
						&& Util.bytesToString(sourceIP).equals(Util.bytesToString(Main.senderIP))
						&& packet.hasHeader(eth)) {
				Main.senderMAC = eth.source();
				break;
			} else {
				continue;
			}
		}
		textArea.appendText("���� �� �ּ�: " +
				Util.bytesToString(Main.senderMAC) + "\n");
    }
    
}

