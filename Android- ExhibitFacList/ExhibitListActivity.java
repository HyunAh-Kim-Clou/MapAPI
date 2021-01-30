package com.rndns.currentlocmapkt;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class ExhibitListActivity extends Activity {
    String URLStr = "http://openapi.seoul.go.kr:8088/6864435155726e64313131695366554f/xml/culturalSpaceInfo/1/5/";
    Handler handler = new Handler();
    ListView showDB;
    Button getUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_exhibitlist);

        ArrayList<String> defaultlist = new ArrayList<String>();

        // ListView에 표시
        showDB = findViewById(R.id.showDB);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, defaultlist);
        showDB.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        // URL data 가져오기
        getUrl = findViewById(R.id.getUrl);
        getUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectThread thread = new ConnectThread(URLStr);
                thread.start();
            }
        });
    }

    class ConnectThread extends Thread {
        String url_str;
        public ConnectThread(String url) {
            this.url_str = url;
        }
        public ArrayList<String> evaluateXPath(Document document, String xpathExpression) throws Exception {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            ArrayList<String> values = new ArrayList<>();
            try {
                XPathExpression expr = xpath.compile(xpathExpression);
                NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); i++) {
                    values.add(nodes.item(i).getNodeValue());
                }
            } catch (XPathExpressionException e) {
                Log.e("Error_evaluateXPath", "XPathExpressionException occurred");
            }
            return values;
        }
        public void run() {
            Document xml = request(url_str);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        String num_exp = "/culturalSpaceInfo/row/NUM/text()";
                        String subjcode_exp = "/culturalSpaceInfo/row/SUBJCODE/text()";
                        String fac_exp = "/culturalSpaceInfo/row/FAC_NAME/text()";
                        String addr_exp = "/culturalSpaceInfo/row/ADDR/text()";
                        String x_exp = "/culturalSpaceInfo/row/X_COORD/text()";
                        String y_exp = "/culturalSpaceInfo/row/Y_COORD/text()";
                        ArrayList<String> nums = evaluateXPath(xml, num_exp);
                        ArrayList<String> subjs = evaluateXPath(xml, subjcode_exp);
                        ArrayList<String> facs = evaluateXPath(xml, fac_exp);
                        ArrayList<String> addrs = evaluateXPath(xml, addr_exp);
                        ArrayList<String> xs = evaluateXPath(xml, x_exp);
                        ArrayList<String> ys = evaluateXPath(xml, y_exp);

                        ArrayList<String[]> attrs = new ArrayList<String[]>();
                        for (int i = 0; i < nums.size(); i++) {
                            // new String(nums.get(i).getBytes(), "euc-kr")
                            String[] attr = {
                                    nums.get(i),
                                    subjs.get(i),
                                    facs.get(i),
                                    addrs.get(i),
                                    xs.get(i),
                                    ys.get(i)
                            };
                            System.out.println(" - col["+i+"]: "+attr[0]+attr[1]+attr[2]+attr[3]+attr[4]+attr[5]);
                            attrs.add(attr);
                        }

                        // ListView 바꾸기
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, facs);
                        showDB.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e("Error_pasing", "XPath failed", e);
                    }
                }
            });
        }
        public Document request(String url_str) {
            Document outputDoc = null;
            try {
                URL url = new URL(url_str);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");

                    InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "utf-8");
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = dbFactory.newDocumentBuilder();
                    outputDoc = builder.parse(new InputSource(isr));

                    conn.disconnect();
                }
            } catch (IOException | SAXException | ParserConfigurationException e) {
                Log.e("Error_Http", "Exception in processing response", e);
            }
            return outputDoc;
        }
    }
}
