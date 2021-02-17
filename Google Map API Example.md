# Google Map API

Kakao Map API를 활용하여 kotlin으로 구현을 해보았다. 하지만 현위치를 중심으로 지도를 출력하는데 어려움이 있었다. Android 활용 샘플을 확인하였지만 지금의 실력으로는 무슨 소린지 모르겠다. JS로 활용한 예시는 훨씬 보기 좋았던 것 같다. 그래서 Google Map API를 활용하기로 결정하였다. 

# Google Map 보여주기

[Google Maps Android API 사용 방법 및 예제](https://webnautes.tistory.com/647)

## API 키 생성하기

[Get Started | Maps SDK for Android | Google Developers](https://developers.google.com/maps/documentation/android-sdk/start#None-kotlin)

[ proj name ] MapOfExhibition-305103

[ proj id ] mapofexhibition-305103

## 지도 출력하기

[Overview | Maps SDK for Android | Google Developers](https://developers.google.com/maps/documentation/android-sdk/overview)

AndroidStudio에서 java> 패키지 에서 오른쪽 마우스를 클릭한다.

New> Google> Google Maps Activity 하여 Activity를 생성한다.

생성된 google_maps_api.xml 에서 API키를 입력하고 빌드하면 된다.

# 현위치의 위도, 경도 출력하기

단, 위치 권한이 미리 켜져있어야 한다.

## 현위치로 카메라 조정하기

[GoogleMap.OnMyLocationButtonClickListener | Google APIs for Android](https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap.OnMyLocationButtonClickListener)

```java
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {
    private GoogleMap mMap;
    
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);  // 상단의 위치버튼을 클릭리스너
        mMap.setOnMyLocationClickListener(this);  // 내 위치 클릭리스너
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. 
						// Show rationale and request permission
            ActivityCompat.requestPermissions(MapsActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();

        Log.d("MapActivity", "lat("+location.getLatitude()+") lon("+location.getLongitude()+")");
    }
}
```

## 현위치의 위도, 경도 값 가져오기

[Task | Google APIs for Android | Google Developers](https://developers.google.com/android/reference/com/google/android/gms/tasks/Task)

```java
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    // 위에 코드를 추가한다.
    private Location currentLoc;

    private Boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return false;
        }
        return true;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();

        FusedLocationProviderClient locClient = new FusedLocationProviderClient(getApplicationContext());
        if (checkPermission()) {
            Task<Location> taskLoc = locClient.getLastLocation();
            currentLoc = taskLoc.getResult();
        }
        Log.d("MapsActivity", "currentLoc : "+currentLoc);

        return false;
    }
}
```