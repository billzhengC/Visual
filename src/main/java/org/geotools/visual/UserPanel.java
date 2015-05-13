package org.geotools.visual;
import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.vividsolutions.jts.geom.Coordinate;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

class UserPanel {
	JFrame anaFrame;
	JLabel vehicleLable;
	JLabel vehicleIdLabel;
	String curTripid;
	Trajectory curTraj;
	JTextPane resultText;
	
	int width = 300;
	GTFS curGTFS;

	public UserPanel() {
		curGTFS = GTFS.getGTFS(); 
		anaFrame = new JFrame("Data Analysis");
		anaFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		anaFrame.setLocation(280, 300);
		anaFrame.setSize(width, 600);
		anaFrame.requestFocus();
		anaFrame.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent event) {
				// Request the focus 
				if (!anaFrame.hasFocus()) {
					anaFrame.requestFocus();
				}
			}
		});
		anaFrame.setLayout(new BorderLayout(0,0));
		JPanel curVehiclePanel = new JPanel();
		JPanel selectPanel = new JPanel();
		JPanel dataPanel = new JPanel();
		
		// panel to show the number of active vehicles currently
		curVehiclePanel.setPreferredSize(new Dimension(width, 100));		
		vehicleLable = new JLabel("Vehicles in Service: 0");
		vehicleLable.setHorizontalAlignment(JLabel.CENTER);
		vehicleLable.setPreferredSize(new Dimension(width, 100));
		vehicleLable.setFont(new Font("Serif", Font.PLAIN, 25));
		vehicleLable.setForeground(Color.GREEN);
		curVehiclePanel.add(vehicleLable);
		
		
		// panel for selecting the vehicle
		JLabel promptLabel = new JLabel("Selected Vehicle Id:");
		promptLabel.setFont(new Font("Serif", Font.PLAIN, 20));
		vehicleIdLabel = new JLabel();
		vehicleIdLabel.setFont(new Font("Serif", Font.ITALIC|Font.BOLD, 18));
		selectPanel.setPreferredSize(new Dimension(width,150));
		//selectPanel.setLayout(new GridLayout(2,0));
		selectPanel.add(promptLabel);
		selectPanel.add(vehicleIdLabel);
		
		// panel to display analysis data of the chosen vehicle
		dataPanel.setPreferredSize(new Dimension(width, 350));
		resultText = new JTextPane();
		resultText.setEditable(false);  // set textArea non-editable
		JScrollPane resultScroll = new JScrollPane(resultText);
		resultScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		resultScroll.setPreferredSize(new Dimension(width, 350));
//		JLabel resultLabel = new JLabel();
//		resultLabel.setHorizontalAlignment(JTextField.CENTER);
//		resultLabel.setFont(new Font("Serif", Font.PLAIN, 20));
//		resultLabel.setPreferredSize(new Dimension(width,350));
//		resultLabel.setText("Radius = 5km. City Covered: 50%");
		dataPanel.add(resultScroll);
		
		anaFrame.add(curVehiclePanel,BorderLayout.NORTH);
		anaFrame.add(selectPanel,BorderLayout.WEST);
		anaFrame.add(dataPanel,BorderLayout.SOUTH);
	}

	public void start() {
		anaFrame.setVisible(true);
	}
	
	public void update() {
		curTripid = "B20141207WKD_070850_M..N20R";
		vehicleIdLabel.setText(curTripid);
		curTraj = GTFSParser.trajMap.get(curTripid);;
		resultText.setText("");
		StyledDocument doc = resultText.getStyledDocument();
	
		//  Define a keyword attribute
		SimpleAttributeSet keyWord = new SimpleAttributeSet();
		StyleConstants.setForeground(keyWord, Color.RED);
		StyleConstants.setBold(keyWord, true);
		resultText.setText("");
		for (Map.Entry<Long, String> entry:curTraj.trajectoryWithName.entrySet()) {
			String append = curGTFS.toStandardTime(entry.getKey());
			append += " " + entry.getValue() + "\n";
			try
			{
			    doc.insertString(doc.getLength(), append, keyWord );
			}
			catch(Exception e) { System.out.println(e); }
		}
	
		vehicleLable.setText("Vehicles in Service: " + curGTFS.vehiclesInService);
	}
}
