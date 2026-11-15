package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import com.google.gson.JsonParser
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import fr.free.nrw.commons.nearby.model.NearbyResponse
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.Locale

internal const val FALLBACK_LANGUAGES =
    "en,fr,de,es,ja,ru,it,zh,pt,ar,fa,pl,nl,id,uk,he,sv,cs,ko,vi,ca,no,fi,hu,tr,th,hi,bn,ceb," +
        "ro,sw,kk,da,eo,sr,lt,sk,bg,sl,eu,et,hr,ms,el,arz,ur,ta,te,nn,gl,az,af,bs,be,ml,ka,is,sq," +
        "uz,la,br,mk,lv,azb,mr,sh,tl,cy,ckb,ast,be-tarask,zh-yue,hy,pa,as,my,kn,ne,si,tt,ha,war," +
        "zh-min-nan,vo,min,lmo,ht,lb,gu,tg,sco,ku,new,bpy,nds,io,pms,su,oc,jv,nap,ba,scn,wa,bar," +
        "an,ksh,szl,fy,frr,als,ia,ga,yi,mg,gd,vec,ce,sa,mai,xmf,sd,wuu,mrj,mhr,km,roa-tara,am," +
        "roa-rup,map-bms,bh,mnw,shn,bcl,co,cv,dv,nds-nl,fo,hif,fur,gan,glk,hak,ilo,pam,csb,avk," +
        "lij,li,gv,mi,mt,nah,nrm,se,nov,qu,os,pi,pag,ps,pdc,rm,bat-smg,sc,to,tk,hsb,fiu-vro,vls," +
        "yo,diq,zh-classical,frp,lad,kw,mn,haw,ang,ln,ie,wo,tpi,ty,crh,nv,jbo,ay,pcd,zea,eml,ky," +
        "ig,or,cbk-zam,kg,arc,rmy,ab,gn,so,kab,ug,stq,udm,ext,mzn,pap,cu,sah,tet,sn,lo,pnb,iu,na," +
        "got,bo,dsb,chr,cdo,om,sm,ee,ti,av,bm,zu,pnt,cr,pih,ss,ve,bi,rw,ch,xh,kl,ik,bug,dz,ts,tn," +
        "kv,tum,xal,st,tw,bxr,ak,ny,fj,lbe,za,ks,ff,lg,sg,rn,chy,mwl,lez,bjn,gom,tyv,vep,nso,kbd," +
        "ltg,rue,pfl,gag,koi,krc,ace,olo,kaa,mdf,myv,srn,ady,jam,tcy,dty,atj,kbp,din,lfn,gor,inh," +
        "sat,hyw,nqo,ban,szy,awa,ary,lld,smn,skr,mad,dag,shi,nia,ki,gcr"

