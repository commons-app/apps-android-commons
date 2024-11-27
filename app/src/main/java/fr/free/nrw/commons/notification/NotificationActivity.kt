package fr.free.nrw.commons.notification

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.databinding.ActivityNotificationBinding
import fr.free.nrw.commons.notification.models.Notification
import fr.free.nrw.commons.notification.models.NotificationType
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.NetworkUtils
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by root on 18.12.2017.
 */
class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding

    @Inject
    lateinit var controller: NotificationController

    @Inject
    lateinit var sessionManager: SessionManager

    private val tagNotificationWorkerFragment = "NotificationWorkerFragment"
    private var mNotificationWorkerFragment: NotificationWorkerFragment? = null
    private lateinit var adapter: NotificationAdapter
    private var notificationList: MutableList<Notification> = mutableListOf()
    private var notificationMenuItem: MenuItem? = null

    /**
     * Boolean isRead is true if this notification activity is for read section of notification.
     */
    private var isRead: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isRead = intent.getStringExtra("title") == "read"
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mNotificationWorkerFragment = supportFragmentManager.findFragmentByTag(
            tagNotificationWorkerFragment
        ) as? NotificationWorkerFragment
        initListView()
        setPageTitle()
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    fun removeNotification(notification: Notification) {
        if (isRead) return

        val disposable = Observable.defer { controller.markAsRead(notification) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                if (result) {
                    notificationList.remove(notification)
                    setItems(notificationList)
                    adapter.notifyDataSetChanged()
                    ViewUtil.showLongSnackbar(binding.container, getString(R.string.notification_mark_read))
                    if (notificationList.isEmpty()) {
                        setEmptyView()
                        binding.container.visibility = View.GONE
                        binding.noNotificationBackground.visibility = View.VISIBLE
                    }
                } else {
                    adapter.notifyDataSetChanged()
                    setItems(notificationList)
                    ViewUtil.showLongToast(this, getString(R.string.some_error))
                }
            }, { throwable ->
                if (throwable is InvalidLoginTokenException) {
                    val username = sessionManager.getUserName()
                    val logoutListener = CommonsApplication.BaseLogoutListener(
                        this,
                        getString(R.string.invalid_login_message),
                        username
                    )
                    CommonsApplication.instance.clearApplicationData(this, logoutListener)
                } else {
                    Timber.e(throwable, "Error occurred while loading notifications")
                    ViewUtil.showShortSnackbar(binding.container, R.string.error_notifications)
                }
                binding.progressBar.visibility = View.GONE
            })
        compositeDisposable.add(disposable)
    }

    private fun initListView() {
        binding.listView.layoutManager = LinearLayoutManager(this)
        val itemDecor = DividerItemDecoration(binding.listView.context, DividerItemDecoration.VERTICAL)
        binding.listView.addItemDecoration(itemDecor)
        refresh(isRead)
        adapter = NotificationAdapter { item ->
            Timber.d("Notification clicked %s", item.link)
            if (item.notificationType == NotificationType.EMAIL) {
                ViewUtil.showLongSnackbar(binding.container, getString(R.string.check_your_email_inbox))
            } else {
                handleUrl(item.link)
            }
            removeNotification(item)
        }
        binding.listView.adapter = adapter
    }

    private fun refresh(archived: Boolean) {
        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            binding.progressBar.visibility = View.GONE
            Snackbar.make(binding.container, R.string.no_internet, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry) { refresh(archived) }
                .show()
        } else {
            addNotifications(archived)
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.noNotificationBackground.visibility = View.GONE
        binding.container.visibility = View.VISIBLE
    }

    @SuppressLint("CheckResult")
    private fun addNotifications(archived: Boolean) {
        Timber.d("Add notifications")
        if (mNotificationWorkerFragment == null) {
            binding.progressBar.visibility = View.VISIBLE
            compositeDisposable.add(controller.getNotifications(archived)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ notificationList ->
                    notificationList.reversed()
                    Timber.d("Number of notifications is %d", notificationList.size)
                    this.notificationList = notificationList.toMutableList()
                    if (notificationList.isEmpty()) {
                        setEmptyView()
                        binding.container.visibility = View.GONE
                        binding.noNotificationBackground.visibility = View.VISIBLE
                    } else {
                        setItems(notificationList)
                    }
                    binding.progressBar.visibility = View.GONE
                }, { throwable ->
                    Timber.e(throwable, "Error occurred while loading notifications")
                    ViewUtil.showShortSnackbar(binding.container, R.string.error_notifications)
                    binding.progressBar.visibility = View.GONE
                }))
        } else {
            notificationList = mNotificationWorkerFragment?.notificationList?.toMutableList() ?: mutableListOf()
            setItems(notificationList)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        notificationMenuItem = menu.findItem(R.id.archived)
        setMenuItemTitle()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.archived -> {
                if (item.title == getString(R.string.menu_option_read)) {
                    startYourself(this, "read")
                } else if (item.title == getString(R.string.menu_option_unread)) {
                    onBackPressed()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleUrl(url: String?) {
        if (url.isNullOrEmpty()) return
        Utils.handleWebUrl(this, Uri.parse(url))
    }

    private fun setItems(notificationList: List<Notification>?) {
        if (notificationList.isNullOrEmpty()) {
            ViewUtil.showShortSnackbar(binding.container, R.string.no_notifications)
            binding.container.visibility = View.GONE
            setEmptyView()
            binding.noNotificationBackground.visibility = View.VISIBLE
            return
        }
        binding.container.visibility = View.VISIBLE
        binding.noNotificationBackground.visibility = View.GONE
        adapter.items = notificationList
    }

    private fun setPageTitle() {
        supportActionBar?.title = if (isRead) {
            getString(R.string.read_notifications)
        } else {
            getString(R.string.notifications)
        }
    }

    private fun setEmptyView() {
        binding.noNotificationText.text = if (isRead) {
            getString(R.string.no_read_notification)
        } else {
            getString(R.string.no_notification)
        }
    }

    private fun setMenuItemTitle() {
        notificationMenuItem?.title = if (isRead) {
            getString(R.string.menu_option_unread)
        } else {
            getString(R.string.menu_option_read)
        }
    }

    companion object {
        fun startYourself(context: Context, title: String) {
            val intent = Intent(context, NotificationActivity::class.java)
            intent.putExtra("title", title)
            context.startActivity(intent)
        }
    }
}
