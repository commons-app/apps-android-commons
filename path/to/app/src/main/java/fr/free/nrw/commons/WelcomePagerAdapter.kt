import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomePagerAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {
  override fun createFragment(position: Int): Fragment {
    return WelcomeFragment()
  }

  override fun getItemCount(): Int {
    return 1
  }
}

class WelcomeFragment : Fragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_welcome, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val welcomeTextView = view.findViewById<TextView>(R.id.welcome_text_view)
    welcomeTextView.text = "Welcome to the project!"
  }
}