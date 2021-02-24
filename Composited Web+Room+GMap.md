# Composited Web+Room+GMap

Created: Feb 18, 2021 6:10 PM

# 1. Web API + Room DB

## Work Flow

![Composited%20Web+Room+GMap/workflow_browExh1.png](Composited%20Web+Room+GMap/workflow_browExh1.png)

## Room DB 구성 코드

```kotlin
@Entity(tableName = "ExhFacility")
data class ExhFacility (
    @PrimaryKey(autoGenerate = true) var no: Int,
    @ColumnInfo(name = "facName") var facName: String,
    @ColumnInfo(name = "addr") var addr: String,
    @ColumnInfo(name = "xCoord") var xCoord: Double,
    @ColumnInfo(name = "yCoord") var yCoord: Double,
    @ColumnInfo(name = "homepage") var homepage: String
)
@Entity(tableName = "Exhibition")
data class Exhibition(
    @PrimaryKey(autoGenerate = true) var no: Int,
    @ColumnInfo(name = "exhName") var exhName: String,
    @ColumnInfo(name = "period") var period: String,
    @ColumnInfo(name = "website") var website: String,
    @ColumnInfo(name = "fees") var fees: String,
    @ColumnInfo(name = "closeDay") var closeDay: String,
    @ColumnInfo(name = "operHours") var operHours: String
)
@Entity(tableName = "UserOperated")
data class UserOperated(
    @PrimaryKey(autoGenerate = true) var no: Int,
    @ColumnInfo(name = "userName") var userName: String,
    @ColumnInfo(name = "exhGoing") var exhGoing: String,
    @ColumnInfo(name = "facFavor") var facFavor: String
)

@Dao
interface ExhFacilityDao {
    @Query("SELECT * FROM ExhFacility")
    fun getAll(): List<ExhFacility>
    @Insert
    fun insert(obj: ExhFacility)
    @Update
    fun update(obj: ExhFacility)
    @Delete
    fun delete(obj: ExhFacility)
}
@Dao
interface ExhibitionDao {
    @Query("SELECT * FROM Exhibition")
    fun getAll(): List<Exhibition>
    @Insert
    fun insert(obj: Exhibition)
    @Update
    fun update(obj: Exhibition)
    @Delete
    fun delete(obj: Exhibition)
}
@Dao
interface UserOperatedDao {
    @Query("SELECT * FROM UserOperated")
    fun getAll(): List<UserOperated>
    @Insert
    fun insert(obj: UserOperated)
    @Update
    fun update(obj: UserOperated)
    @Delete
    fun delete(obj: UserOperated)
}

// ExhDatabase
//   ExhFacility
//   Exhibition
//   UserOperated
@Database(entities = arrayOf(ExhFacility::class, Exhibition::class, UserOperated::class), version = 1)
abstract class ExhDatabase: RoomDatabase() {
    abstract fun ExhFacilityDao(): ExhFacilityDao
    abstract fun ExhibitionDao(): ExhibitionDao
    abstract fun UserOperatedDao(): UserOperatedDao

    companion object {
        private var INSTANCE: ExhDatabase? = null

        fun getInstance(context: Context): ExhDatabase? {
            if (INSTANCE == null) {
                synchronized(ExhDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        ExhDatabase::class.java, "rndns_exhibitbrowse.db")
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }

        fun destoryInstance() {
            INSTANCE = null
        }
    }
}
```

## Main 페이지(Activity) 코드

```kotlin
class MainActivity: AppCompatActivity() {
    lateinit var checkDB: Button
    lateinit var removeDB: Button
    var db: ExhDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        checkDB = findViewById(R.id.checkDB)
        removeDB = findViewById(R.id.removeDB)

        db = ExhDatabase.getInstance(this)
        Log.d("MA10001", ""+this)

        checkDB.setOnClickListener {
            Log.d("MA10001", ""+ (db?.isOpen))
            val intent: Intent = Intent(this, getWebAPI::class.java)
            startActivity(intent)
        }
        removeDB.setOnClickListener {
            Log.d("MA10001", ""+db)
            ExhDatabase.destoryInstance()
        }
    }
}
```

## handler Room DB on Java

Room이 kotlin으로 구현되어 있어서 그런지? Java에서 getInstance()의 context를 매개하는데 오류가 계속 발생했다.

이를 해결하기 위해 Room(kotlin)과 (Java)를 연결하기 위한 클래스(kotlin)를 만들어주었다.

