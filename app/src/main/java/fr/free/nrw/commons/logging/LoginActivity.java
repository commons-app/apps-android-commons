package fr.free.nrw.commons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.logging.CommonsLogSender;

public class FeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                EditText text1 = (EditText) findViewById(R.id.editText2);
                EditText text2 = (EditText) findViewById(R.id.editText3);
                String body = "Problem : " + text1.getText().toString()+"\n"+"Steps: "+text2.getText().toString()+"\n";
                String mailTo = "mando.ato@gmail.com";
                String subject = "error report";
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.fromParts("mailto", mailTo, null));
                emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(emailIntent);
            }
        });
    }

}
