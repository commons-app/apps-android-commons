package fr.free.nrw.commons.quiz

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue


class QuizControllerTest {

    @Mock
    private lateinit var quizController: QuizController

    @Mock
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getString(any(Int::class.java)))
            .thenReturn("")
        quizController = QuizController()
    }

    @Test
    fun testInitialise() {
        quizController.initialize(context)
    }

    @Test
    fun testGetQuiz() {
        assertThat(quizController.getQuiz(), notNullValue())
    }

}