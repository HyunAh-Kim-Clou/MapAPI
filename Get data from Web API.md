# Get data from Web API

Created: Feb 18, 2021 7:27 PM

아래의 Open API를 가져와 출력해보겠습니다.

[](http://data.seoul.go.kr/dataList/OA-15487/S/1/datasetView.do)

# 출력결과 화면

![Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/Screenshot_20210218-200654_Browse_Exhibition.jpg](Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/Screenshot_20210218-200654_Browse_Exhibition.jpg)

첫 화면

![Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/Screenshot_20210218-200658_Browse_Exhibition.jpg](Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/Screenshot_20210218-200658_Browse_Exhibition.jpg)

'API DATA 가져오기' 버튼 클릭 후 화면

# Source Code

[AndroidManifest.xml](Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/AndroidManifest.xml)

[getWebAPI.java](Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/getWebAPI.java)

[layout_getwebapi.xml](Get%20data%20from%20Web%20API%204e61947152c64179856813d7c3d9bae4/layout_getwebapi.xml)

# Source Code 설명하기

화면은 다음과 같이 구성되었습니다.

- ListView : Data를 List형태로 출력한다
- Button : 클릭을 통해 Web으로부터 데이터를 가져온다

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" 
    android:layout_height="match_parent">

    <ListView
        android:id="@+id/showList"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#FFFFFF"/>
    <Button
        android:id="@+id/getWebApi"
        android:text="API data 가져오기"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_centerHorizontal="true"
        android:layout_alignParentBottom="true"/>

</RelativeLayout>
```

첫화면에는 아직 데이터를 가져오지 않아 빈 List를 확인할 수 있습니다.

다음 코드를 통해 onCreate에 Default List를 설정합니다.

```java
ArrayList<String> defaultlist = new ArrayList<String>();
ArrayAdapter<String> adapter = new ArrayAdapter<String>(
		getApplicationContext(), android.R.layout.simple_list_item_1, defaultlist);
showList.setAdapter(adapter);
adapter.notifyDataSetChanged();
```

Web으로부터 데이터를 가져오기 위해 ConnectThread를 사용하였습니다.

ConnectThread는 직접 정의한 클래스인데 코드는 다음과 같습니다.

```java
Handler handler = new Handler();

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
            // xpathExpression에 해당하는 Node들 가져오기
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
        // URL주소의 document 가져오기
        Document xml = request(url_str);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    // XPathExpression 작성하기
                    String num_exp = "/culturalSpaceInfo/row/NUM/text()";
                    String subjcode_exp = "/culturalSpaceInfo/row/SUBJCODE/text()";
                    String fac_exp = "/culturalSpaceInfo/row/FAC_NAME/text()";
                    String addr_exp = "/culturalSpaceInfo/row/ADDR/text()";
                    String x_exp = "/culturalSpaceInfo/row/X_COORD/text()";
                    String y_exp = "/culturalSpaceInfo/row/Y_COORD/text()";

                    // document에서 XPath 가져오기
                    ArrayList<String> nums = evaluateXPath(xml, num_exp);
                    ArrayList<String> subjs = evaluateXPath(xml, subjcode_exp);
                    ArrayList<String> facs = evaluateXPath(xml, fac_exp);
                    ArrayList<String> addrs = evaluateXPath(xml, addr_exp);
                    ArrayList<String> xs = evaluateXPath(xml, x_exp);
                    ArrayList<String> ys = evaluateXPath(xml, y_exp);

                    // attrs에 web api에서 가져온 데이터 추가하기
                    ArrayList<String[]> attrs = new ArrayList<String[]>();
                    for (int i = 0; i < nums.size(); i++) {
                        // 문자열 깨짐 발생시, new String(nums.get(i).getBytes(), "euc-kr")
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

                    // 화면의 list에 출력하기
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, facs);
                    showList.setAdapter(adapter);
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
            // HTTP로 URL주소 연결하기
            URL url = new URL(url_str);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            if (conn != null) {
                conn.setConnectTimeout(10000);  // 10s이상이면 Timeout
                conn.setRequestMethod("GET");

                // utf-8 인코딩으로 읽어오기
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
```

다음 코드를 통해 onCreate에 버튼 리스너를 설정합니다.

```java
String API_URL = "http://openapi.seoul.go.kr:8088/sample/xml/culturalSpaceInfo/1/5/";

getWebApi.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        ConnectThread thread = new ConnectThread(API_URL);
        thread.start();
    }
});
```

마지막으로 필요한 Permission 및 속성을 추가합니다.

해당 API는 Web URL을 통해 데이터를 가져오므로 Internet Permission이 필요합니다.

[안드로이드 http 프로토콜 접속 시 예외발생 조치 (ERR CLEARTEXT NOT PERMITTED)](https://developside.tistory.com/85)

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.BrowseExhibition"
    android:usesCleartextTraffic="true">
```