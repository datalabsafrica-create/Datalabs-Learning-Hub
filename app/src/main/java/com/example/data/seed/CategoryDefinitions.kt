package com.example.data.seed

import com.example.data.model.Course

data class CourseCategorySpec(
  val id: String,
  val name: String,
  val shortDescription: String,
  val iconKey: String,
  val accentHex: String
)

object CategoryDefinitions {

  val categories: List<CourseCategorySpec> = listOf(
    CourseCategorySpec(
      id = "cat_data_analytics",
      name = "Data Analytics",
      shortDescription = "Excel, SQL, Power BI, Python, statistics & analytics portfolios",
      iconKey = "analytics",
      accentHex = "#0284C7"
    ),
    CourseCategorySpec(
      id = "cat_ai",
      name = "Artificial Intelligence",
      shortDescription = "AI foundations, prompt engineering, agents & generative media",
      iconKey = "ai",
      accentHex = "#2563EB"
    ),
    CourseCategorySpec(
      id = "cat_ai_business",
      name = "AI for Business",
      shortDescription = "Workflow automation, office AI, SME productivity & AI marketing",
      iconKey = "ai_business",
      accentHex = "#1D4ED8"
    ),
    CourseCategorySpec(
      id = "cat_entrepreneurship",
      name = "Entrepreneurship",
      shortDescription = "Business ideas, validation, planning, pricing & small enterprises",
      iconKey = "business",
      accentHex = "#0F766E"
    ),
    CourseCategorySpec(
      id = "cat_digital_business",
      name = "Digital Business",
      shortDescription = "Online stores, Jumia/Amazon, digital products & online payments",
      iconKey = "ecommerce",
      accentHex = "#0E7490"
    ),
    CourseCategorySpec(
      id = "cat_freelancing",
      name = "Freelancing",
      shortDescription = "Upwork, Fiverr, remote client proposals, pricing & portfolios",
      iconKey = "freelancing",
      accentHex = "#0369A1"
    ),
    CourseCategorySpec(
      id = "cat_career",
      name = "Career Development",
      shortDescription = "CV writing, LinkedIn, interviews, workplace skills & portfolios",
      iconKey = "career",
      accentHex = "#4338CA"
    ),
    CourseCategorySpec(
      id = "cat_marketing",
      name = "Digital Marketing",
      shortDescription = "Content creation, SEO, social media, copywriting & paid ads",
      iconKey = "marketing",
      accentHex = "#B45309"
    ),
    CourseCategorySpec(
      id = "cat_finance",
      name = "Finance & Business Skills",
      shortDescription = "Budgeting, cash flow, investing literacy, accounting & industry ops",
      iconKey = "finance",
      accentHex = "#047857"
    ),
    CourseCategorySpec(
      id = "cat_tech_productivity",
      name = "Technology & Productivity",
      shortDescription = "Computing, coding, Android/no-code apps, cybersecurity & time mastery",
      iconKey = "tech",
      accentHex = "#15803D"
    )
  )

  fun resolvePrimaryCategory(academyId: String, courseTitle: String): String {
    val lower = courseTitle.lowercase()
    return when {
      academyId == "academy_data" || lower.contains("sql") || lower.contains("power bi") ||
        lower.contains("data analy") || lower.contains("data clean") || lower.contains("data visual") ||
        lower.contains("business intelligence") -> "Data Analytics"

      academyId == "academy_ai" && (
        lower.contains("business") || lower.contains("entrepreneur") ||
          lower.contains("office") || lower.contains("workflow") ||
          lower.contains("automation") || lower.contains("marketing") ||
          lower.contains("freelancer") || lower.contains("professional")
        ) -> "AI for Business"

      academyId == "academy_ai" || lower.contains("chatgpt") ||
        lower.contains("prompt") || lower.contains("ai ") -> "Artificial Intelligence"

      academyId == "academy_freelancing" || lower.contains("freelanc") ||
        lower.contains("upwork") || lower.contains("fiverr") -> "Freelancing"

      academyId == "academy_career" || lower.contains("interview") ||
        lower.contains("cv") || lower.contains("linkedin") ||
        lower.contains("job") || lower.contains("workplace") -> "Career Development"

      academyId == "academy_marketing" || lower.contains("marketing") ||
        lower.contains("seo") || lower.contains("content") ||
        lower.contains("youtube") || lower.contains("social media") -> "Digital Marketing"

      academyId == "academy_ecommerce" || lower.contains("online business") ||
        lower.contains("digital business") || lower.contains("online store") ||
        lower.contains("e-commerce") || lower.contains("jumia") ||
        lower.contains("amazon") || lower.contains("digital products") -> "Digital Business"

      academyId == "academy_money" || academyId == "academy_investing" ||
        lower.contains("finance") || lower.contains("cash flow") ||
        lower.contains("budget") || lower.contains("tax") ||
        lower.contains("invest") -> "Finance & Business Skills"

      academyId == "academy_business" || academyId == "academy_industry" -> "Entrepreneurship"

      else -> "Technology & Productivity"
    }
  }

  fun resolveCategories(academyId: String, courseTitle: String): List<String> {
    val primary = resolvePrimaryCategory(academyId, courseTitle)
    val lower = courseTitle.lowercase()
    val extra = mutableListOf(primary)
    if (lower.contains("business") && primary != "Entrepreneurship") extra += "Entrepreneurship"
    if ((lower.contains("ai") || lower.contains("chatgpt")) && primary != "Artificial Intelligence") {
      extra += "Artificial Intelligence"
    }
    if ((lower.contains("excel") || lower.contains("productivity") || lower.contains("apps")) &&
      primary != "Technology & Productivity"
    ) {
      extra += "Technology & Productivity"
    }
    if ((lower.contains("portfolio") || lower.contains("interview")) && primary != "Career Development") {
      extra += "Career Development"
    }
    return extra.distinct()
  }

  fun coursesForCategory(courses: List<Course>, categoryName: String): List<Course> {
    if (categoryName.isBlank() || categoryName == "All") return courses
    return courses.filter { course ->
      val cats = course.categories.ifEmpty {
        resolveCategories(course.academyId, course.title)
      }
      course.category.equals(categoryName, ignoreCase = true) ||
        cats.any { it.equals(categoryName, ignoreCase = true) }
    }
  }

  fun nonEmptyCategoriesForCourses(courses: List<Course>): List<CourseCategorySpec> =
    categories.filter { cat -> coursesForCategory(courses, cat.name).isNotEmpty() }
}
