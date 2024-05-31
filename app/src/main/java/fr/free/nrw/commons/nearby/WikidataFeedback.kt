package fr.free.nrw.commons.nearby

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.databinding.ActivityWikidataFeedbackBinding
import fr.free.nrw.commons.nearby.model.TalkItem
import fr.free.nrw.commons.theme.BaseActivity
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Callable
import javax.inject.Inject


class WikidataFeedback : BaseActivity() {
    private lateinit var binding: ActivityWikidataFeedbackBinding
    var place: String = ""
    var wikidataQId: String = ""
    var pageTitle: String = ""
    var preText: String = ""
    var lat: Double = 0.0
    var lng: Double = 0.0

    @Inject
    lateinit var pageEditHelper: PageEditHelper

    @Inject
    lateinit var nearbyController: NearbyController

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWikidataFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        place = intent.getStringExtra("place") ?: ""
        wikidataQId = intent.getStringExtra("qid") ?: ""
        pageTitle = "Talk:" + wikidataQId
        binding.toolbarBinding.toolbar.title = pageTitle
        binding.textHeader.text =
            "Write something about the " + "'$place'" + " item. It will be publicly visible."
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        getWikidataFeedback(place, wikidataQId)

        binding.appCompatButton.setOnClickListener {
            var desc = binding.descriptionEditText.text.toString()
            var det = binding.detailsEditText.text.toString()

            if (binding.descriptionEditText.text!!.isNotEmpty() && binding.detailsEditText.text!!.isNotEmpty()) {
                binding.descriptionEditText.setText("")
                binding.detailsEditText.setText("")
                Single.defer<Boolean?>(Callable<SingleSource<Boolean?>> {
                    pageEditHelper.makePageEdit(
                        this, pageTitle, preText,
                        desc,
                        det, lat, lng
                    )
                } as Callable<SingleSource<Boolean?>>)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ aBoolean: Boolean? ->
                        if (aBoolean!!) {
                            getWikidataFeedback(place, wikidataQId)
                        }
                    }, { throwable: Throwable? ->
                        Timber.e(throwable!!)
                    })
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * This function starts the Wikidata feedback activity of the selected place
     * The API returns feedback given by other users
     */
    @SuppressLint("CheckResult")
    fun getWikidataFeedback(name: String, wikidataQID: String?) {
        binding.talkPageLayout.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE

        try {
            val wikiTalkObservable = Observable
                .fromCallable {
                    nearbyController.getWikiTalk("Talk:" + wikidataQID)
                }
            compositeDisposable.add(
                wikiTalkObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        if (result != null) {
                            val key = "\"*\":\""
                            var startIndex = result.indexOf(key)
                            startIndex += key.length
                            var endIndex = result.indexOf("\"", startIndex)
                            while (endIndex != -1 && result[endIndex - 1] == '\\') {
                                endIndex = result.indexOf("\"", endIndex + 1)
                            }
                            var value = result.substring(startIndex, endIndex)
                            value = value.replace("\\n", "\n").replace("\\\"", "\"")
                            preText = value

                            if (preText != "") {
                                val regex = "==(.+?)==".toRegex()
                                val matchResult = regex.find(preText)

                                matchResult?.let {
                                    binding.pageTitleTextView.text =
                                        it.groupValues[1] // Access the captured group
                                }
                                val usernameRegex =
                                    """\[\[User:[A-Za-z0-9_]+\|([A-Za-z0-9_]+)\]\]""".toRegex()
                                val dateRegex =
                                    """(\d{2}:\d{2}, \d{2} \w+ \d{4} \(UTC\))""".toRegex()

                                val lastUsernameMatch = usernameRegex.findAll(preText).lastOrNull()
                                val lastDateMatch = dateRegex.findAll(preText).lastOrNull()

                                val username = lastUsernameMatch?.groupValues?.get(1)
                                val date = lastDateMatch?.value

                                binding.lastEditTextView.text =
                                    "Last Edited by " + username + " on " + date

                                val descriptionRegex = """\* <i><nowiki>(.*?)</nowiki></i>""".toRegex()
                                val detailsRegex = """Details: <i><nowiki>(.*?)</nowiki></i>""".toRegex()
                                val descriptions =
                                    descriptionRegex.findAll(preText).map { it.groupValues[1] }
                                        .toList()
                                val details =
                                    detailsRegex.findAll(preText).map { it.groupValues[1] }.toList()

                                var t = ArrayList<TalkItem>();
                                for (i in 0..descriptions.size - 1) {
                                    t.add(TalkItem(descriptions.get(i), details.get(i)))
                                }
                                if (t.size > 0) {
                                    updateUi(t)
                                } else {
                                    binding.progressBar.visibility = View.GONE
                                }
                            } else {
                                binding.progressBar.visibility = View.GONE
                            }
                        } else {
                            Timber.d("result is null")
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                        }
                    }, { throwable ->
                        Timber.e(throwable, "Error occurred while loading notifications")
                        throwable.printStackTrace()
                    })
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun updateUi(titles: List<TalkItem>) {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = TitleAdapter(titles)
        binding.talkPageLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }
}