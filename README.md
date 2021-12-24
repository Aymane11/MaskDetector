## **Rapport technique du projet Android Mobile<br>Détection du port du masque**

Réalisé par : [Othmane OUTAMA](https://github.com/outama-othmane) - [Haitham OUMERZOUG](https://github.com/HaithamOumerzoug) - [Aymane BOUMAAZA](https://github.com/Aymane11)

## Objectif du projet

La pandémie du COVID-19 a rapidement affecté notre vie quotidienne en perturbant le commerce et les mouvements mondiaux. Le port d'un masque est devenu la nouvelle norme, de nombreux services publics demandent à leurs clients de porter leur masque. Par conséquent, la détection des masques est devenue une tâche cruciale pour aider la société.

Notre projet présente une solution pour atteindre cet objectif en utilisant un modèle Deep Learning. En plus, l'application offre la liste des centres de vaccination présentes dans chaque ville du Maroc, avec la possibilité de recherche par ville via une barre de recherche, avec une vue sur carte contenant les centres de vaccination.

## Arborescence du projet

```
MaskDetector
|__app
   |__src
        |__main
           |__assets
           |   |____database
           |__java
           |   |____com.maskdetector
           |       |____adapter
           |       |____database
           |       |   |____models
           |       |   |____repository
           |       |____detection
           |       |   |____helpers
           |       |____fragments
           |       |____listeners
           |       |____utils
           |__ml
           |__res
               |____drawable
               |____layout
               |____menu
               |____mipmap-anydpi-v26
               |____...
```

- **`assets/database`**: Dossier contenant les bases de données des centres de vaccination format **JSON**.
- **`res`**: Dossier contenant les fichiers XML des différents resources de l'application.
- **`ml`**: Dossier contenant le modèle **Deep Learning** qui permet de détecter le port du masque facial.
- **`com.maskdetector/fragments`**: Dossier contenant les fragments présents dans l'application (Fragment de la détection du masque **(caméra)**, de la liste des centres et celui de la carte **(Google Maps)**).
- **`com.maskdetector/adapter`**: Dossier contenant les classes Adapters utilisés dans les fragments **(Liste des centres)**.
- **`com.maskdetector/listeners`**: Dossier contenant les classes Listeners utilisés dans les fragments **(Navigation entre les fragments)**.
- **`com.maskdetector/database`**: Dossier contenant les entities et les classes DAO utilisés dans l'application **(DAO des centres de vaccination)**.
- **`com.maskdetector/detection/helpers`**: Dossier contenant les classes utilitaires utilisées pour la détection du masque, **(Conversion de modèles couleurs)**.
- **`com.maskdetector/utils`**: Dossier contenant des classes utilitaires générales.

## Screenshots

### 1- Main activity

> L'activité principale de l'application.

```java
public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationViewListener(this));
        bottomNavigationView.setSelectedItemId(R.id.maskCheckMenu);
    }
}
```

### 2- Détection du masque

> Dans cette interface, on détecte par la caméra si l'utilisateur porte le masque facial ou pas.

<table>
    <tr>
        <td>
            <div>L'utilisateur porte le masque</div>
			<img src="https://lh3.googleusercontent.com/bmRAX4UmGEUrL7SaZrvQ9tUjlBuM4l9KG2OyAFvCF6xvRUdnXbZQ_2TZs9yG3NZVsQa5CkVtDCjsU6jpbs1N=w960-h936-rw" width="" height="500" />
        </td>
        <td>
            <div>L'utilisateur ne porte pas de masque</div>
			<img src="https://lh4.googleusercontent.com/Qw4WudxYGbDn2dZWnZRHEGDGACxrJWmR9LXjA2QcbysSoIBNbJCFQWwhvxq0omyhVsMO780H0oNxdCbMsLum=w1920-h938-rw" width="" height="500" />
        </td>
    </tr>
</table>

#### Classes utilisées

- **`com.maskdetector.fragments/MaskDetector`** : Fragment de détection du masque.
- **Fonctionnement**: Cette classe vérifie les permission de la caméra, intialise la caméra et le modèle de détection, puis lance la détection du masque en temps réél.

```java
public class MaskDetector extends Fragment {

   private static final String TAG = "FACE_MASK_DETECTOR";
   private static final Integer REQUEST_CODE_PERMISSIONS = 0x98;
   private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
   private static final Double RATIO_4_3_VALUE = 4.0 / 3.0;
   private static final Double RATIO_16_9_VALUE = 16.0 / 9.0;
   private static final int TF_NUM_THREADS = 5;

   private Preview preview = null;
   private ImageAnalysis imageAnalyzer = null;
   private ProcessCameraProvider cameraProvider = null;

   private Integer lensFacing = CameraSelector.LENS_FACING_BACK;

   private Camera camera = null;

   private ExecutorService cameraExecutor;

   private FrameLayout maskDetectorFrameLayout;
   private PreviewView previewView;
   private FloatingActionButton cameraSwitcher;
   private TextView detectionTxtOutput;

   private FaceMaskDetection faceMaskDetection;

   private ActivityResultContracts.RequestMultiplePermissions requestMultiplePermissions;
   private ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher;

   public MaskDetector() {}

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_mask_detector, container, false);

      setupViewAttributes(root);
      setupActivityResult();

      setupML();
      setupCameraThread();
      setupCameraControllers();
      requireCameraPermission();

      return root;
   }

   private void setupActivityResult() {
      requestMultiplePermissions = new ActivityResultContracts.RequestMultiplePermissions();
      multiplePermissionActivityResultLauncher = registerForActivityResult(requestMultiplePermissions, this::grantedCameraPermission);
   }

   private void setupViewAttributes(ViewGroup root) {
      maskDetectorFrameLayout = root.findViewById(R.id.mask_detector_frame_layout);
      previewView = root.findViewById(R.id.preview_view);
      cameraSwitcher = root.findViewById(R.id.camera_switcher);
      detectionTxtOutput = root.findViewById(R.id.detection_txt_output);
   }

   private void setupML() {
      Model.Options options = new Model.Options.Builder()
          .setDevice(Model.Device.GPU)
          .setNumThreads(TF_NUM_THREADS)
          .build();
      try {
         faceMaskDetection = FaceMaskDetection.newInstance(requireContext(), options);
      } catch (IOException exception) {
         Log.e(TAG, "Could not load the tensorflow-lite model.", exception);
      }
   }

   @SuppressLint("UseCompatLoadingForDrawables")
   private void setupMLOutput(Bitmap bitmap) {
      TensorImage tfImage = TensorImage.fromBitmap(bitmap);
      FaceMaskDetection.Outputs result = faceMaskDetection.process(tfImage);
      List<Category> output = result.getProbabilityAsCategoryList();

      new Handler(Looper.getMainLooper()).post(() -> getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
         int supIndex = output.get(0).getScore() > output.get(1).getScore() ? 0 : 1;

         boolean isMaskOn = output.get(supIndex)
             .getLabel()
             .equals("with_mask");
         float score = output.get(supIndex).getScore();

         String message = String.valueOf(isMaskOn ?
             requireContext().getText(R.string.label_with_mask) :
             requireContext().getText(R.string.label_without_mask));
         message += " - " + Double.valueOf(score * 100).intValue() + "%";
         detectionTxtOutput.setText(message);

         int color = isMaskOn ?
             R.color.blue_400 :
             R.color.red_600;
         detectionTxtOutput.setTextColor(requireContext().getColor(color));

         int border = isMaskOn ?
             R.drawable.with_mask_border :
             R.drawable.without_mask_border;
         maskDetectorFrameLayout.setBackground(requireContext().getDrawable(border));
      }));
   }

   private void setupCameraThread() {
      cameraExecutor = Executors.newSingleThreadExecutor();
   }

   private void setupCameraControllers() {
      setLensButtonIcon();
      cameraSwitcher.setOnClickListener(it -> {
         lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT ?
             CameraSelector.LENS_FACING_BACK :
             CameraSelector.LENS_FACING_FRONT;

         setLensButtonIcon();
         setupCameraUseCases();
      });

      try {
         cameraSwitcher.setEnabled(hasBackCamera() && hasFrontCamera());
      } catch (CameraInfoUnavailableException exception) {
         cameraSwitcher.setEnabled(false);
      }
   }

   private void setupCameraUseCases() {
      CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

      DisplayMetrics metrics = new DisplayMetrics();
      previewView.getDisplay().getRealMetrics(metrics);

      int rotation = previewView.getDisplay().getRotation();
      int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);

      preview = new Preview.Builder()
          .setTargetAspectRatio(screenAspectRatio)
          .setTargetRotation(rotation)
          .build();

      imageAnalyzer = new ImageAnalysis.Builder()
          .setTargetAspectRatio(screenAspectRatio)
          .setTargetRotation(rotation)
          .build();
      imageAnalyzer.setAnalyzer(cameraExecutor, new BitmapOutputAnalysis(getContext()));

      if (cameraProvider != null) {
         cameraProvider.unbindAll();

         try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
            preview.setSurfaceProvider(previewView.createSurfaceProvider());

         } catch (Exception exception) {
            Log.e(TAG, "Use case binding failure.", exception);
         }
      }
   }

   private void requireCameraPermission() {
      if (!checkIfAllPermissionsGranted()) {
         multiplePermissionActivityResultLauncher.launch(REQUIRED_PERMISSIONS);
      } else {
         setupCamera();
      }
   }

   private void grantedCameraPermission(Map<String, Boolean> isGranted) {
      if (isGranted.containsValue(false)) {
         Snackbar.make(
             requireView(),
             requireContext().getString(R.string.permissions_not_granted_snackbar),
             Snackbar.LENGTH_LONG
         )
             .setAction(R.string.grant_permission_action, v -> {
                requireCameraPermission();
             })
             .show();

         Log.i(TAG, "Permissions are not granted.");
      } else {
         setupCamera();
      }
   }

   private void setupCamera() {
      ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
          ProcessCameraProvider.getInstance(requireContext());
      cameraProviderFuture.addListener(() -> {
         try {
            cameraProvider = cameraProviderFuture.get();
            if (hasFrontCamera()) {
               lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else if (hasBackCamera()) {
               lensFacing = CameraSelector.LENS_FACING_BACK;
            } else {
               throw new IllegalStateException("No cameras are available.");
            }

            setupCameraControllers();
            setupCameraUseCases();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }, ContextCompat.getMainExecutor(requireContext()));
   }

   private void setLensButtonIcon() {
      int icon = lensFacing == CameraSelector.LENS_FACING_FRONT ?
          R.drawable.ic_baseline_camera_rear_24 :
          R.drawable.ic_baseline_camera_front_24;

      cameraSwitcher.setImageDrawable(AppCompatResources.getDrawable(requireContext(), icon));
   }

   private Boolean checkIfAllPermissionsGranted() {
      for (String permission : REQUIRED_PERMISSIONS) {
         if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            return false;
         }
      }

      return true;
   }

   private Boolean hasFrontCamera() throws CameraInfoUnavailableException {
      if (cameraProvider == null) {
         return false;
      }

      return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
   }

   private Boolean hasBackCamera() throws CameraInfoUnavailableException {
      if (cameraProvider == null) {
         return false;
      }

      return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
   }

   @Override
   public void onConfigurationChanged(@NonNull Configuration newConfig) {
      super.onConfigurationChanged(newConfig);

      setupCameraControllers();
   }

   private Integer aspectRatio(Integer width, Integer height) {
      Double previewRation = Integer.valueOf(Math.max(width, height)).doubleValue() / Math.min(width, height);

      if (Math.abs(previewRation - RATIO_4_3_VALUE) <= Math.abs(previewRation - RATIO_16_9_VALUE)) {
         return AspectRatio.RATIO_4_3;
      }

      return AspectRatio.RATIO_16_9;
   }

   private class BitmapOutputAnalysis implements ImageAnalysis.Analyzer {
      private final Context context;
      private final YuvToRgbConverter yuvToRgbConverter;

      private Bitmap bitmapBuffer;
      private Matrix rotationMatrix;

      BitmapOutputAnalysis(Context context) {
         this.context = context;
         yuvToRgbConverter = new YuvToRgbConverter(context);
      }

      @Override
      public void analyze(@NonNull ImageProxy imageProxy) {
         try {
            Bitmap bitmap = toBitmap(imageProxy);
            setupMLOutput(bitmap);
         } catch (Exception exception) {
            Log.e(TAG, "An error occurred within the bitmap output analysis.", exception);
         } finally {
            imageProxy.close();
         }
      }

      @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
      private Bitmap toBitmap(ImageProxy imageProxy) throws Exception {

         if (bitmapBuffer == null) {
            rotationMatrix = new Matrix();
            rotationMatrix.postRotate(Integer.valueOf(imageProxy.getImageInfo().getRotationDegrees()).floatValue());
            bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
         }

         yuvToRgbConverter.yuvToRgb(imageProxy.getImage(), bitmapBuffer);

         return Bitmap.createBitmap(bitmapBuffer,0,0,bitmapBuffer.getWidth(),bitmapBuffer.getHeight(),rotationMatrix,false);
      }
   }
}
```

- **`com.maskdetector.detection/YuvToRgbConverter`** : Convertion du modèle de couleurs YUV en RGB.

```java
public final class YuvToRgbConverter {
   private final RenderScript renderScript;
   private final ScriptIntrinsicYuvToRGB scriptIntrinsicYuvToRGB;

   private ByteBuffer yuvBuffer;
   private Allocation inputAllocation;
   private Allocation outputAllocation;
   private Integer pixelCount = -1;

   public YuvToRgbConverter(Context context) {
      this.renderScript = RenderScript.create(context);
      this.scriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript));
   }

   public synchronized void yuvToRgb(Image image, Bitmap outputBitmap) throws Exception {
      if (yuvBuffer == null) {
         pixelCount = image.getCropRect().width() * image.getCropRect().height();
         int pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888);
         yuvBuffer = ByteBuffer.allocateDirect(pixelCount * pixelSizeBits / 8);
      }

      yuvBuffer.rewind();
      imageToByteBuffer(image, yuvBuffer.array());

      if (inputAllocation == null) {
         Type elemType = new Type.Builder(renderScript, Element.YUV(renderScript))
             .setYuvFormat(ImageFormat.NV21)
             .create();
         inputAllocation = Allocation.createSized(renderScript,
             elemType.getElement(),
             yuvBuffer.array().length
         );
      }

      if (outputAllocation == null) {
         outputAllocation = Allocation.createFromBitmap(renderScript, outputBitmap);
      }

      inputAllocation.copyFrom(yuvBuffer.array());
      scriptIntrinsicYuvToRGB.setInput(inputAllocation);
      scriptIntrinsicYuvToRGB.forEach(outputAllocation);
      outputAllocation.copyTo(outputBitmap);
   }

   private void imageToByteBuffer(Image image, byte[] outputBuffer) throws Exception {
      if (BuildConfig.DEBUG && image.getFormat() != ImageFormat.YUV_420_888) {
         throw new Exception("Assertion Failure");
      }

      Rect imageCrop = image.getCropRect();
      Image.Plane[] imagePlanes = image.getPlanes();
      for (int planeIndex = 0; planeIndex < imagePlanes.length; planeIndex++) {
         int outputStride;
         int outputOffset;

         switch (planeIndex) {
            case 0:
               outputStride = 1;
               outputOffset = 0;
               break;
            case 1:
               outputStride = 2;
               outputOffset = pixelCount + 1;
               break;
            case 2:
               outputStride = 2;
               outputOffset = pixelCount;
               break;
            default:
               continue;
         }

         ByteBuffer planeBuffer = imagePlanes[planeIndex].getBuffer();
         int rowStride = imagePlanes[planeIndex].getRowStride();
         int pixelStride = imagePlanes[planeIndex].getPixelStride();
         Rect planeCrop = planeIndex == 0 ?
             imageCrop :
             new Rect(imageCrop.left / 2,imageCrop.top / 2,imageCrop.right / 2,imageCrop.bottom / 2);
         int planeWidth = planeCrop.width();
         int planeHeight = planeCrop.height();
         byte[] rowBuffer = new byte[rowStride];
         int rowLength = (pixelStride == 1 && outputStride == 1) ?
             planeWidth :
             (planeWidth - 1) * pixelStride + 1;

         for (int row = 0; row < planeHeight; row++) {
            planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
            );

            if (pixelStride == 1 && outputStride == 1) {
               planeBuffer.get(outputBuffer, outputOffset, rowLength);
               outputOffset += rowLength;
            } else {
               planeBuffer.get(rowBuffer, 0, rowLength);
               for (int col = 0; col < planeWidth; col++) {
                  outputBuffer[outputOffset] = rowBuffer[col * pixelStride];
                  outputOffset += outputStride;
               }
            }
         }
      }
   }
}
```

### 3- Les centres de vaccination

> Cette interface liste l'ensmble de villes avec le nombre de centres de vaccination dans chaque ville, avec une barre de recherche pour filtrer les villes.

<table>
    <tr>
        <td>
            <div>La liste de villes</div>
			<img src="https://lh5.googleusercontent.com/zF7lfgEOhvlQ40ZGjNAcgd7z8DTq7PPfUpgOJF2SW7DPTpzT_xrjGsE7lbevsOyC7S7gJ69HIOOe-bNlkl3_=w1920-h579-rw" width="" height="500" />
        </td>
        <td>
            <div>Exemple d'utilisation de la recherche</div>
			<img src="https://lh3.googleusercontent.com/c0rimfiMMewhYGA8dfs_NXYgwKDtrhGWlgb07icMrhiibiDiQPXs_YHofbiFY2MEGBYCIBGtkir1SFtFMpQ8=w1920-h938-rw" width="" height="500" />
        </td>
    </tr>
</table>

#### Classes utilisées

- **`com.maskdetector.fragments/VaccineCenters`** : Fragment de liste de centres de vaccination.

- **Fonctionnement**: Cette classe prend charge du fragments (ListeView et barre de recherche) et charge les données des villes pour les afficher.

```java
public class VaccineCenters extends Fragment {
   private MainAdapter adapter;
   private SwipeRefreshLayout refreshLayout;
   private CityRepository cityRepository;

   public VaccineCenters() {}

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
      ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_vaccine_centers, container, false);

      cityRepository = new CityRepository(root.getContext());

      MaterialToolbar toolbar = root.findViewById(R.id.top_toolbar);

      MenuItem searchItem = toolbar.getMenu().findItem(R.id.search_cities);
      SearchView searchView = (SearchView) searchItem.getActionView();
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
         @Override
         public boolean onQueryTextSubmit(String query) {
            return false;
         }

         @Override
         public boolean onQueryTextChange(String newText) {
            adapter.filter(newText);
            return true;
         }
      });

      adapter = new MainAdapter(new MainAdapter.OnItemClickListener() {
         @Override
         public void onItemClicked(City city) {
            Log.d("clicked", "You clicked " + city.getCity_name());
            MapsFragment mapFragment = new MapsFragment();
            mapFragment.setCenters(city.getCenters());
            FragmentManager fragmentManager = getFragmentManager();

            fragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, mapFragment)
                .addToBackStack("VaccineCenters")
                .commit();
         }
      });
      RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
      recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));
      recyclerView.setAdapter(adapter);

      refreshLayout = root.findViewById(R.id.refresh);
      refreshLayout.setOnRefreshListener(this::loadCities);

      loadCities();

      return root;
   }

   private void loadCities() {
      adapter.setData(cityRepository.getAllCities());
      refreshLayout.setRefreshing(false);
   }
}
```

- **`com.maskdetector.fragments/MainAdapter`** : permet d’adapter les données sources au layout.

```java
public class MainAdapter extends ListAdapter<City, MainAdapter.MainViewHolder> {
    private List<City> originalList = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClicked(City city);
    }

    private final OnItemClickListener clickListener;

    public MainAdapter(OnItemClickListener clickListener) {
        super(DIFF_CALLBACK);

        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_main, parent, false);
        return new MainViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    public void setData(@Nullable List<City> list) {
        originalList = list;
        super.submitList(list);
    }

    public void filter(String query) {
        List<City> filteredList = new ArrayList<>();
        for (City city : originalList) {
            if (city.getCity_name().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(city);
            }
        }
        submitList(filteredList);
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTitle;
        private final TextView textCount;
        private City city;

        MainViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            itemView.setOnClickListener(v -> listener.onItemClicked(city));
            textTitle = itemView.findViewById(R.id.textTitle);
            textCount = itemView.findViewById(R.id.textCount);
        }

        @SuppressLint("SetTextI18n")
        void bindTo(City city) {
            this.city = city;

            textTitle.setText(city.getCity_name());
            textCount.setText(String.valueOf(city.getCenters().size()));
        }
    }

    private static final DiffUtil.ItemCallback<City> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<City>() {
            @Override
            public boolean areItemsTheSame(@NonNull City oldData,
                                           @NonNull City newData) {
                return oldData.getId().equals(newData.getId());
            }

            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull City oldData,
                                              @NonNull City newData) {
                return oldData.equals(newData);
            }
        };
}
```

- **`com.maskdetector.database.repository/CityRepository`** : Permet de récupérer les données sources et les convertir en objets Java.
- **Fonctionnement**: Cette classe utilise la librairie **Gson** pour la conversion des objets JSON en Objets Java.

```java
public class CityRepository {
    public static final String DATABASE_CITIES_JSON = "database/centersdata.json";

    private final Gson gson = new Gson();
    private final Context context;

    public CityRepository(Context context) {
        this.context = context;
    }

    public List<City> getAllCities() {
        CitiesData citiesData = gson.fromJson(Utils.getJsonFromAssets(context, DATABASE_CITIES_JSON), CitiesData.class);
        return citiesData.getData();
    }
}
```

- **`com.maskdetector.utils/Utils`** : Classe utilitaire permettant l'accés aux fichiers présents dans **`assets`**.

```java
public class Utils {
    public static String getJsonFromAssets(Context context, String fileName) {
        String jsonString;
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonString;
    }
}
```

- **`com.maskdetector.database/CitiesData`** : Classe utilitaire utilisée pour la conversion JSON - Objet Java.

```java
public class CitiesData {
    private List<City> data;

    public List<City> getData() {
        return data != null ? data : new ArrayList<>();
    }
}
```

- **`com.maskdetector.database.models/City`** : Classe représentant le modéle Ville.

```java
public class City {
    private Integer id;
    private String city_name;
    private List<Center> centers;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCity_name() {
        return city_name;
    }

    public void setCity_name(String city_name) {
        this.city_name = city_name;
    }

    public List<Center> getCenters() {
        return centers;
    }

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }

    @Override
    public String toString() {
        return city_name;
    }
}
```

- - **`assets/database/centersdata.json`** : Les données sur les centres de vaccinations (Source de données: [https://www.liqahcorona.ma/](https://www.liqahcorona.ma/)).

```JSON
{
	"data": [
      {
         "city_name": "AGADIR IDA OUTANANE",
         "id": 0,
         "centers": [
            {
               "latitude": "30.7456",
               "longitude": "-9.3279",
               "name": "Centre de Santé Rural Isk Ntiki",
               "address": "تيقي"
            },
				...
			]
	  	},
   		{
         "city_name": "AIN CHOCK",
         "id": 1,
         "centers": [
            {
               "latitude": "33.5264",
               "longitude": "-7.65111",
               "name": "TERRAIN DE PROXIMITE AL MADINA",
               "address": "إقامة المدينة سيدي عروف"
            },
				...
			]
	  	},
	  ...
	]
}
```

### 4- Présentation des centres dans la carte

> Dans cette interface, on présente les centres de vaccination de la ville sélectionnée dans la carte.

<table>
    <tr>
        <td>
            <div>Liste des centres de vaccination à Marrakech</div>
			<img src="https://lh6.googleusercontent.com/syqdDieWTV8e3yeZI3HWlaReEYP9dWhqIIP-nIG6RDv3GxIkWWcGmPFbNv-yXQdPbLwhjX24KQhbkgZrXPkW=w1920-h938-rw" width="" height="500" />
        </td>
    </tr>
</table>

#### Classes utilisées

- **`com.maskdetector.fragments/MapsFragment`** : Fragment de carte.

- **Fonctionnement**: Cette classe affiche l'ensemble des centres de vaccination de la ville sélectionnée dans la carte et calcule le point central entre eux pour centrer la carte.


```java
public class MapsFragment extends Fragment {

    private List<Center> centers;

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }

    public LatLng getMapCenter() {
        double sumLat = 0, sumLon = 0;
        for (Center center : centers) {
            sumLon += Double.parseDouble(center.getLongitude());
            sumLat += Double.parseDouble(center.getLatitude());
        }
        double centerLat = sumLat / centers.size();
        double centerLon = sumLon / centers.size();
        return new LatLng(centerLat, centerLon);
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            for (Center center : centers) {
                double lat = Double.parseDouble(center.getLatitude());
                double lon = Double.parseDouble(center.getLongitude());
                LatLng centerLatLng = new LatLng(lat, lon);
                googleMap.addMarker(new MarkerOptions().position(centerLatLng).title(center.getName() + "(" + center.getAddress() + ")"));
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(getMapCenter()));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
```

- **`com.maskdetector.database.models/Center`** : Classe représentant le modéle Centre de vaccination.

```java
public class Center {
    private String name;
    private String latitude;
    private String longitude;
    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Center{" +
                "name='" + name + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
```