```kotlin
class BridgeOfExhDB {
    var exhDB: ExhDatabase
    constructor(context: Context) {
        exhDB = ExhDatabase.getInstance(context)!!
    }
    
    fun insertExhFacs(exhfacs: ArrayList<TmpExhFac>) {
        Log.d("BE10001", ""+exhDB?.isOpen())
        Log.d("BE10001", ""+exhfacs.isEmpty())
        exhfacs.forEach {
            exhDB?.ExhFacilityDao()?.insert(
                ExhFacility(it.no, it.facname, it.addr, it.x, it.y, it.homepage))
        }
    }
    fun getExhFacList(): List<ExhFacility> {
        return exhDB?.ExhFacilityDao()?.getAll()
    }
    
    class TmpExhFac constructor(
            var no: Int,
            var facname:String,
            var addr: String,
            var x: Double,
            var y: Double,
            var homepage: String) {
        fun show(): String {
            return "$no $facname $addr $x $y $homepage"
        }
    }
}
```

## getWebAPI 페이지 코드

```java
public class getWebAPI extends Activity {
    ListView showList;
    Button getWebApi;
    Button addExhFacList;
    Button showExhFacTable;

    String API_URL = "http://openapi.seoul.go.kr:8088/sample/xml/culturalSpaceInfo/1/5/";
    Handler handler = new Handler();
    BridgeOfExhDB db;
    ArrayList<BridgeOfExhDB.TmpExhFac> exhFacs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_getwebapi);

        showList = findViewById(R.id.showList);
        getWebApi = findViewById(R.id.getWebApi);
        addExhFacList = findViewById(R.id.addExhFacList);
        showExhFacTable = findViewById(R.id.showExhFacTable);

        db = new BridgeOfExhDB(this);

        // default list를 출력한다
        ArrayList<String> defaultlist = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, defaultlist);
        showList.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        // 버튼을 클릭하면, Api data를 list에 출력한다
        getWebApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ConnectThread thread = new ConnectThread(API_URL);
                    thread.start();
                } catch (Exception e) {
                    Toast.makeText(getWebAPI.this, "Check your Internet", Toast.LENGTH_LONG);
                }
            }
        });
        addExhFacList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.insertExhFacs(exhFacs);
                Log.d("WP10001", "[insert exhfaclist to db]"+exhFacs);
            }
        });
        // 버튼을 클릭하면, Room에 저장된 exhfaclist를 보여준다
        showExhFacTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<ExhFacility> exhfaclist = db.getExhFacList();
                Log.d("WP10001", "[get exhfaclist from db]"+exhfaclist);

                ArrayAdapter<ExhFacility> adapter = new ArrayAdapter<ExhFacility>(
                        getWebAPI.this, android.R.layout.simple_list_item_1, exhfaclist);
                showList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
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
                // xpathExpression에 해당하는 Node들 가져오기
                XPathExpression expr = xpath.compile(xpathExpression);
                NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
                for (int i = 0; i < nodes.getLength(); i++) {
                    values.add(nodes.item(i).getNodeValue());
                }
            } catch (XPathExpressionException e) {
                Log.e("Error_10001", "XPathExpressionException occurred");
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
                        String fac_exp = "/culturalSpaceInfo/row/FAC_NAME/text()";
                        String addr_exp = "/culturalSpaceInfo/row/ADDR/text()";
                        String x_exp = "/culturalSpaceInfo/row/X_COORD/text()";
                        String y_exp = "/culturalSpaceInfo/row/Y_COORD/text()";
                        String hp_exp = "/culturalSpaceInfo/row/HOMEPAGE/text()";

                        // document에서 XPath 가져오기
                        ArrayList<String> nums = evaluateXPath(xml, num_exp);
                        ArrayList<String> facs = evaluateXPath(xml, fac_exp);
                        ArrayList<String> addrs = evaluateXPath(xml, addr_exp);
                        ArrayList<String> xs = evaluateXPath(xml, x_exp);
                        ArrayList<String> ys = evaluateXPath(xml, y_exp);
                        ArrayList<String> homepages = evaluateXPath(xml, hp_exp);

                        // attrs에 web api에서 가져온 데이터 추가하기
                        exhFacs = new ArrayList<BridgeOfExhDB.TmpExhFac>();

                        for (int i = 0; i < nums.size(); i++) {
                            // 문자열 깨짐 발생시, new String(nums.get(i).getBytes(), "euc-kr")
                            BridgeOfExhDB.TmpExhFac tmp = new BridgeOfExhDB.TmpExhFac(
                                    Integer.parseInt(nums.get(i)),
                                    facs.get(i),
                                    addrs.get(i),
                                    Double.parseDouble(xs.get(i)),
                                    Double.parseDouble(ys.get(i)),
                                    homepages.get(i));
                            Log.d("WP10001", "[loaded from web api] "+tmp.show());
                            exhFacs.add(tmp);
                        }

                        // 화면의 list에 출력하기
                        ArrayAdapter<BridgeOfExhDB.TmpExhFac> adapter = new ArrayAdapter<BridgeOfExhDB.TmpExhFac>(
                                getWebAPI.this, android.R.layout.simple_list_item_1, exhFacs);
                        showList.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e("Error_10001", "XPath failed", e);
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
                Log.e("Error_10001", "Exception in processing response", e);
            }
            return outputDoc;
        }
    }
}
```

