package upload.firbase.com.firebaseuploadexample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Random;


public class CameraActivity extends AppCompatActivity {

    TextView button;
    ImageView imageview;
    private TextView scan;
    StorageReference myreference;
    DatabaseReference mDatabaseRef;
    private Button analyzeBtn;
    private Button clearBtn;

    private static final String LOG_TAG = "Barcode Scanner API";
    private static final int PHOTO_REQUEST = 10;
    private Uri imageUri;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVE_INSTANCE_URI = "uri";
    private static final String SAVE_INSTANCE_RESULT = "result";
    ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();

        if(savedInstanceState != null){

            imageUri = Uri.parse(savedInstanceState.getString(SAVE_INSTANCE_URI));
            scan.setText(savedInstanceState.getString(SAVE_INSTANCE_RESULT));

        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scan.setText("");
                imageview.setImageBitmap(null);
                ActivityCompat.requestPermissions(CameraActivity.this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_PERMISSION);

            }
        });



        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imageview.setImageBitmap(null);

            }
        });

    }


    private void init() {

        button = (TextView) findViewById(R.id.button);
    imageview = (ImageView) findViewById(R.id.imgview);
    scan = (TextView) findViewById(R.id.txtContent);
        myreference = FirebaseStorage.getInstance().getReference("uploads");
       mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads");
    progressDialog = new ProgressDialog(CameraActivity.this);

    analyzeBtn = (Button)findViewById(R.id.analyze);
    clearBtn = (Button) findViewById(R.id.clear);
}



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){

            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {

                    Toast.makeText(CameraActivity.this, "Permission Denied!"+requestCode, Toast.LENGTH_SHORT).show();
                }


        }
    }

    private void takePicture() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Random random = new Random();
        int key =random.nextInt(1000);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture"+key+".jpg");
        //  File photo = new File(getCacheDir(), "picture.jpg");
        imageUri = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                Scanner  scanner = new Scanner();
                final Bitmap bitmap = scanner.decodeBitmapUri(CameraActivity.this, imageUri);
                progressDialog.setTitle("Uploading..");
                progressDialog.show();
                StorageReference filepath = myreference.child("uploads").child(imageUri.getLastPathSegment());
                filepath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageview.setImageBitmap(bitmap);
                        Toast.makeText(CameraActivity.this,"uploaded",Toast.LENGTH_LONG).show();
                        scan.setText("Image just uploaded on Firebase");
                        progressDialog.dismiss();


                        String name = "Picture";

                        //upload image in firebase database

                        Upload upload = new Upload(System.currentTimeMillis()+"picture",
                                taskSnapshot.getDownloadUrl().toString());
                        String uploadId = mDatabaseRef.push().getKey();
                        mDatabaseRef.child(uploadId).setValue(upload);

                    }
                });


            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    private void launchMediaScanIntent() {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putString(SAVE_INSTANCE_RESULT, imageUri.toString());
            outState.putString(SAVE_INSTANCE_RESULT, scan.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }



}
