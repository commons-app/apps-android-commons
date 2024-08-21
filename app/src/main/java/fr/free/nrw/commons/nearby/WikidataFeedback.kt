package fr.free.nrw.commons.nearby

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityWikidataFeedbackBinding
import fr.free.nrw.commons.theme.BaseActivity
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * Activity for providing feedback about Wikidata items.
 */
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
        pageTitle = getString(R.string.talk) + ":" + wikidataQId
        binding.toolbarBinding.toolbar.title = pageTitle
        binding.textHeader.text =
            getString(R.string.write_something_about_the_item, place)
        binding.radioButton1.setText(
            getString(
                R.string.does_not_exist_anymore_no_picture_can_ever_be_taken_of_it,
                place
            ))
        binding.radioButton2.setText(
            getString(
                R.string.is_at_a_different_place_please_specify_the_correct_place_below_if_possible_tell_us_the_correct_latitude_longitude,
                place
            ))
        binding.radioButton3.setText(getString(R.string.other_problem_or_information_please_explain_below))
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding.appCompatButton.setOnClickListener {
            var desc = findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId).text
            var det = binding.detailsEditText.text.toString()
            if (binding.radioGroup.checkedRadioButtonId == R.id.radioButton3 && binding.detailsEditText.text.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_enter_some_comments), Toast.LENGTH_SHORT
                ).show()
            } else {
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
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}