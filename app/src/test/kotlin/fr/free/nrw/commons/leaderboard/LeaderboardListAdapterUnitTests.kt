package fr.free.nrw.commons.leaderboard

import android.content.Context
import android.widget.TextView
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.leaderboard.LeaderboardList
import fr.free.nrw.commons.profile.leaderboard.LeaderboardListAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LeaderboardListAdapterUnitTests {

    private lateinit var context: Context
    private lateinit var adapter: LeaderboardListAdapter

    @Mock
    private lateinit var viewHolder: LeaderboardListAdapter.ListViewHolder

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var simpleDraweeView: SimpleDraweeView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MockitoAnnotations.initMocks(this)
        SoLoader.setInTestMode()
        Fresco.initialize(context)

        adapter = LeaderboardListAdapter()

        val rank: Field =
            LeaderboardListAdapter.ListViewHolder::class.java.getDeclaredField("rank")
        rank.isAccessible = true
        rank.set(viewHolder, textView)

        val avatar: Field =
            LeaderboardListAdapter.ListViewHolder::class.java.getDeclaredField("avatar")
        avatar.isAccessible = true
        avatar.set(viewHolder, simpleDraweeView)

        val username: Field =
            LeaderboardListAdapter.ListViewHolder::class.java.getDeclaredField("username")
        username.isAccessible = true
        username.set(viewHolder, textView)

        val count: Field =
            LeaderboardListAdapter.ListViewHolder::class.java.getDeclaredField("count")
        count.isAccessible = true
        count.set(viewHolder, textView)

        val itemView: Field =
            RecyclerView.ViewHolder::class.java.getDeclaredField("itemView")
        itemView.isAccessible = true
        itemView.set(viewHolder, textView)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBindViewHolder() {
        val list = LeaderboardList()
        list.rank = 1
        list.avatar = ""
        list.categoryCount = 1
        list.username = ""
        adapter.submitList(mockPagedList(listOf(list)))
        adapter.onBindViewHolder(viewHolder, 0)
    }

    fun <T> mockPagedList(list: List<T>): PagedList<T> {
        val pagedList = Mockito.mock(PagedList::class.java) as PagedList<T>
        `when`(pagedList[ArgumentMatchers.anyInt()]).then { invocation ->
            val index = invocation.arguments.first() as Int
            list[index]
        }
        `when`(pagedList.size).thenReturn(list.size)
        return pagedList
    }

}