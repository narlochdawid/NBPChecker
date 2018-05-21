/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package curr;

import java.awt.*;
import javax.swing.*;
import javax.xml.parsers.*;
import java.net.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.lang.Math.sqrt;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 *
 * @author narlo
 */

public class MainFrame extends JFrame implements ActionListener{
   
    private int WINDOW_WIDTH = 450;
    private int WINDOW_HEIGHT = 300;
    private int MAX_PERIOD = 93;
    private String MIN_DATE = "2002-01-02";
    
    private JTextField currencyTxtField;
    private  JFormattedTextField startDateTxtField;
    private  JFormattedTextField endDateTxtField;
    private JLabel buyRateResultLabel;
    private JLabel deviationResultLabel;
    private JLabel errLabel;
    
    
    public MainFrame(){
        
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension dm = tk.getScreenSize();
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        this.setTitle("NBP checker");
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setLocation((dm.width-WINDOW_WIDTH)/2, (dm.height-WINDOW_HEIGHT)/2);
        //this.setResizable(false);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.BASELINE;
        
        JLabel codeLabel = new JLabel("kod waluty");
        JLabel startDateLabel = new JLabel("data początkowa");
        JLabel endDateLabel = new JLabel("data końcowa");
        JLabel buyRateLabel = new JLabel("> średni kurs");
        buyRateResultLabel = new JLabel("");
        JLabel deviationLabel = new JLabel("> odchylenie");
        deviationResultLabel = new JLabel("");
        errLabel = new JLabel("----");
        
        JLabel authorLabel = new JLabel("autor: Dawid Narloch");
        authorLabel.setFont(new Font("Times New Roman", Font.ITALIC, 10));
        
        currencyTxtField = new JTextField();
        currencyTxtField.setColumns(3);
        currencyTxtField.setToolTipText("Tutaj wpisz trzyliterowy kod waluty (np. USD, EUR, CHF, GPB)");
        
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        
        startDateTxtField = new JFormattedTextField(format);
        startDateTxtField.setColumns(10);
        startDateTxtField.setToolTipText("<html>" +
                "Tutaj wpisz datę początkową w formacie RRRR-MM-DD (np. 2017-11-20)\n" +
                "<br>Minimalna data początkowa to 2002-01-02" +
                "</html>");
        
        endDateTxtField = new JFormattedTextField(format);
        endDateTxtField.setColumns(10);
        endDateTxtField.setToolTipText("Tutaj wpisz datę końcową w formacie RRRR-MM-DD (np. 2017-11-24)");
        
        JButton process = new JButton("Wyświetl");
        process.addActionListener(this);
        process.setToolTipText("Wyswietlone zostaną: średni kurs kupna, odchylenie standardowe kursów sprzedaży");
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(codeLabel,gbc);
        gbc.gridy = 4;
        mainPanel.add(buyRateLabel,gbc);
        gbc.gridy = 1;
        mainPanel.add(currencyTxtField,gbc);
        gbc.gridy = 5;
        mainPanel.add(deviationLabel,gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        mainPanel.add(startDateLabel,gbc);
        gbc.gridx = 2;
        mainPanel.add(endDateLabel,gbc);
        gbc.gridy = 1;
        mainPanel.add(endDateTxtField,gbc);
        gbc.gridy = 6;
        mainPanel.add(authorLabel,gbc);
        gbc.gridx = 1;
        mainPanel.add(errLabel,gbc); 
        gbc.gridy = 1;
        mainPanel.add(startDateTxtField,gbc);
        gbc.gridy = 3;
        mainPanel.add(process,gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        mainPanel.add(buyRateResultLabel,gbc);
        gbc.gridy = 5;
        mainPanel.add(deviationResultLabel,gbc);
        
        this.add(mainPanel);
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    
    private void parseRequest(){

        double bidSum = 0;
        double askSum = 0;
        double bidCnt = 0;
        double askCnt = 0;
        double askRes = 0;
        double[] tmpRes;
        
        String table = "C";
        
        LocalDate start = LocalDate.parse(startDateTxtField.getText());
        LocalDate end  = LocalDate.parse(endDateTxtField.getText());
        int days = (int) DAYS.between(start, end) + 1;
        int cnt = (int) Math.ceil(days/(double) MAX_PERIOD);
        
        for(int i=0;i<cnt;i++){
            String url = "http://api.nbp.pl/api/exchangerates/rates/"+ table + "/";
            url += currencyTxtField.getText().toUpperCase()+"/"+start.toString()+"/"+endDateTxtField.getText()+"/";
            if(i != cnt-1){
                end = start.plusDays(MAX_PERIOD - 1);
                url = "http://api.nbp.pl/api/exchangerates/rates/"+ table + "/";
                url += currencyTxtField.getText().toUpperCase()+"/"+start.toString()+"/"+end.toString()+"/";
            }
            tmpRes = calculateMedianes(sendGet(url));
            if(tmpRes == null){
                displayWarning();
                return;
            }
            bidSum += tmpRes[0];
            bidCnt += tmpRes[2];
            askSum += tmpRes[1];
            askCnt += tmpRes[3];
            if(i != cnt-1){
                start = start.plusDays(MAX_PERIOD);
            }
        }
        
        start = LocalDate.parse(startDateTxtField.getText());
        
        for(int i=0;i<cnt;i++){
            String url = "http://api.nbp.pl/api/exchangerates/rates/"+table+"/";
            url += currencyTxtField.getText().toUpperCase()+"/"+start.toString()+"/"+endDateTxtField.getText()+"/";
            if(i != cnt-1){
                end = start.plusDays(MAX_PERIOD-1);
                url = "http://api.nbp.pl/api/exchangerates/rates/"+table+"/";
                url += currencyTxtField.getText().toUpperCase()+"/"+start.toString()+"/"+end.toString()+"/";
            }
            
            double returnedVal = calculateDeviation(sendGet(url),(askSum/askCnt));
            if(returnedVal == -1){
                displayWarning();
                return;
            }
            askRes += returnedVal;
            if(i != cnt-1){
                start = start.plusDays(MAX_PERIOD);
            } 
        }
        
        askRes /= askCnt;
        askRes = sqrt(askRes);
        deviationResultLabel.setText(String.format ("%.4f", askRes));
        deviationResultLabel.setToolTipText("Odchylenie standardowe kursów sprzedaży wybranej waluty w wybranym okresie "
                + "według notowań NBP");
        buyRateResultLabel.setText(String.format ("%.4f", bidSum/bidCnt));
        buyRateResultLabel.setToolTipText("Średni kurs kupna wybranej waluty w wybranym okresie według notowań NBP");
    }
    
    private double calculateDeviation(Document xml, double askMedian){
        if(xml == null)
            return -1;
        
        Element rootElement = xml.getDocumentElement();
        int askLengh = rootElement.getElementsByTagName("Ask").getLength();
        
        double askResult = 0;
        
        for(int i=0;i<askLengh;i++){
            double tmp = Double.parseDouble(rootElement.getElementsByTagName("Ask").item(i).getTextContent()) - askMedian;
            askResult += tmp*tmp;
        }
        return askResult;
    }
    
    private double[] calculateMedianes(Document xml){
        if(xml == null)
            return null;
        
        Element rootElement = xml.getDocumentElement();
        
        double bidSum=0;
        double askSum=0;
        
        int bidLengh = rootElement.getElementsByTagName("Bid").getLength();
        int askLengh = rootElement.getElementsByTagName("Ask").getLength();

        for(int i=0;i<bidLengh;i++){
            bidSum += Double.parseDouble(rootElement.getElementsByTagName("Bid").item(i).getTextContent());
        }
        for(int i=0;i<askLengh;i++){
            askSum += Double.parseDouble(rootElement.getElementsByTagName("Ask").item(i).getTextContent());
        }
        double[] res = {bidSum,askSum,bidLengh,askLengh};
        return  res;
    }
    
    private Document sendGet(String url){
        
        URL obj;
        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            
            con.setRequestMethod("GET");
            con.addRequestProperty("Accept", "application/xml");
            
            int responseCode = con.getResponseCode();
            
            if(responseCode==200){
                
                errLabel.setText("----");
                errLabel.setToolTipText("");
                StringBuilder response;
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String inputLine;
                response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                DocumentBuilderFactory fctr = DocumentBuilderFactory.newInstance();
                DocumentBuilder bldr = fctr.newDocumentBuilder();
                InputSource insrc = new InputSource(new StringReader(response.toString()));
                Document xml = bldr.parse(insrc);
                return xml;
            }
            else {
                displayWarning();
                return null;
            }
            
        } catch (MalformedURLException ex) {
            displayWarning();
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            displayWarning();
        }
        return null;
        
    }
    
    private boolean inputOK(){
        if(currencyTxtField.getText().length()==3 && startDateTxtField.getText().length()==10 && endDateTxtField.getText().length()==10){
            LocalDate start = LocalDate.parse(startDateTxtField.getText());
            LocalDate end  = LocalDate.parse(endDateTxtField.getText());
            LocalDate tommorow = LocalDate.now().plusDays(1);
            LocalDate firstPossible = LocalDate.parse(MIN_DATE);
            if((start.isBefore(end) || start.equals(end)) && end.isBefore(tommorow) && start.isAfter(firstPossible.minusDays(1)))
                return true;
        }
        return false;
    }
    
    void displayWarning(){
        errLabel.setText("Błąd zapytania");
         errLabel.setToolTipText("<html>" +
                "Błąd może wystąpić, gdy:" +
                "<br>1. Wybrano okres, w którym NBP nie dokonywał notowań" +
                "<br>kursów (np. święta, dni wolne od pracy)" +
                "<br>2. Wprowadzono niepoprawny kod waluty" +
                "<br>3. Utracono połączenie z internetem" +
                "</html>");
        buyRateResultLabel.setText("");
        deviationResultLabel.setText("");
        deviationResultLabel.setToolTipText("");
        buyRateResultLabel.setToolTipText("");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(inputOK()){
            errLabel.setText("----");
            errLabel.setToolTipText("");
            parseRequest();
        }
        else{
            errLabel.setText("Niepoprawne dane");
            errLabel.setToolTipText("<html>" +
                "Podano niepoprawne dane. Sprawdz czy:" +
                "<br>1. Wszystkie pola zostały wypełnione (kod waluty, data początkowa, data końcowa)" +
                "<br>2. Data początkowa jest pózniejsza niż 2002-01-01" +
                "<br>3. Data końcowa jest wcześniejsza lub równa obecnej dacie" +
                "<br>4. Data początkowa jest wcześniejsza niż data końcowa" +
                "<br>5. Wpisany kod waluty jest zgodny ze standardem ISO 4217" +
                "</html>");
            buyRateResultLabel.setText("");
            deviationResultLabel.setText("");
            deviationResultLabel.setToolTipText("");
            buyRateResultLabel.setToolTipText("");
            
        }
    }    
}