package com.example

import com.example.data.model.CourseRatingHelper
import com.example.data.model.CourseReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseRatingTest {

  @Test
  fun emptyReviewsReturnsZeroSummary() {
    val summary = CourseRatingHelper.summarizeCourse(emptyList())
    assertEquals(0.0, summary.averageRating, 0.001)
    assertEquals(0, summary.reviewCount)
    assertEquals("", CourseRatingHelper.formatAverage(summary))
    assertTrue(CourseRatingHelper.summarizeByCourse(emptyList()).isEmpty())
  }

  @Test
  fun aggregatesAndRoundsAverageToOneDecimalPerCourse() {
    val reviews = listOf(
      CourseReview(id = "u1_c1", userId = "u1", courseId = "c1", rating = 5, reviewText = "Great"),
      CourseReview(id = "u2_c1", userId = "u2", courseId = "c1", rating = 4, reviewText = "Solid"),
      CourseReview(id = "u3_c1", userId = "u3", courseId = "c1", rating = 5, reviewText = "Loved it"),
      CourseReview(id = "u1_c2", userId = "u1", courseId = "c2", rating = 3, reviewText = "Okay")
    )

    val byCourse = CourseRatingHelper.summarizeByCourse(reviews)
    val c1 = byCourse.getValue("c1")
    val c2 = byCourse.getValue("c2")

    assertEquals(3, c1.reviewCount)
    assertEquals(4.7, c1.averageRating, 0.001)
    assertEquals("4.7", CourseRatingHelper.formatAverage(c1))

    assertEquals(1, c2.reviewCount)
    assertEquals(3.0, c2.averageRating, 0.001)
    assertEquals("3.0", CourseRatingHelper.formatAverage(c2))
  }

  @Test
  fun ignoresOutOfRangeRatingsOrBlankCourseIds() {
    val reviews = listOf(
      CourseReview(id = "u1_c1", userId = "u1", courseId = "c1", rating = 0, reviewText = "Invalid low"),
      CourseReview(id = "u2_c1", userId = "u2", courseId = "c1", rating = 6, reviewText = "Invalid high"),
      CourseReview(id = "u3_", userId = "u3", courseId = "", rating = 5, reviewText = "No course"),
      CourseReview(id = "u4_c1", userId = "u4", courseId = "c1", rating = 4, reviewText = "Valid")
    )

    val byCourse = CourseRatingHelper.summarizeByCourse(reviews)
    assertEquals(1, byCourse.size)
    val c1 = byCourse.getValue("c1")
    assertEquals(1, c1.reviewCount)
    assertEquals(4.0, c1.averageRating, 0.001)
  }
}
