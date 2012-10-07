package org.wikimedia.commons;

import java.io.*;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.support.v4.app.NavUtils;

public class ShareActivity extends Activity {

    private CommonsApplication app;
   
    private ImageView backgroundImageView;
    private Button uploadButton;
    private EditText titleEdit;
    private EditText descEdit;
    
    private Uri imageUri;
    
    class UploadImageTask extends AsyncTask<String, Integer, String> {
        MWApi api;
        Activity context;
        ProgressDialog dialog;
        
        @Override
        protected String doInBackground(String... params)  {
            Uri imageUri = Uri.parse(params[0]);
            String filename = params[1];
            String text = params[2];
            String comment = params[3];
            
            InputStream file;
            
            try {
                file =  context.getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            Log.d("Commons", "isLoggedIn? " + api.isLoggedIn);
            
            try {
                ApiResult result = api.upload(filename, file, text, comment);
//                Document document = (Document)result.getDocument();
//                StringWriter wr = new StringWriter();
//                try {
//                    Transformer trans = TransformerFactory.newInstance().newTransformer();
//                    trans.transform(new DOMSource(result.getDocument()),   new StreamResult(wr));
//                    String res = wr.toString();
//                    return res;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    //FUCK YOU YOU SON OF A LEMON PARTY
//                }
                
//                DOMImplementationLS domImplLS = (DOMImplementationLS) document
//                    .getImplementation();
//                LSSerializer serializer = domImplLS.createLSSerializer();
//                String str = serializer.writeToString(result.getDocument());
//                return str;
                return result.getString("/api/upload/@result");
            } catch (IOException e) {
                e.printStackTrace();
                return "Failure";
            }
        }
        
        UploadImageTask(MWApi api, Activity context) {
            this.api = api;
            this.context = context;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result.equals("Success")) {
                dialog.hide();
                Toast successToast = Toast.makeText(context, R.string.uploading_success, Toast.LENGTH_LONG);
                successToast.show();
                context.finish();
            } else {
                dialog.hide();
                Toast failureToast = Toast.makeText(context, R.string.uploading_failed, Toast.LENGTH_LONG);
                failureToast.show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setTitle(R.string.uploading_title);
            dialog.setMessage(getString(R.string.uploading_message));
            dialog.show();
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Actionbar overlay on top of imageview (should be called before .setcontentview)
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getActionBar().setDisplayShowTitleEnabled(false);
        
        setContentView(R.layout.activity_share);
        
        app = (CommonsApplication)this.getApplicationContext();
        
        backgroundImageView = (ImageView)findViewById(R.id.backgroundImage);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        descEdit = (EditText)findViewById(R.id.descEdit);
        uploadButton = (Button)findViewById(R.id.uploadButton);
    
        Intent intent = getIntent();
        
        final Activity that = this;
        
        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            if(intent.getType().startsWith("image/")) {
                ImageLoaderTask loader = new ImageLoaderTask(backgroundImageView);
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                loader.execute(imageUri);
                
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UploadImageTask uploadImage = new UploadImageTask(app.getApi(), that);
                        Log.d("Commons", "Starting upload, yo!");
                        uploadImage.execute(imageUri.toString(), titleEdit.getText().toString(), descEdit.getText().toString(), "Mobile Upload represent!");
                    }
                });
            }
        }
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        if(!app.getApi().isLoggedIn) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            this.startActivity(loginIntent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
