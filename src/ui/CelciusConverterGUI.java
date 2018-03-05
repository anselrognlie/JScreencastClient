package ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;

public class CelciusConverterGUI extends javax.swing.JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JTextField celciusTextField;
    private JLabel celciusLabel;
    private JButton convertButton;
    private JLabel farenheitLabel;

    public CelciusConverterGUI() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        setTitle("Celcius Converter");
        
        celciusTextField = new JTextField();
        celciusLabel = new JLabel("Celcius");
        convertButton = new JButton("Convert");
        farenheitLabel = new JLabel("Farenheit");
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);;   
        
        layout.setHorizontalGroup(layout.
            createSequentialGroup().
                addGroup(layout.
                    createParallelGroup().
                        addComponent(celciusTextField).
                        addComponent(convertButton)).
                addGroup(layout.
                    createParallelGroup().
                        addComponent(celciusLabel).
                        addComponent(farenheitLabel)));
        layout.setVerticalGroup(layout.
            createSequentialGroup().
                addGroup(layout.
                    createParallelGroup(Alignment.BASELINE).
                        addComponent(celciusTextField).
                        addComponent(celciusLabel)).
                addGroup(layout.
                    createParallelGroup(Alignment.BASELINE).
                        addComponent(convertButton).
                        addComponent(farenheitLabel)));
        pack();
        
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                CelciusConverterGUI.this.convertButtonClicked(arg0);
            }
        });
    }
    
    private void convertButtonClicked(ActionEvent e) {
      //Parse degrees Celsius as a double and convert to Fahrenheit.
        int tempFahr = (int)((Double.parseDouble(celciusTextField.getText()))
                * 1.8 + 32);
        farenheitLabel.setText(tempFahr + " Fahrenheit");
    }

    public static void main(String[] args) {
        JFrame frame = new CelciusConverterGUI();
        frame.setVisible(true);
    }

}
