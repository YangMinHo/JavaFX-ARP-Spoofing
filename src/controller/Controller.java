package controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class Controller implements Initializable {

	@FXML
	private ListView<String> networkListView;
	
	@FXML
	private TextArea textArea;
	
	@FXML
	private Button pickButton;
	
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

	
}
