package fr.free.nrw.commons.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnitRunner;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;

import androidx.test.runner.AndroidJUnit4;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(AndroidJUnit4.class)
public class PasteSensitiveTextInputEditTextTest {

    private Context context;
    private PasteSensitiveTextInputEditText textView;

    @Before
    public void setup(){
        context = ApplicationProvider.getApplicationContext();
        textView = new PasteSensitiveTextInputEditText(context);
    }

    @Test
    public void onTextContextMenuItem() {

        textView.setText("Text");
        textView.onTextContextMenuItem(android.R.id.paste);
        assertEquals("Text", textView.getText().toString());
    }

    @Test
    public void setFormattingAllowed() throws Exception {

        Field fieldFormattingAllowed = textView.getClass().getDeclaredField("formattingAllowed");
        fieldFormattingAllowed.setAccessible(true);

        textView.setFormattingAllowed(true);
        assertTrue(fieldFormattingAllowed.getBoolean(textView));

        textView.setFormattingAllowed(false);
        assertFalse(fieldFormattingAllowed.getBoolean(textView));
    }


}