## 추가 설정 코드

Web API 데이터를 가져오기 위해 AndroidManifest.xml에 다음을 추가하였다.

```xml
<manifest>
	<uses-permission android:name="android.permission.INTERNET" />
	<application
        android:usesCleartextTraffic="true">
		<!-- <activity>들 -->
	</application>
</manifest>
```

Room (kotlin)을 사용하기 위해 build.gradle (Module)에 다음을 추가하였다.

```
plugins {
	id 'kotlin-kapt'
}

dependencies {
	def room_version = "2.2.6"
  implementation "androidx.room:room-runtime:$room_version"
  annotationProcessor "androidx.room:room-compiler:$room_version"
  annotationProcessor "android.arch.persistence.room:compiler:$room_version"
  kapt "android.arch.persistence.room:compiler:$room_version"
}
```

# 2. Web + Room + Map

## [Google Map] 현위치 가져오기

[Overview | Maps SDK for Android | Google Developers](https://developers.google.com/maps/documentation/android-sdk/overview)

```java
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

		@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
            Log.e(TAG, "[saved_maps_data] "+lastKnownLocation+" "+cameraPosition);
        }
        setContentView(R.layout.activity_maps);

        Places.initialize(getApplicationContext(), String.valueOf(R.string.google_maps_key));
        placesClient = Places.createClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
            Log.e(TAG, "[saving maps_data] "+lastKnownLocation+" "+cameraPosition);
        }
        super.onSaveInstanceState(outState);
    }

		@Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());
                Log.e(TAG, "[UI info_window] "+title.getText()+" "+snippet.getText());

                return infoWindow;
            }
        });

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                            Log.e(TAG, "[set last_known_loc] "+lastKnownLocation);
                        } else {
                            Log.e(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

		private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
```

```
dependencies {
		implementation 'com.google.android.gms:play-services-maps:17.0.0'
    implementation 'com.google.android.gms:play-services-location:17.1.0'
    implementation 'com.google.android.libraries.places:places:2.4.0'
}
```

## [Google Map] 지도 위에 마크찍기

[googlemaps/android-samples](https://github.com/googlemaps/android-samples/blob/master/ApiDemos/java/app/src/gms/java/com/example/mapdemo/BasicMapDemoActivity.java)

```java
@Override
public void onMapReady(GoogleMap map) {
    this.map = map;
    this.map.addMarker(new MarkerOptions()
            .position(new LatLng(37.572426, 126.975632))
            .title("세종문화회관")
            .snippet("서울특별시 종로구 세종대로 175 (세종로) 세종문화회관 (우)03172"));

    this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
        @Override
        public View getInfoWindow(Marker arg0) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                    (FrameLayout) findViewById(R.id.map), false);

            TextView title = infoWindow.findViewById(R.id.title);
            title.setText(marker.getTitle());

            TextView snippet = infoWindow.findViewById(R.id.snippet);
            snippet.setText(marker.getSnippet());
            Log.e(TAG, "[UI info_window] "+title.getText()+" "+snippet.getText());

            return infoWindow;
        }
    });

    getLocationPermission();
    updateLocationUI();
    getDeviceLocation();
}
```

## (old ver. Source Code)

[webdbmap.zip](Composited%20Web+Room+GMap/webdbmap.zip)

![Composited%20Web+Room+GMap/gmap_cmps_workflow.png](Composited%20Web+Room+GMap/gmap_cmps_workflow.png)

인터넷과 위치를 켜둔 후에, 위의 작업흐름과 같이 테스트를 진행한다.

2- 화면을 통해 Web에서의 Open API를 통해 데이터를 가져온 것을 확인한다.

4- 화면을 통해 Room DB의 ExhFac테이블에 저장된 데이터를 확인한다.

5- DB에 저장된 데이터를 Google Map위에 마크한 것을 확인한다.

6- 현위치로 Google Map 카메라가 조정된 것을 확인한다.

```java
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
            Log.e(TAG, "[saved_maps_data] "+lastKnownLocation+" "+cameraPosition);
        }
        setContentView(R.layout.activity_maps);

        Places.initialize(getApplicationContext(), String.valueOf(R.string.google_maps_key));
        placesClient = Places.createClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
            Log.e(TAG, "[saving maps_data] "+lastKnownLocation+" "+cameraPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        // DB로 부터 데이터 가져와 찍기
        BridgeOfExhDB db = new BridgeOfExhDB(MapsActivity.this);
        List<ExhFacility> exhfaclist = db.getExhFacList();
        exhfaclist.forEach(element ->
                this.map.addMarker(new MarkerOptions()
                        .position(new LatLng(element.getXCoord(), element.getYCoord()))
                        .title(element.getFacName())
                        .snippet(" [주소]"+element.getAddr()+" [웹사이트]"+element.getHomepage())
                )
        );

        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());
                Log.e(TAG, "[UI info_window] "+title.getText()+" "+snippet.getText());

                return infoWindow;
            }
        });

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                            Log.e(TAG, "[set last_known_loc] "+lastKnownLocation);
                        } else {
                            Log.e(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {
        Toast.makeText(this, "Clicked: "+pointOfInterest.name
                +" "+pointOfInterest.placeId, Toast.LENGTH_LONG).show();
    }
}
```

```
dependencies {
    implementation 'com.google.android.gms:play-services-maps:17.0.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
    implementation 'com.google.android.gms:play-services-location:17.1.0'
    implementation 'com.google.android.libraries.places:places:2.4.0'

    def room_version = "2.2.6"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"
    annotationProcessor "android.arch.persistence.room:compiler:$room_version"
    kapt "android.arch.persistence.room:compiler:$room_version"
}
```

## 통합 소스 코드

[cmps_webdbmap.zip](Composited%20Web+Room+GMap/cmps_webdbmap.zip)

![Composited%20Web+Room+GMap/Screenshot_20210224-223956_Browse_Exhibition.jpg](Composited%20Web+Room+GMap/Screenshot_20210224-223956_Browse_Exhibition.jpg)

```kotlin
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnPoiClickListener {
    private val TAG = MapsActivity::class.java.simpleName
    private val DEFAULT_ZOOM = 15
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private val KEY_CAMERA_POSITION = "camera_position"
    private val KEY_LOCATION = "location"

    var API_URL = "http://openapi.seoul.go.kr:8088/sample/xml/culturalSpaceInfo/1/5/"
    lateinit var thread: ConnectThread

    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private var placesClient: PlacesClient? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            Log.e(TAG, "[saved_maps_data] $lastKnownLocation $cameraPosition")
        }
        setContentView(R.layout.activity_maps)

        try {
            thread = ConnectThread(API_URL, this)
            thread.start()
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "Check your Internet", Toast.LENGTH_LONG)
        }

        Places.initialize(applicationContext, R.string.google_maps_key.toString())
        placesClient = Places.createClient(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map!!.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
            Log.e(TAG, "[saving maps_data] $lastKnownLocation $cameraPosition")
        }
        super.onSaveInstanceState(outState)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onMapReady(map: GoogleMap) {
        this.map = map

        // DB로 부터 데이터 가져와 찍기
        val db: ExhDatabase? = ExhDatabase.getInstance(this)
        val exhfaclist = db?.ExhFacilityDao()?.getAll()
        exhfaclist?.forEach(Consumer { (_, facName, addr, xCoord, yCoord, homepage) ->
            this.map!!.addMarker(MarkerOptions()
                    .position(LatLng(xCoord, yCoord))
                    .title(facName)
                    .snippet(" [주소]$addr [웹사이트]$homepage")
            )
        })

        this.map!!.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
                        findViewById<View>(R.id.map) as FrameLayout, false)
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker.snippet
                Log.e(TAG, "[UI info_window] " + title.text + " " + snippet.text)
                return infoWindow
            }
        })
        locationPermission()
        updateLocationUI()
        deviceLocation()
    }

    // Set the map's camera position to the current location of the device.
    private fun deviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient!!.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    LatLng(lastKnownLocation!!.latitude,
                                            lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                        Log.e(TAG, "[set last_known_loc] $lastKnownLocation")
                    } else {
                        Log.e(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map!!.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map!!.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun locationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.size > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map!!.isMyLocationEnabled = true
                map!!.uiSettings.isMyLocationButtonEnabled = true
            } else {
                map!!.isMyLocationEnabled = false
                map!!.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                locationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        Toast.makeText(this, "Clicked: ${pointOfInterest.name} ${pointOfInterest.placeId}", Toast.LENGTH_LONG).show()
    }

    public class ConnectThread(var url_str: String, val context: Context) : Thread() {
        private val TAG = ConnectThread::class.java.simpleName
        var handler = Handler()
        lateinit var db: ExhDatabase

        @Throws(Exception::class)
        fun evaluateXPath(document: Document?, xpathExpression: String?): ArrayList<String> {
            val xpathFactory = XPathFactory.newInstance()
            val xpath = xpathFactory.newXPath()
            val values = ArrayList<String>()
            try {
                // xpathExpression에 해당하는 Node들 가져오기
                val expr = xpath.compile(xpathExpression)
                val nodes = expr.evaluate(document, XPathConstants.NODESET) as NodeList
                for (i in 0 until nodes.length) {
                    values.add(nodes.item(i).nodeValue)
                }
            } catch (e: XPathExpressionException) {
                Log.e(TAG, "XPathExpressionException occurred")
            }
            return values
        }

        override fun run() {
            // URL주소의 document 가져오기
            val xml = request(url_str)
            handler.post(Runnable {
                try {
                    // XPathExpression 작성하기
                    val num_exp = "/culturalSpaceInfo/row/NUM/text()"
                    val fac_exp = "/culturalSpaceInfo/row/FAC_NAME/text()"
                    val addr_exp = "/culturalSpaceInfo/row/ADDR/text()"
                    val x_exp = "/culturalSpaceInfo/row/X_COORD/text()"
                    val y_exp = "/culturalSpaceInfo/row/Y_COORD/text()"
                    val hp_exp = "/culturalSpaceInfo/row/HOMEPAGE/text()"

                    // document에서 XPath 가져오기
                    val nums = evaluateXPath(xml, num_exp)
                    val facs = evaluateXPath(xml, fac_exp)
                    val addrs = evaluateXPath(xml, addr_exp)
                    val xs = evaluateXPath(xml, x_exp)
                    val ys = evaluateXPath(xml, y_exp)
                    val homepages = evaluateXPath(xml, hp_exp)

                    db = ExhDatabase.getInstance(context)!!
                    Log.e(TAG, ""+db.isOpen)
                    for (i in nums.indices) {
                        var flag = db?.ExhFacilityDao()?.insert(ExhFacility(
                                nums[i].toInt(),
                                facs[i],
                                addrs[i],
                                xs[i].toDouble(),
                                ys[i].toDouble(),
                                homepages[i]
                        ))
                        Log.e(TAG, "[added exhfac from web] $flag")
                    }

                } catch (e: Exception) {
                    Log.e("Error_10001", "XPath failed", e)
                }
            })
        }

        fun request(url_str: String?): Document? {
            var outputDoc: Document? = null
            try {
                // HTTP로 URL주소 연결하기
                val url = URL(url_str)
                val conn = url.openConnection() as HttpURLConnection
                if (conn != null) {
                    conn.connectTimeout = 10000 // 10s이상이면 Timeout
                    conn.requestMethod = "GET"

                    // utf-8 인코딩으로 읽어오기
                    val isr = InputStreamReader(conn.inputStream, "utf-8")
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val builder = dbFactory.newDocumentBuilder()
                    outputDoc = builder.parse(InputSource(isr))
                    conn.disconnect()
                }
            } catch (e: IOException) {
                Log.e("Error_10001", "Exception in processing response", e)
            } catch (e: SAXException) {
                Log.e("Error_10001", "Exception in processing response", e)
            } catch (e: ParserConfigurationException) {
                Log.e("Error_10001", "Exception in processing response", e)
            }
            return outputDoc
        }
    }

}
```

## 참조 사이트

[Select Current Place and Show Details on a Map | Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial)

[Classes - Help | Kotlin](https://kotlinlang.org/docs/classes.html#secondary-constructors)