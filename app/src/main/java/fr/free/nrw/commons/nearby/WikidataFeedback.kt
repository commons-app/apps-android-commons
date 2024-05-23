package fr.free.nrw.commons.nearby

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.databinding.ActivityWikidataFeedbackBinding
import fr.free.nrw.commons.nearby.model.Description
import fr.free.nrw.commons.nearby.model.Title
import fr.free.nrw.commons.theme.BaseActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import timber.log.Timber
import javax.inject.Inject


class WikidataFeedback : BaseActivity() {
    private lateinit var binding: ActivityWikidataFeedbackBinding
    var place: String = ""
    var wikidataQId: String = ""

    @Inject
    lateinit var nearbyController: NearbyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWikidataFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        place = intent.getStringExtra("place") ?: ""
        wikidataQId = intent.getStringExtra("qid") ?: ""
        binding.toolbarBinding.toolbar.title = "Talk:" + wikidataQId
        binding.textHeader.text = "Write something about the "+"'$place'"+" item. It will be publicly visible."
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        getWikidataFeedback(place, wikidataQId)

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
        try {
            val wikiTalkObservable = Observable
                .fromCallable {
                    nearbyController.getWikiTalk("Talk:"+wikidataQID)
                }
            compositeDisposable.add(
                wikiTalkObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        if (result != null) {
                            Timber.tag("PRINT").d("$result")
                            val key = "\"*\":\""
                            var startIndex = result.indexOf(key)
                            startIndex += key.length
                            var endIndex = result.indexOf("\"", startIndex)
                            while (endIndex != -1 && result[endIndex - 1] == '\\') {
                                endIndex = result.indexOf("\"", endIndex + 1)
                            }
                            var value = result.substring(startIndex, endIndex)

                            value = value.replace("\\n", "\n").replace("\\\"", "\"")
                            Timber.tag("PRINT").e(value);
                            updateUi(name, value, extractData(value))
                        } else {
                            Timber.d("result is null")
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
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

    private fun updateUi(place: String, feedback: String,titles: List<Title> ) {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = TitleAdapter(titles)
        binding.progressBar.visibility = View.GONE
        binding.activityLayout.visibility = View.VISIBLE
        binding.descText.text = if (feedback.isNotEmpty()) feedback else "No Feedback"
    }
    fun extractData(input: String): List<Title> {
        val titlePattern = Regex("""==\s*(.*?)\s*==""")
        val descriptionPattern = Regex("==\\n(.*?\\.)")
        val userPattern = Regex("""\[\[User:(.*?)\|""")
        val timestampPattern = Regex("""\d{2}:\d{2}, \d+ \w+ \d{4} \(UTC\)""")

        val titles = titlePattern.findAll(input).map { it.groupValues[1] }.toList()
        val descriptions = descriptionPattern.findAll(input).map { it.groupValues[1] }.toList()
        val users = userPattern.findAll(input).map { it.groupValues[1] }.toList()
        val timestamps = timestampPattern.findAll(input).map { it.value }.toList()

        val groupedDescriptions = mutableListOf<Description>()
        for (i in 0 until minOf(descriptions.size, users.size, timestamps.size)) {
            groupedDescriptions.add(Description(descriptions[i], users[i], timestamps[i]))
        }

        val titleToDescriptions = mutableMapOf<String, MutableList<Description>>()
        var currentTitle: String? = null

        input.lines().forEach { line ->
            val titleMatch = titlePattern.matchEntire(line)
            if (titleMatch != null) {
                currentTitle = titleMatch.groupValues.getOrNull(1)
                currentTitle?.let {
                    titleToDescriptions[it] = mutableListOf()
                }
            } else if (!currentTitle.isNullOrBlank()) {
                groupedDescriptions.removeFirstOrNull()
                    ?.let { titleToDescriptions[currentTitle]?.add(it) }
            }
        }

        return titleToDescriptions.map { (title, descriptions) -> Title(title, descriptions) }
    }
}