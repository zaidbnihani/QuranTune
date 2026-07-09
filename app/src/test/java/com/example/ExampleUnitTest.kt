package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
      assertEquals(4, 2 + 2)
  }
  
  @Test
  fun cardSorting_isCorrect() {
      val list = listOf(3, 1, 2)
      val sorted = list.sorted()
      assertEquals(listOf(1, 2, 3), sorted)
  }
}
