package fr.free.nrw.commons.nearby

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.RadioButton
import fr.free.nrw.commons.databinding.ActivityWikidataFeedbackBinding
import fr.free.nrw.commons.theme.BaseActivity
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
        binding.radioButton1.setText("'$place' does not exist anymore, no picture can ever be taken of it.")
        binding.radioButton2.setText("'$place' is at a different place (please specify the correct place below, if possible tell us the correct latitude/longitude).")
        binding.radioButton3.setText("Other problem or information (please explain below).")
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding.appCompatButton.setOnClickListener {
            var desc = findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId).text
            var det = binding.detailsEditText.text.toString()

            binding.radioGroup.clearCheck()
            binding.detailsEditText.setText("")
            Single.defer<Boolean?>(Callable<SingleSource<Boolean?>> {
                pageEditHelper.makePageEdit(
                    this, pageTitle, preText,
                    desc.toString(),
                    det, lat, lng
                )
            } as Callable<SingleSource<Boolean?>>)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ aBoolean: Boolean? ->
                }, { throwable: Throwable? ->
                    Timber.e(throwable!!)
                })

        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}