/** Queries Wikidata for entity relationships, place details and nearby item counts. */
class WikidataQueryClient(
    private val okHttpClient: OkHttpClient,
    private val depictsClient: DepictsClient,
    private val sparqlQueryUrl: String,
    private val gson: Gson,
) {
    /** Fetches one page of subclasses for the selected Wikidata item. */
    @Throws(IOException::class)
    fun getChildDepictions(
        qid: String,
        startPosition: Int,
        limit: Int,
    ): Single<List<DepictedItem>> =
        depictedItemsFrom(sparqlQuery(qid, startPosition, limit, "/queries/subclasses_query.rq"))

    /** Fetches one page of parent classes for the selected Wikidata item. */
    @Throws(IOException::class)
    fun getParentDepictions(
        qid: String,
        startPosition: Int,
        limit: Int,
    ): Single<List<DepictedItem>> =
        depictedItemsFrom(
            sparqlQuery(
                qid,
                startPosition,
                limit,
                "/queries/parentclasses_query.rq",
            ),
        )

    /** Refreshes place details and localized labels for the supplied Wikidata entities. */
    @Throws(IOException::class)
    fun getPlaces(
        placeList: List<Place>,
        language: String,
    ): List<Place>? {
        val wikidataQuery = FileUtils.readFromResource("/queries/query_for_item.rq")
        var qids = ""
        for (place in placeList) {
            qids += """
${"wd:" + place.wikiDataEntityId}"""
        }
        val query =
            wikidataQuery
                .replace("\${ENTITY}", qids)
                .replace("\${LANG}", language)
                .replace("\${FALLBACK_LANGUAGES}", FALLBACK_LANGUAGES)
        val urlBuilder: HttpUrl.Builder =
            sparqlQueryUrl
                .toHttpUrlOrNull()!!
                .newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("format", "json")

        val request: Request = Request.Builder().url(urlBuilder.build()).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = response.body!!.string()
                val nearbyResponse = gson.fromJson(json, NearbyResponse::class.java)
                val bindings = nearbyResponse.results.bindings
                val places: MutableList<Place> = ArrayList()
                for (item in bindings) {
                    val placeFromNearbyItem = Place.from(item)
                    places.add(placeFromNearbyItem)
                }
                return places
            } else {
                throw IOException("Unexpected response code: " + response.code)
            }
        }
    }

    /**
     * Returns the count of items in the specified area by querying Wikidata.
     *
     * @param queryParams: a `NearbyQueryParam` specifying the geographical area.
     * @return The count of items in the specified area.
     */
    @Throws(Exception::class)
    fun getNearbyItemCount(queryParams: NearbyQueryParams): Int {
        val wikidataQuery: String =
            when (queryParams) {
                is NearbyQueryParams.Rectangular -> {
                    val westCornerLat = queryParams.screenTopRight.latitude
                    val westCornerLong = queryParams.screenTopRight.longitude
                    val eastCornerLat = queryParams.screenBottomLeft.latitude
                    val eastCornerLong = queryParams.screenBottomLeft.longitude
                    FileUtils
                        .readFromResource("/queries/rectangle_query_for_item_count.rq")
                        .replace("\${LAT_WEST}", String.format(Locale.ROOT, "%.4f", westCornerLat))
                        .replace("\${LONG_WEST}", String.format(Locale.ROOT, "%.4f", westCornerLong))
                        .replace("\${LAT_EAST}", String.format(Locale.ROOT, "%.4f", eastCornerLat))
                        .replace("\${LONG_EAST}", String.format(Locale.ROOT, "%.4f", eastCornerLong))
                }

                is NearbyQueryParams.Radial -> {
                    FileUtils
                        .readFromResource("/queries/radius_query_for_item_count.rq")
                        .replace(
                            "\${LAT}",
                            String.format(Locale.ROOT, "%.4f", queryParams.center.latitude),
                        ).replace(
                            "\${LONG}",
                            String.format(Locale.ROOT, "%.4f", queryParams.center.longitude),
                        ).replace("\${RAD}", String.format(Locale.ROOT, "%.2f", queryParams.radiusInKm))
                }
            }

        val urlBuilder: HttpUrl.Builder =
            sparqlQueryUrl
                .toHttpUrlOrNull()!!
                .newBuilder()
                .addQueryParameter("query", wikidataQuery)
                .addQueryParameter("format", "json")

        val request: Request =
            Request
                .Builder()
                .url(urlBuilder.build())
                .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.body != null && response.isSuccessful) {
            val json = response.body!!.string()
            return JsonParser
                .parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("results")
                .getAsJsonArray("bindings")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("itemCount")
                .get("value")
                .asInt
        }
        throw IOException(response.message)
    }

    /** Resolves the query result IDs through the existing entity-to-depiction mapping. */
    private fun depictedItemsFrom(request: Request): Single<List<DepictedItem>> {
        return depictsClient.toDepictions(
            Single
                .fromCallable({
                    okHttpClient.newCall(request).execute().body.use { body ->
                        return@fromCallable gson.fromJson<SparqlResponse>(
                            body!!.string(),
                            SparqlResponse::class.java,
                        )
                    }
                })
                .doOnError({ t: Throwable? -> Timber.e(t) }),
        )
    }

    /** Builds the query from the resource template with the paging parameters for this request. */
    @Throws(IOException::class)
    private fun sparqlQuery(
        qid: String,
        startPosition: Int,
        limit: Int,
        fileName: String,
    ): Request {
        val query =
            FileUtils
                .readFromResource(fileName)
                .replace("\${QID}", qid)
                .replace("\${LANG}", "\"" + Locale.getDefault().language + "\"")
                .replace("\${LIMIT}", "" + limit)
                .replace("\${OFFSET}", "" + startPosition)
        val urlBuilder: HttpUrl.Builder =
            sparqlQueryUrl
                .toHttpUrlOrNull()!!
                .newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("format", "json")
        return Request.Builder().url(urlBuilder.build()).build()
    }
}
