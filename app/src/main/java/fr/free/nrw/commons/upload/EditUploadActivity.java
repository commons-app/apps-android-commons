package fr.free.nrw.commons.upload;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;

import static fr.free.nrw.commons.upload.UploadService.EXTRA_FILES;

public class EditUploadActivity extends AppCompatActivity {

    GridView gridView;
    CropImageView cropImageView;
    HashMap<Integer, Uri> hashMap=new HashMap<Integer, Uri>();
    private static int currentSelectedPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_upload);

        gridView = (GridView) findViewById(R.id.gridView);
        cropImageView = findViewById(R.id.cropImageView);
        ArrayList<UploadableFile> files = getIntent().getParcelableArrayListExtra(EXTRA_FILES);
        ImageAdapter adapter = new ImageAdapter(this, files);
        gridView.setOnItemClickListener((parent, v, position, id) -> {
            currentSelectedPosition=position;
            CropImage.activity(Uri.fromFile(files.get(position).getFile()))
                    .start(this);
        });

        gridView.setAdapter(adapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                hashMap.put(currentSelectedPosition,resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, UploadActivity.class);
        intent.putExtra("hashMap", hashMap);
        startActivity(intent);
    }
}
