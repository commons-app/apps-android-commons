import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
  private lateinit var welcomeTextView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_welcome)

    welcomeTextView = findViewById(R.id.welcome_text_view)

    // Display welcome message
    welcomeTextView.text = "Welcome to the project!"
  }
}