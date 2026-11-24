package com.example.data.seed

import com.example.data.model.QuizQuestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GeneratedLessonSpec(
  val suffix: String,
  val moduleTitle: String,
  val title: String,
  val summary: String,
  val orderIndex: Long,
  val durationMinutes: Long,
  val format: String,
  val freePreview: Boolean,
  val notesMarkdown: String,
  val slidesText: String,
  val downloadTitle: String,
  val downloadContent: String,
  val exercisePrompt: String,
  val keyTakeaways: List<String>,
  val caseStudyText: String
)

data class GeneratedCourseCurriculum(
  val primaryCategory: String,
  val categories: List<String>,
  val subtitle: String,
  val description: String,
  val durationMinutes: Long,
  val lessonCount: Long,
  val whatYouWillLearn: List<String>,
  val prerequisites: List<String>,
  val formats: List<String>,
  val tags: List<String>,
  val lessons: List<GeneratedLessonSpec>,
  val moduleQuizTitle: String,
  val moduleQuizDescription: String,
  val moduleQuizQuestionsJson: String,
  val quizTitle: String,
  val quizDescription: String,
  val quizQuestionsJson: String,
  val assignmentTitle: String,
  val assignmentInstructions: String,
  val assignmentDeliverableFormat: String
)

object CourseContentBlueprint {

  private data class DomainKit(
    val toolOrMethod: String,
    val coreFramework: String,
    val workedExample: String,
    val commonMistake: String,
    val tableHeaderMetric: String,
    val tableRows: List<Triple<String, String, String>>,
    val moduleCaseStudies: List<String>,
    val downloadExt: String,
    val downloadBody: (courseTitle: String) -> String,
    val quizTopicQuestion: (courseTitle: String) -> QuizQuestion,
    val quizScenarioQuestion: (courseTitle: String) -> QuizQuestion,
    val moduleConceptQuestion: (courseTitle: String) -> QuizQuestion,
    val moduleTrueFalseQuestion: (courseTitle: String) -> QuizQuestion
  )

  fun buildForCourse(
    academy: SeedAcademySpec,
    courseTitle: String,
    slug: String,
    isFree: Boolean,
    level: String
  ): GeneratedCourseCurriculum {
    AiAcademyCourseCurricula.buildIfAiCourse(
      academy = academy,
      courseTitle = courseTitle,
      slug = slug,
      isFree = isFree,
      level = level
    )?.let { return it }

    val kit = resolveDomainKit(academy.id, courseTitle)
    val primaryCategory = CategoryDefinitions.resolvePrimaryCategory(academy.id, courseTitle)
    val categories = CategoryDefinitions.resolveCategories(academy.id, courseTitle)

    val m1Case = kit.moduleCaseStudies.getOrElse(0) { kit.moduleCaseStudies.first() }
    val m2Case = kit.moduleCaseStudies.getOrElse(1) { m1Case }
    val m3Case = kit.moduleCaseStudies.getOrElse(2) { m1Case }
    val m4Case = kit.moduleCaseStudies.getOrElse(3) { m1Case }

    val lessons = listOf(
      GeneratedLessonSpec(
        suffix = "l1",
        moduleTitle = "Module 1: Core Foundations",
        title = "1. Foundations & Core Principles of $courseTitle",
        summary = "Master why $courseTitle matters, core mental models, and how the ${kit.coreFramework} works in practice.",
        orderIndex = 1L,
        durationMinutes = 35L,
        format = "WRITTEN",
        freePreview = true,
        notesMarkdown = buildModule1Notes(courseTitle, academy.name, academy.instructorName, kit, m1Case),
        slidesText = listOf(
          "Slide 1 — Mental Model: Why mastery of $courseTitle begins with decision clarity before tooling.",
          "Slide 2 — Core Framework: Applying the ${kit.coreFramework} cleanly step by step.",
          "Slide 3 — Primary Tooling: Mastering ${kit.toolOrMethod} with repeatable standards.",
          "Slide 4 — Failure Prevention: Why experienced teams guard against ${kit.commonMistake}.",
          "Slide 5 — Case Study Breakdown: Lessons learned from applying foundations in real organizations."
        ).joinToString("\n"),
        downloadTitle = "${slug}_foundations_guide.txt",
        downloadContent = buildFoundationsDownload(courseTitle, academy.name, kit, m1Case),
        exercisePrompt = "Define one concrete problem in your work or business where applying the ${kit.coreFramework} from $courseTitle eliminates rework this week.",
        keyTakeaways = listOf(
          "Anchor every task in $courseTitle to a clear decision or stakeholder outcome.",
          "Use the ${kit.coreFramework} so outputs remain transparent and repeatable.",
          "Spot early risk around ${kit.commonMistake} before presenting conclusions."
        ),
        caseStudyText = m1Case
      ),
      GeneratedLessonSpec(
        suffix = "l2",
        moduleTitle = "Module 2: Techniques & Worked Walkthrough",
        title = "2. Methods, Worked Examples & Step-by-Step Patterns",
        summary = "Learn ${kit.toolOrMethod} deeply through Before/After comparisons, worked examples, and structured patterns.",
        orderIndex = 2L,
        durationMinutes = 45L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = buildModule2Notes(courseTitle, kit, m2Case),
        slidesText = listOf(
          "Slide 1 — Setup & Inputs: Inspecting raw definitions, units, and assumptions before starting.",
          "Slide 2 — Guided Mechanics: Executing ${kit.toolOrMethod} cleanly in verifiable stages.",
          "Slide 3 — Before vs After Contrast: Why structured inputs beat ad-hoc improvisation.",
          "Slide 4 — Verification Check: Cross-checking results against objective edge cases.",
          "Slide 5 — Applied Walkthrough: Inspecting how teams scale this workflow safely."
        ).joinToString("\n"),
        downloadTitle = "${slug}_practice_kit.${kit.downloadExt}",
        downloadContent = kit.downloadBody(courseTitle),
        exercisePrompt = "Inspect the downloadable ${slug}_practice_kit.${kit.downloadExt} and trace the Before/After pattern on two scenarios from your own context.",
        keyTakeaways = listOf(
          "Inspect raw inputs and assumptions before running calculations or publishing deliverables.",
          "Use standardized patterns from ${kit.toolOrMethod} rather than ad-hoc manual edits.",
          "Document logic cleanly so teammates can verify every parameter change."
        ),
        caseStudyText = m2Case
      ),
      GeneratedLessonSpec(
        suffix = "l3",
        moduleTitle = "Module 3: Debugging, Quality & Edge Cases",
        title = "3. Diagnosing Failures, Edge Cases & Quality Audits",
        summary = "Understand why ${kit.commonMistake} occurs in live projects and master structured troubleshooting playbooks.",
        orderIndex = 3L,
        durationMinutes = 45L,
        format = "WRITTEN",
        freePreview = isFree,
        notesMarkdown = buildModule3Notes(courseTitle, academy.shortName, kit, m3Case),
        slidesText = listOf(
          "Slide 1 — Anatomy of Silent Errors: How subtle defects slip past casual spot-checks.",
          "Slide 2 — Failure Diagnosis: Tracing root causes instead of patching surface symptoms.",
          "Slide 3 — Edge-Case Matrix: Testing boundary inputs, missing values, and scale shifts.",
          "Slide 4 — The 3-Minute Quality Audit: Verifying definitions, math, and limitations.",
          "Slide 5 — Case Recovery: Turning a production failure into durable preventive SOPs."
        ).joinToString("\n"),
        downloadTitle = "${slug}_quality_checklist.txt",
        downloadContent = buildChecklistDownload(courseTitle, kit),
        exercisePrompt = "Walk through a recent deliverable using the 3-Minute Quality Audit and document two edge cases that the audit catches early.",
        keyTakeaways = listOf(
          "Ask why subtle failures occur in real teams and how verification catches them early.",
          "Apply the 3-Minute Quality Audit before sharing any work with managers or clients.",
          "Document assumptions, limitations, and boundary risks explicitly."
        ),
        caseStudyText = m3Case
      ),
      GeneratedLessonSpec(
        suffix = "l4",
        moduleTitle = "Module 4: Production Systems & Strategy",
        title = "4. Scaling $courseTitle in Production & Decision Strategy",
        summary = "Synthesize architecture, governance, stakeholder storytelling, and operational rollout into production-grade practice.",
        orderIndex = 4L,
        durationMinutes = 40L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = buildModule4Notes(courseTitle, academy.name, kit, m4Case),
        slidesText = listOf(
          "Slide 1 — From Pilot to Production: Designing workflows teammates can run without hand-holding.",
          "Slide 2 — Decision Communication: Framing findings as Problem -> Method -> Evidence -> Action.",
          "Slide 3 — Governance & Maintenance: Keeping templates, schemas, and controls current over time.",
          "Slide 4 — Trade-Off Analysis: Balancing speed, accuracy, and stakeholder transparency.",
          "Slide 5 — Mastery Synthesis: Connecting all 4 modules to real career and business impact."
        ).joinToString("\n"),
        downloadTitle = "${slug}_capstone_template.txt",
        downloadContent = buildCapstoneDownload(courseTitle, academy.name, kit),
        exercisePrompt = "Synthesize your architecture decisions from Modules 1–4 into a concise executive rollout brief.",
        keyTakeaways = listOf(
          "Durable systems require clear handoff documentation alongside technical correctness.",
          "Present recommendations concisely: Problem -> Method -> Verification -> Impact.",
          "Continuous monitoring prevents silent drift after initial deployment."
        ),
        caseStudyText = m4Case
      )
    )

    val moduleQuestions = listOf(
      kit.moduleConceptQuestion(courseTitle),
      kit.moduleTrueFalseQuestion(courseTitle),
      QuizQuestion(
        id = "mq3",
        type = "MULTIPLE_CHOICE",
        prompt = "Scenario: You are asked to start work quickly on a $courseTitle task. Based on the ${kit.coreFramework}, what is the right first step?",
        options = listOf(
          "Clarify the exact decision needed, verify input quality, and apply ${kit.toolOrMethod} systematically",
          "Skip input validation and share the very first unverified draft immediately",
          "Copy another team's numbers without checking whether definitions match",
          "Ignore constraints and rework everything only after stakeholders complain"
        ),
        correctAnswer = "Clarify the exact decision needed, verify input quality, and apply ${kit.toolOrMethod} systematically",
        explanation = "Correct! Starting with clear requirements and clean inputs prevents rework and guards against ${kit.commonMistake}. Jumping straight to unverified drafts creates avoidable errors.",
        points = 34
      )
    )

    val finalQuestions = listOf(
      kit.quizTopicQuestion(courseTitle),
      kit.quizScenarioQuestion(courseTitle),
      QuizQuestion(
        id = "q3",
        type = "TRUE_FALSE",
        prompt = "True or False: In $courseTitle, running a structured check against ${kit.commonMistake} before sharing final deliverables protects stakeholder trust and decision accuracy.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        explanation = "True. Quality control catches subtle mistakes early before they reach clients, customers, or managers—making True the reliable professional choice.",
        points = 25
      ),
      QuizQuestion(
        id = "q4",
        type = "MULTIPLE_CHOICE",
        prompt = "What is the strongest way to demonstrate real competence to employers or clients after completing $courseTitle?",
        options = listOf(
          "Produce a documented real-world deliverable using ${kit.toolOrMethod} with clear verification",
          "Skim lesson summaries quickly without completing practice tasks",
          "Copy unverified sample numbers directly into a client proposal",
          "Skip quality audits and leave assumptions undocumented"
        ),
        correctAnswer = "Produce a documented real-world deliverable using ${kit.toolOrMethod} with clear verification",
        explanation = "Documented, verified deliverables prove applied skill and reliability; passive reading or unverified copying fails real-world scrutiny.",
        points = 25
      )
    )

    val learnPoints = listOf(
      "Apply the ${kit.coreFramework} cleanly across real $courseTitle scenarios",
      "Execute hands-on workflows with ${kit.toolOrMethod} using guided templates",
      "Identify and prevent ${kit.commonMistake} through structured quality checks",
      "Deliver a verified capstone project ready for portfolios, jobs, or business growth"
    )

    val formats = buildList {
      add("WRITTEN")
      add("SLIDES")
      if (academy.id == "academy_data") add("PDF") else add("TEMPLATE")
      add("EXERCISE")
      add("QUIZ")
      add("ASSIGNMENT")
      add("PROJECT")
    }

    val durationMinutes = lessons.sumOf { it.durationMinutes }

    return GeneratedCourseCurriculum(
      primaryCategory = primaryCategory,
      categories = categories,
      subtitle = "Hands-on training in ${academy.shortName} using ${kit.toolOrMethod}",
      description = "$courseTitle delivers comprehensive, practical mastery inside ${academy.name} ($primaryCategory). Led by ${academy.instructorName}, the program spans 4 structured modules—combining deep conceptual explanations, comparison tables, worked case studies, downloadable practice kits, a Module Checkpoint Quiz, a Practical Portfolio Assignment, and a Final Course Examination leading to a verified Datalabs certificate.",
      durationMinutes = durationMinutes,
      lessonCount = lessons.size.toLong(),
      whatYouWillLearn = learnPoints,
      prerequisites = listOf(
        when (level) {
          "BEGINNER" -> "No prior experience required; bring curiosity and willingness to practice"
          "ADVANCED" -> "Solid foundational experience in ${academy.shortName} recommended"
          else -> "Basic familiarity with ${academy.shortName} concepts"
        }
      ),
      formats = formats,
      tags = (categories + listOf(
        academy.shortName,
        level.lowercase().replaceFirstChar { it.uppercase() },
        if (isFree) "Free" else "Premium",
        "Certificate"
      )).distinct(),
      lessons = lessons,
      moduleQuizTitle = "$courseTitle — Module 1–2 Checkpoint Quiz",
      moduleQuizDescription = "Check your grasp of Module 1 foundations and Module 2 practical methods (passing score 70%, detailed feedback included).",
      moduleQuizQuestionsJson = Json.encodeToString(moduleQuestions),
      quizTitle = "$courseTitle — Final Course Examination",
      quizDescription = "Complete all 4 final exam questions (passing score 70%) covering applied scenarios, quality checks, and capstone readiness to unlock certification.",
      quizQuestionsJson = Json.encodeToString(finalQuestions),
      assignmentTitle = "$courseTitle — Practical Portfolio Deliverable",
      assignmentInstructions = "Using the downloadable practice kit and capstone template in Lessons 2 & 4, complete an applied mini-project for $courseTitle. Your submission must clearly state: (1) Objective & Context, (2) Steps executed using ${kit.toolOrMethod}, (3) How you verified outputs against ${kit.commonMistake}, and (4) Measurable result or key takeaway.",
      assignmentDeliverableFormat = "Structured Summary + Optional Link (100 pts)"
    )
  }

  private fun resolveDomainKit(academyId: String, courseTitle: String): DomainKit {
    val lower = courseTitle.lowercase()
    return when {
      lower.contains("sql") -> DomainKit(
        toolOrMethod = "Relational SQL Queries (SELECT, JOIN, GROUP BY, CTEs)",
        coreFramework = "Grain-Filter-Aggregate Query Design Pattern",
        workedExample = """
          ```sql
          SELECT
            c.region,
            COUNT(DISTINCT o.order_id) AS total_orders,
            SUM(o.amount_ugx) AS total_revenue
          FROM orders o
          INNER JOIN customers c ON o.customer_id = c.customer_id
          WHERE o.order_date >= '2026-01-01' AND o.status = 'COMPLETED'
          GROUP BY c.region
          ORDER BY total_revenue DESC;
          ```
        """.trimIndent(),
        commonMistake = "many-to-many join duplication inflating totals",
        tableHeaderMetric = "SQL Query Stage",
        tableRows = listOf(
          Triple("Row Filtering (WHERE)", "Filter status & date window before joining or grouping", "Filtering aggregates in WHERE instead of HAVING"),
          Triple("Joining (INNER/LEFT)", "Verify primary-key to foreign-key grain first", "Joining unaggregated child rows causing duplicate totals"),
          Triple("Aggregation (GROUP BY)", "Use COUNT(DISTINCT id) alongside SUM()", "Selecting non-aggregated columns omitted from GROUP BY")
        ),
        moduleCaseStudies = listOf(
          "A Kampala distribution team noticed monthly revenue looked 3x too high in an executive dashboard. Auditing the SQL query revealed orders joined directly to a multi-note customer_notes table before grouping, duplicating every order once per note.",
          "An e-commerce ops analyst replaced slow correlated subqueries with a staged WITH cte_daily_orders block, cutting weekly cohort reporting runtime from 14 minutes to 11 seconds while making business filters easy to verify.",
          "A regional clinic billing report silently omitted 420 insurance claims because an INNER JOIN on patient_plan_id dropped patients with NULL optional secondary IDs. Switching to LEFT JOIN plus COALESCE(plan_name, 'Self-Pay') restored 100% revenue reconciliation.",
          "A fintech finance team instituted automated pre-publish SQL row-count and SUM invariants comparing staging ledger tables against aggregated executive views, catching duplicate webhook inserts before monthly investor reporting."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "order_id,customer_id,region,order_date,status,amount_ugx\n1001,C01,Kampala,2026-09-01,COMPLETED,450000\n1002,C02,Entebbe,2026-09-02,COMPLETED,320000\n1003,C01,Kampala,2026-09-04,COMPLETED,510000\n1004,C03,Nairobi,2026-09-05,CANCELLED,180000\n1005,C04,Mbarara,2026-09-07,COMPLETED,275000"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, which clause filters grouped aggregate rows (such as SUM(revenue) > 100000) after GROUP BY?",
            options = listOf("HAVING", "WHERE", "ORDER BY", "DISTINCT"),
            correctAnswer = "HAVING",
            explanation = "HAVING filters groups after aggregation completes. WHERE runs earlier on raw rows, ORDER BY only sorts results, and DISTINCT removes duplicate output rows.",
            points = 25
          )
        },
        quizScenarioQuestion = { title ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: When applying $title to join an orders table with a customer_notes table where one customer has multiple notes, what prevents double-counting revenue?",
            options = listOf(
              "Aggregate or deduplicate customer_notes to one row per customer before joining",
              "Remove the ON join condition completely",
              "Use SELECT * without checking row counts",
              "Replace SUM with COUNT(*)"
            ),
            correctAnswer = "Aggregate or deduplicate customer_notes to one row per customer before joining",
            explanation = "Pre-aggregating to matching granularity keeps one row per order. Removing ON causes a Cartesian explosion, SELECT * hides row multiplication, and COUNT(*) counts rows rather than revenue.",
            points = 25
          )
        },
        moduleConceptQuestion = { title ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why should you inspect the row count before and after adding a JOIN?",
            options = listOf(
              "To catch unexpected row multiplication caused by one-to-many or many-to-many keys",
              "Because SQL automatically deletes rows larger than 100",
              "To rename every table column alphabetically",
              "Because INNER JOIN disables WHERE filters"
            ),
            correctAnswer = "To catch unexpected row multiplication caused by one-to-many or many-to-many keys",
            explanation = "Checking row counts confirms join granularity immediately; SQL does not delete large rows or disable WHERE filters.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Common Table Expressions (CTEs / WITH clauses) help break complex multi-step queries into readable, auditable blocks.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. CTEs isolate staging, deduplication, and final aggregation clearly so teammates can audit logic step by step.",
            points = 33
          )
        }
      )

      lower.contains("excel") -> DomainKit(
        toolOrMethod = "Structured Tables, XLOOKUP, SUMIFS & PivotTables",
        coreFramework = "Raw Data -> Clean Staging -> Summary Model Architecture",
        workedExample = """
          - Dynamic Lookup: `=XLOOKUP(A2, Clients[ClientID], Clients[Region], "Unassigned")`
          - Multi-Condition Total: `=SUMIFS(Sales[Revenue], Sales[Region], "Kampala", Sales[Status], "Paid")`
          - Always press `Ctrl + T` to convert ranges into named Excel Tables before building PivotTables.
        """.trimIndent(),
        commonMistake = "hardcoding numbers inside formulas or breaking lookups with trailing spaces",
        tableHeaderMetric = "Spreadsheet Practice",
        tableRows = listOf(
          Triple("Data Storage (Ctrl+T)", "Keep one header row, zero merged cells, tabular records", "Mixing totals inside raw input rows or merging headers"),
          Triple("Clean Keys", "Apply TRIM() & consistent number/text types", "Hidden trailing spaces causing unexplained #N/A lookups"),
          Triple("Formula Hygiene", "Reference dedicated parameter cells instead of magic numbers", "Typing hard-coded tax or FX rates inside 50 cells")
        ),
        moduleCaseStudies = listOf(
          "A finance assistant spent 3 hours every Friday manually fixing broken VLOOKUPs across 400 invoices. Converting ranges into named Excel Tables (Ctrl+T) and separating Raw Data from Summary tabs stopped range-drift bugs immediately.",
          "Switching legacy positional VLOOKUP calls to XLOOKUP paired with TRIM() on client IDs eliminated 18 mysterious #N/A errors caused by trailing spaces in exported bank CSV files.",
          "Auditing a quarterly budget sheet uncovered hardcoded tax percentages inside 64 separate formula cells. Moving the tax rate to one dedicated yellow Assumptions input cell eliminated stale calculation discrepancies across departments.",
          "A logistics coordinator standardized weekly fleet reporting into a 3-tab workbook (Raw_Dispatch, Clean_Staging, Executive_KPIs) with a pre-send control total check, cutting Friday prep from 3 hours to 12 minutes."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "invoice_id,client_name,region,category,units,unit_price_usd,status\nINV-01,Acacia Retail,Kampala,Consulting,4,250,Paid\nINV-02,Nile Logistics,Jinja,Training,10,120,Paid\nINV-03,Savannah Foods,Nairobi,Audit,2,450,Pending\nINV-04,Acacia Retail,Kampala,Analytics,6,180,Paid"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why is converting a dataset into an Excel Table (Ctrl+T) recommended before reporting?",
            options = listOf(
              "Formulas and PivotTables expand automatically when new rows are added",
              "It converts numeric columns into plain text strings",
              "It disables all conditional formatting rules",
              "It removes the ability to sort or filter columns"
            ),
            correctAnswer = "Formulas and PivotTables expand automatically when new rows are added",
            explanation = "Structured Tables adjust ranges dynamically on new rows. They preserve numeric types, formatting, and sorting/filtering.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A revenue lookup returns #N/A for an ID that visibly matches. Which cleaning step most often fixes hidden mismatch issues?",
            options = listOf(
              "Use TRIM() and ensure both keys share the same text/number data type",
              "Change the sheet background color",
              "Merge header cells across columns",
              "Hide empty rows manually"
            ),
            correctAnswer = "Use TRIM() and ensure both keys share the same text/number data type",
            explanation = "Trailing spaces and text-vs-number storage are the leading causes of lookup mismatches; visual formatting like shading or merged headers never fixes key matching.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "When designing a maintainable workbook, why should assumptions (such as discount % or tax rate) live in their own input cells rather than inside formulas?",
            options = listOf(
              "You can update the rule once cleanly and audit assumptions visually",
              "Excel forbids numbers inside arithmetic formulas",
              "It prevents PivotTables from refreshing",
              "It deletes formula dependencies automatically"
            ),
            correctAnswer = "You can update the rule once cleanly and audit assumptions visually",
            explanation = "Dedicated assumption cells make models auditable and prevent stale hardcoded figures when rates change.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Keeping raw transactional data separate from summary dashboard tabs reduces accidental overrides and broken formulas.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Separating Raw Data, Clean Staging, and Summary views is the foundation of reliable spreadsheet modeling.",
            points = 33
          )
        }
      )

      lower.contains("power bi") || lower.contains("business intelligence") || lower.contains("visualization") -> DomainKit(
        toolOrMethod = "Star Schema Modeling, Power Query & Core DAX Measures",
        coreFramework = "Fact-Dimension Star Schema & Decision-First Visual Hierarchy",
        workedExample = """
          - Keep one central Fact Table (`FactSales`) linked via 1-to-many single-direction relationships to Dimension Tables (`DimDate`, `DimProduct`, `DimRegion`).
          - Core DAX:
            `Total Revenue = SUM(FactSales[Amount])`
            `Revenue YoY % = DIVIDE([Total Revenue] - [PY Revenue], [PY Revenue])`
        """.trimIndent(),
        commonMistake = "loading flat unmodeled spreadsheets or using cluttered 3D pie charts",
        tableHeaderMetric = "BI Modeling & Visual Choice",
        tableRows = listOf(
          Triple("Data Model", "Star Schema (Facts + Dimensions with 1:* filters)", "Bidirectional many-to-many chains that ambiguate totals"),
          Triple("Comparisons", "Sorted horizontal bars with direct data labels", "3D pie charts or rainbow gauges hiding tiny differences"),
          Triple("Trends Over Time", "Clean line chart anchored to a proper DimDate table", "Sorting month names alphabetically instead of by date")
        ),
        moduleCaseStudies = listOf(
          "A regional logistics manager could not explain a Q3 drop in on-time deliveries because one giant flat spreadsheet caused conflicting visual filters. Splitting into FactDeliveries and clean Dimension tables fixed cross-filtering instantly.",
          "Standardizing Power Query ingestion steps (Trim, Date Type, Currency Normalization) eliminated manual Monday morning CSV cleanups prior to refreshing management dashboards.",
          "Replacing alphabetical month labels ('April, August, December...') and 14 overlapping gauges with a chronological DimDate table and 3 sorted horizontal bar charts helped executives pinpoint a depot delay in two minutes.",
          "An analytics lead implemented explicit DIVIDE() DAX measures and a standardized KPI Definition Card inside every executive report, ending meetings wasted arguing over conflicting margin numbers."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "date,product_id,region,channel,revenue_usd,cost_usd\n2026-08-01,P101,East Africa,Direct,4200,2500\n2026-08-02,P102,West Africa,Partner,3100,1900\n2026-08-03,P101,East Africa,Online,2850,1500\n2026-08-04,P103,Global,Online,5400,2900"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why do analytics teams organize tables into a Star Schema rather than one giant flat table?",
            options = listOf(
              "It ensures unambiguous filter propagation, faster queries, and accurate DAX measures",
              "It forces every chart to use a pie layout",
              "It prevents dates from being filtered by month or quarter",
              "It eliminates the need for primary keys"
            ),
            correctAnswer = "It ensures unambiguous filter propagation, faster queries, and accurate DAX measures",
            explanation = "Star schemas separate quantitative events (Facts) from lookup attributes (Dimensions), keeping filters fast and predictable.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: An executive asks to compare monthly revenue across 6 regions cleanly. Which chart choice gives the clearest reading?",
            options = listOf(
              "A sorted horizontal bar chart or small-multiples line chart with direct data labels",
              "A 3D exploded pie chart with 18 slices",
              "A radar chart with overlapping polygons",
              "An unsorted multi-color bubble table"
            ),
            correctAnswer = "A sorted horizontal bar chart or small-multiples line chart with direct data labels",
            explanation = "Aligned bars and clean small-multiple lines allow exact visual comparison; 3D slices and overlapping polygons distort proportions.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Why should repetitive cleaning transformations (trimming text, fixing dates) happen in Power Query before visuals are built?",
            options = listOf(
              "Cleaning runs automatically on every data refresh without manual rework",
              "Power Query replaces the need for accurate source data",
              "It hides numeric decimals permanently",
              "Visuals cannot render bar charts without pie overlays"
            ),
            correctAnswer = "Cleaning runs automatically on every data refresh without manual rework",
            explanation = "Repeatable ETL steps in Power Query guarantee fresh data always enters the model clean.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Using explicit DAX measures like DIVIDE() prevents division-by-zero errors cleanly across filtered cards.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. DIVIDE() safely handles zero denominators instead of surfacing NaN or Infinity errors in reports.",
            points = 33
          )
        }
      )

      lower.contains("python") || lower.contains("coding") || lower.contains("android") -> DomainKit(
        toolOrMethod = "Modular Functions, Data Validation & Clean Program Structure",
        coreFramework = "Input Validation -> Core Transformation -> Automated Assertions",
        workedExample = """
          ```python
          import pandas as pd

          df = pd.read_csv("sales_clean.csv").drop_duplicates()
          df["margin"] = df["revenue"] - df["cost"]
          summary = df.groupby("region", as_index=False)[["revenue", "margin"]].sum()
          print(summary.sort_values("revenue", ascending=False))
          ```
        """.trimIndent(),
        commonMistake = "mutating raw inputs without checks or ignoring edge-case nulls",
        tableHeaderMetric = "Engineering Stage",
        tableRows = listOf(
          Triple("Input Parsing", "Check required columns, types, and non-null keys", "Assuming external files never change format"),
          Triple("Pure Functions", "Keep transformations small with explicit parameters", "Writing a single 300-line script mixing I/O and logic"),
          Triple("Verification", "Assert row counts, invariants, and edge cases", "Running code once on ideal input and skipping error logs")
        ),
        moduleCaseStudies = listOf(
          "A startup's nightly billing script mixed file parsing, tax math, and email sending in a single 350-line function. Refactoring into pure functions with explicit schemas made failures immediately localized and testable.",
          "Replacing manual dictionary key indexing with explicit schema checks and structured Pandas aggregation pipelines cut ad-hoc CSV reporting work by 80% without brittle side effects.",
          "When a partner CSV omitted an optional discount column, the team's ingestion job silently wrote wrong totals. Adding explicit column assertions and null-handling logs caught missing headers before invoices went out.",
          "Adding a 6-test edge-case suite covering empty inputs, zero prices, and duplicate IDs cut production hotfixes by 75% across quarterly releases."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "id,region,revenue,cost,units\n1,Kampala,1200,720,12\n2,Nairobi,1850,1100,19\n3,Kigali,940,560,9\n4,Kampala,1200,720,12"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "When implementing $title workflows, why should data loading and transformation logic be separated into small, testable steps?",
            options = listOf(
              "It makes debugging, edge-case testing, and reuse reliable and transparent",
              "It prevents variables from storing strings or lists",
              "It doubles execution errors intentionally",
              "It removes the need for documentation"
            ),
            correctAnswer = "It makes debugging, edge-case testing, and reuse reliable and transparent",
            explanation = "Small modular functions can be tested independently with edge cases, making failures fast to isolate and fix.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A script crashes when an input column contains missing values (NaN/null). What is the right engineering fix?",
            options = listOf(
              "Validate schema explicitly, handle nulls intentionally (drop or impute with rule), and log rows affected",
              "Delete random lines from the source file manually",
              "Ignore all exceptions silently without logging",
              "Hardcode the first row's value for every error"
            ),
            correctAnswer = "Validate schema explicitly, handle nulls intentionally (drop or impute with rule), and log rows affected",
            explanation = "Explicit null handling with audit logging keeps pipelines safe and explainable; silent catches or manual edits hide data corruption.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "What is the primary benefit of keeping raw source files immutable (read-only) during processing?",
            options = listOf(
              "You can always trace discrepancies back and re-run transformations safely",
              "Read-only files execute 100x faster automatically",
              "It removes the need to test transformations",
              "It converts JSON directly into PDF format"
            ),
            correctAnswer = "You can always trace discrepancies back and re-run transformations safely",
            explanation = "Preserving raw inputs guarantees reproducibility whenever business rules or bug fixes require re-processing.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Writing clear assertions or unit tests on edge cases (empty lists, zeros, duplicates) saves hours during production releases.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Early automated checks catch boundary bugs long before end users or clients encounter them.",
            points = 33
          )
        }
      )

      academyId == "academy_ai" -> DomainKit(
        toolOrMethod = "Role-Context-Task-Format-Verification Prompt Architecture",
        coreFramework = "5-Part Structured Prompt & Human-in-the-Loop Verification",
        workedExample = """
          **Structured Prompt Blueprint:**
          - **Role:** Act as an operations analyst for a growing SME.
          - **Context:** We run 3 retail branches and track weekly stockouts.
          - **Task:** Draft a 5-point weekly inventory review SOP.
          - **Constraints:** Keep under 250 words; use plain checklist language; flag assumptions.
          - **Format:** Markdown table + 3 immediate actions.
        """.trimIndent(),
        commonMistake = "accepting unverified AI outputs or pasting confidential client PII into public tools",
        tableHeaderMetric = "Prompt & Workflow Habit",
        tableRows = listOf(
          Triple("Context & Constraints", "Supply audience, scope, tone, and output schema", "One-line vague requests expecting mind-reading"),
          Triple("Privacy Hygiene", "Sanitize names, IDs, credentials, and private data", "Pasting sensitive customer records or API keys"),
          Triple("Fact Verification", "Check statistics, legal terms, and code manually", "Publishing confident-sounding AI text unverified")
        ),
        moduleCaseStudies = listOf(
          "An operations team noticed inconsistent chatbot replies whenever managers pasted vague 4-word requests. Adopting the 5-part Role-Context-Task-Constraints-Format structure turned one-off drafts into repeatable client-ready memos.",
          "Adding concrete few-shot examples and XML <context> tags around sensitive SOP snippets raised team formatting consistency above 96% across weekly reports.",
          "A consulting agency caught two outdated tax percentages generated in an unreviewed AI draft because they mandated a 3-point primary-source verification pass before every client send.",
          "Packaging top-performing workflows into a shared 5-card team prompt library with explicit review owners saved 45% of weekly admin prep while keeping governance auditable."
        ),
        downloadExt = "txt",
        downloadBody = { title ->
          "DATALABS AI PLAYBOOK — $title\n\nPROMPT TEMPLATE:\nRole: [Specific Expert Role]\nContext: [Audience, Goals & Constraints]\nInput Data: [Sanitized Non-Sensitive Facts]\nTask: [Exact Deliverable]\nVerification: [Ask model to list assumptions & uncertainties]\n"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "Which component in $title most improves output relevance and reduces vague generic replies?",
            options = listOf(
              "Providing clear context, audience constraints, and an explicit output format",
              "Writing a single ambiguous 3-word prompt",
              "Uploading private customer passwords for context",
              "Never reviewing generated figures or citations"
            ),
            correctAnswer = "Providing clear context, audience constraints, and an explicit output format",
            explanation = "Clear context and output constraints narrow the solution space directly to what you need, while ambiguous prompts produce generic filler.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: An AI assistant drafts a legal or financial summary containing a statistic you did not supply. What must you do before publishing?",
            options = listOf(
              "Fact-check every statistic against an authoritative primary source first",
              "Publish immediately because AI text sounds confident",
              "Assume the percentage is accurate if it has two decimal places",
              "Remove the title and keep the unverified claim"
            ),
            correctAnswer = "Fact-check every statistic against an authoritative primary source first",
            explanation = "Language models predict probable phrasing, not verified truth; always validate numbers, regulations, and citations against primary sources.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Before pasting a real company report into a general AI tool, what security step comes first?",
            options = listOf(
              "Strip personal identifiers (PII), bank details, and confidential client names",
              "Increase font size so headers stand out",
              "Convert PDFs to screenshots so passwords stay embedded",
              "Share internal credentials in the system prompt"
            ),
            correctAnswer = "Strip personal identifiers (PII), bank details, and confidential client names",
            explanation = "Data minimization protects client confidentiality and keeps internal workflows compliant.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Asking an AI assistant to list its assumptions and missing data helps surface weak spots before you rely on its draft.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Requesting explicit assumptions forces uncertainties into the open so human reviewers can verify them.",
            points = 33
          )
        }
      )

      academyId == "academy_data" -> DomainKit(
        toolOrMethod = "Data Profiling, Cleaning Logs & Decision-Ready Visuals",
        coreFramework = "Ask -> Prepare -> Clean -> Analyze -> Communicate",
        workedExample = """
          - **Step 1 (Profile):** Check row count, distinct IDs, min/max dates, and % nulls per column.
          - **Step 2 (Clean):** Standardize date formats (`YYYY-MM-DD`), currency units, and category casing.
          - **Step 3 (Communicate):** Lead every insight with *What changed*, *Why it matters*, and *Recommended action*.
        """.trimIndent(),
        commonMistake = "analyzing uncleaned duplicates or reporting averages without checking outliers",
        tableHeaderMetric = "Analytics Workflow Phase",
        tableRows = listOf(
          Triple("Profiling & Audits", "Log missing %, duplicates, and unexpected category values", "Building charts before checking row counts or dates"),
          Triple("Statistical Context", "Show median + count (n) alongside arithmetic mean", "Summarizing skewed data using only an average"),
          Triple("Decision Communication", "Lead with insight headline, evidence, and action", "Sending raw spreadsheets without executive interpretation")
        ),
        moduleCaseStudies = listOf(
          "A clinic network saw average patient wait times double on a monthly scorecard. Profiling row counts and null rates before calculating averages revealed 11 clerical rows entered a month late.",
          "Replacing unskewed mean-only summaries with median + count (n) + interquartile context exposed that 92% of branches were actually running faster than prior quarter.",
          "Maintaining a lightweight 4-line Cleaning Log documenting deduplication rules and negative-quantity return filters resolved an executive audit question in under three minutes.",
          "Reframing dense 12-tab spreadsheets into a 3-part Executive Brief (What Changed -> Why It Matters -> Recommended Action) doubled stakeholder follow-through on inventory decisions."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "date,region,category,revenue_ugx,units_sold,customer_segment\n2026-10-01,Kampala,Analytics Training,1450000,12,Enterprise\n2026-10-02,Nairobi,Business Advisory,980000,8,SME\n2026-10-03,Entebbe,Cloud Tools,620000,5,Startup\n2026-10-04,Kampala,AI Workflows,2100000,15,Enterprise\n2026-10-05,Kigali,Data Cleaning,840000,7,SME"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why should analysts check median and distribution alongside the arithmetic mean?",
            options = listOf(
              "Extreme outliers can skew the average and misrepresent a typical case",
              "The mean is always equal to the minimum value",
              "Medians remove the need to inspect missing rows",
              "Visualizations cannot display averages"
            ),
            correctAnswer = "Extreme outliers can skew the average and misrepresent a typical case",
            explanation = "Means are sensitive to huge or erroneous extremes, whereas medians reflect the central experience accurately.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A dataset shows negative quantities on 14 sales rows. What is the best analytical practice?",
            options = listOf(
              "Investigate whether negative quantities represent product returns or data-entry bugs before aggregating",
              "Multiply every negative number by 10 silently",
              "Delete the entire dataset and guess totals",
              "Report the file as 100% clean without checking"
            ),
            correctAnswer = "Investigate whether negative quantities represent product returns or data-entry bugs before aggregating",
            explanation = "Domain investigation tells you whether negative quantities reflect legitimate refunds/credits or import defects.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Why do professional analysts maintain a short Cleaning Log (steps taken & rows modified)?",
            options = listOf(
              "It ensures audits, handoffs, and executive numbers remain traceable and transparent",
              "It automatically translates files into SQL tables",
              "It reduces sample size to zero rows",
              "It prevents charts from showing axis labels"
            ),
            correctAnswer = "It ensures audits, handoffs, and executive numbers remain traceable and transparent",
            explanation = "Documenting every rule (e.g., rows deduplicated or currency standardized) builds stakeholder confidence in your totals.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Leading a business insight with 'What changed, Why it matters, and Recommended action' makes reports far more useful to decision-makers.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Decision-makers want clear answers and next actions first, supported underneath by clean charts and data.",
            points = 33
          )
        }
      )

      academyId == "academy_business" || academyId == "academy_industry" -> DomainKit(
        toolOrMethod = "Unit Economics Sheet, Customer Discovery & Cash-Flow Tracking",
        coreFramework = "Validate Demand -> Price for Margin -> Control Cash -> Deliver Consistently",
        workedExample = """
          - **Unit Economics Formula:**
            `Contribution Margin = Selling Price - Direct Variable Cost`
            `Break-Even Units = Monthly Fixed Costs / Contribution Margin`
          - Example: If Price = $25, Variable Cost = $15, Margin = $10. With $500 fixed rent/staff, you need 50 units/month to break even.
        """.trimIndent(),
        commonMistake = "confusing total sales revenue with net profit or underpricing fixed overheads",
        tableHeaderMetric = "Enterprise Discipline",
        tableRows = listOf(
          Triple("Demand Validation", "Test small paid pre-orders or interviews first", "Spending all capital on inventory before buyer proof"),
          Triple("Unit Pricing", "Cover direct cost + fixed overhead + healthy margin", "Copying competitor prices without knowing your own cost"),
          Triple("Cash Management", "Track daily inflows/outflows separately from personal cash", "Treating cash register receipts as personal spending money")
        ),
        moduleCaseStudies = listOf(
          "A neighborhood bakery doubled daily loaf volume yet still ran low on cash for flour deliveries. Separating personal draws from the shop ledger exposed hidden owner withdrawals distorting profit.",
          "Calculating true unit economics (Selling Price minus ingredients, fuel, packaging, and spoilage) showed one discounted pastry bundle lost $0.40 on every unit sold.",
          "Auditing overdue B2B catering receivables and renegotiating 50% upfront deposits converted chronic end-of-month cash shortages into steady positive working capital in 6 weeks.",
          "Documenting weekly inventory counts, reorder points, and standardized supplier check-ins allowed the founder to delegate morning opening shifts reliably without quality drops."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "item,category,monthly_budget_usd,actual_usd,notes\nProduct Raw Materials,Variable Cost,800,840,Supplier A bulk order\nPackaging & Delivery,Variable Cost,250,235,Local courier rates\nShop/Workshop Rent,Fixed Cost,300,300,Monthly lease\nMarketing & Outreach,Fixed Cost,120,115,Referral flyers & local ads"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, what determines the break-even sales volume for a product or service?",
            options = listOf(
              "Fixed costs divided by unit contribution margin (Price minus Variable Cost)",
              "Total revenue multiplied by employee count",
              "Only the logo design budget",
              "Gross sales ignoring supplier costs"
            ),
            correctAnswer = "Fixed costs divided by unit contribution margin (Price minus Variable Cost)",
            explanation = "Break-even shows exactly how many units cover fixed overheads once variable costs per item are subtracted.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A small enterprise has rising sales every month yet runs out of cash to restock. What is the most likely cause?",
            options = listOf(
              "Slow customer collections, thin margins, or inventory tying up working capital",
              "Having too many satisfied repeat customers",
              "Recording receipts in a ledger",
              "Maintaining an emergency reserve fund"
            ),
            correctAnswer = "Slow customer collections, thin margins, or inventory tying up working capital",
            explanation = "Sales growth can drain cash if customers pay late or margins fail to replenish working capital fast enough.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Why should a new business owner validate customer willingness to pay before committing large fixed capital?",
            options = listOf(
              "Real buyer feedback and small test orders prove genuine demand at viable margins",
              "Customer feedback eliminates the need for quality delivery",
              "Fixed rent always decreases when demand is unknown",
              "Marketing cannot work if demand already exists"
            ),
            correctAnswer = "Real buyer feedback and small test orders prove genuine demand at viable margins",
            explanation = "Early customer validation saves scarce capital and guides product fit before scaling.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Keeping business money separate from personal household spending is essential for measuring true profitability.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Mixing personal and business accounts hides true margins, tax obligations, and working capital needs.",
            points = 33
          )
        }
      )

      academyId == "academy_money" || academyId == "academy_investing" -> DomainKit(
        toolOrMethod = "50/30/20 Cash Allocation, Emergency Buffer & Risk Due Diligence",
        coreFramework = "Track Cash Flow -> Clear High-Interest Debt -> Build Buffer -> Diversify Long-Term",
        workedExample = """
          - **Educational Disclaimer:** Datalabs Learning Hub teaches objective financial literacy and risk evaluation—not personalized financial advice.
          - **Risk Checklist (Anti-Scam Rule):** Any scheme promising "guaranteed 20% monthly returns with zero risk" fails basic financial verification.
          - **Compound Growth Rule of 72:** `Years to Double ≈ 72 / Annual Return %` (e.g., at 8%/year, ~9 years).
        """.trimIndent(),
        commonMistake = "chasing guaranteed high-return schemes or investing emergency savings into illiquid assets",
        tableHeaderMetric = "Financial Foundation",
        tableRows = listOf(
          Triple("Cash Reserve", "Keep 3–6 months of essentials liquid & accessible", "Locking emergency funds into volatile speculative bets"),
          Triple("Debt Priority", "Pay down high-interest consumer loans aggressively", "Earning 6% savings while paying 24%+ loan interest"),
          Triple("Due Diligence", "Verify licensing, fees, liquidity, and downside risk", "Trusting referrals or 'guaranteed profit' promises blindly")
        ),
        moduleCaseStudies = listOf(
          "Two early-career engineers earned identical salaries; tracking every shilling for 30 days helped one eliminate unnoticed subscription and impulse leaks worth 18% of monthly income.",
          "Building a dedicated 3-month liquid cash reserve in a regulated account prevented one professional from selling long-term holdings at a loss during a sudden family medical bill.",
          "Applying a 4-point regulatory & fee due-diligence checklist exposed an unregulated '30% guaranteed monthly profit' chat group four months before the scheme collapsed.",
          "Comparing low-cost diversified long-term compounding against high-fee short-term trading showed how minimizing 2%+ annual drag compounds dramatically over 10+ years."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "bucket,target_pct,monthly_amount_usd,purpose\nEssential Needs,50,500,Housing food utilities transport\nSavings & Emergency Fund,20,200,3-6 month liquid safety buffer\nGrowth / Skill Building,15,150,Courses tools diversified long-term assets\nFlexible Wants,15,150,Discretionary lifestyle"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why should an emergency cash buffer be built before committing funds to higher-risk investments?",
            options = listOf(
              "It prevents having to sell long-term investments at a loss during unexpected emergencies",
              "Because savings accounts always forbid withdrawals",
              "To eliminate the need for budgeting",
              "Because diversification increases short-term debt"
            ),
            correctAnswer = "It prevents having to sell long-term investments at a loss during unexpected emergencies",
            explanation = "Liquid cash protects your long-term assets from forced panic selling when urgent medical or income shocks happen.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: Someone pitches an unregulated platform guaranteeing '30% profit every month with zero risk'. What is the right response?",
            options = listOf(
              "Treat it as a major scam red flag and verify licensing with official financial regulators",
              "Borrow high-interest loans to deposit immediately",
              "Skip checking how returns are generated",
              "Share your banking PIN to speed up signup"
            ),
            correctAnswer = "Treat it as a major scam red flag and verify licensing with official financial regulators",
            explanation = "Real returns always carry proportional risk; 'guaranteed extraordinary returns with zero risk' is the classic hallmark of financial fraud.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Using the Rule of 72 (72 / Annual Return %), roughly how long does money take to double at an average 8% annual return?",
            options = listOf(
              "About 9 years",
              "About 9 months",
              "Exactly 72 years",
              "About 2 years"
            ),
            correctAnswer = "About 9 years",
            explanation = "72 divided by 8 equals approximately 9 years—illustrating how compound growth rewards patience and consistency.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: High fees and high-interest debt compound against you just as steadily as long-term returns compound for you.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Clearing high-interest debt and minimizing hidden fees boosts net wealth faster than chasing risky yields.",
            points = 33
          )
        }
      )

      academyId == "academy_career" || academyId == "academy_freelancing" -> DomainKit(
        toolOrMethod = "Impact-Metric CV Bullets, Client Discovery & Scope Proposals",
        coreFramework = "Action + Context + Measurable Result (CAR / STAR Method)",
        workedExample = """
          - **Weak Bullet:** "Worked on Excel reports for the sales team."
          - **Strong Outcome Bullet:** "Automated weekly Excel & SQL sales tracking across 4 regions, cutting reporting time by 4 hours/week."
          - **Freelance Proposal Rule:** Lead with the client's problem, outline 3 clear milestones, state exact deliverables, and avoid vague unrealistic promises.
        """.trimIndent(),
        commonMistake = "sending generic copy-pasted applications without tailoring to the role or client brief",
        tableHeaderMetric = "Application / Proposal Choice",
        tableRows = listOf(
          Triple("CV & Profile Bullets", "Quantify action + tool + measurable outcome (STAR)", "Listing generic duties like 'Responsible for tasks'"),
          Triple("Client & Job Outreach", "Tailor opening 3 lines to the specific brief + proof", "Sending identical unedited templates to 50 listings"),
          Triple("Scope & Expectations", "Confirm deliverables, timeline, and revision limits in writing", "Accepting endless unpriced 'small extra requests'")
        ),
        moduleCaseStudies = listOf(
          "A junior analyst sent 40 generic duty-list CVs with zero responses. Rewriting the top 3 experience bullets using Action + Tool + Quantified Outcome landed 4 interviews in two weeks.",
          "Replacing generic 'I am a fast learner' freelance bids with brief client-problem diagnostics and a 2-page visual portfolio sample tripled proposal reply rates.",
          "Using written 3-milestone scope agreements with clear revision caps prevented unpaid 'quick extra tweak' creep while preserving high client satisfaction ratings.",
          "Conducting structured STAR mock interview debriefs turned hesitant technical answers into crisp 90-second stories emphasizing verifiable business results."
        ),
        downloadExt = "txt",
        downloadBody = { title ->
          "DATALABS CAREER & CLIENT TEMPLATE — $title\n\nOUTCOME BULLET FORMULA:\n[Action Verb] + [Specific Task/Tool] + [Quantified Business Result]\n\nCLIENT PROPOSAL STRUCTURE:\n1. Your Understanding of Client Goal (2 sentences)\n2. Proposed 3-Step Execution Plan\n3. Relevant Portfolio Proof\n4. Clear Scope, Timeline & Next Step Call\n"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, what makes a CV bullet or freelance proposal stand out immediately to hiring managers or clients?",
            options = listOf(
              "Tailoring to the exact problem and highlighting measurable outcomes achieved",
              "Listing 20 unrelated hobbies in paragraph form",
              "Sending the exact same unedited message to every opening",
              "Using exaggerated claims with zero portfolio evidence"
            ),
            correctAnswer = "Tailoring to the exact problem and highlighting measurable outcomes achieved",
            explanation = "Decision-makers skim for problem relevance and measurable impact; generic spam or unbacked hype gets ignored.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A prospective freelance client asks for extra features beyond what was agreed in the original project brief. What is the professional response?",
            options = listOf(
              "Acknowledge the request politely, explain the impact on timeline, and send a written scope/fee addendum",
              "Disappear without replying for two weeks",
              "Agree verbally without updating deliverables or timeline",
              "Deliver half the original work without notice"
            ),
            correctAnswer = "Acknowledge the request politely, explain the impact on timeline, and send a written scope/fee addendum",
            explanation = "Structured scope communication maintains goodwill while protecting project timelines and fair pay.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "In behavioral interviews, what does the STAR framework stand for?",
            options = listOf(
              "Situation, Task, Action, Result",
              "Salary, Title, Application, Reference",
              "Study, Test, Apply, Repeat",
              "Summary, Time, Audience, Review"
            ),
            correctAnswer = "Situation, Task, Action, Result",
            explanation = "STAR keeps interview answers structured, concise, and anchored in measurable results you personally drove.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Linking 1–2 real portfolio samples in your LinkedIn or freelance profile substantially increases trust before the first call.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Proof of finished work lowers perceived hiring risk far better than self-described adjectives.",
            points = 33
          )
        }
      )

      academyId == "academy_marketing" || academyId == "academy_ecommerce" -> DomainKit(
        toolOrMethod = "Audience Funnel Mapping, Offer Clarity & Conversion Analytics",
        coreFramework = "Attention -> Trust -> Clear Offer -> Fulfillment -> Repeat Customer",
        workedExample = """
          - **E-commerce Landed Cost Check:**
            `Total Unit Cost = Supplier Price + Shipping + Customs/Taxes + Platform Fee + Packaging`
          - **Content Funnel Mix:** 50% Educational Value, 30% Proof / Case Studies, 20% Direct Offer with Clear Call-to-Action.
        """.trimIndent(),
        commonMistake = "running paid ads or importing stock before validating product demand and landed margin",
        tableHeaderMetric = "Growth & Storefront Lever",
        tableRows = listOf(
          Triple("Unit Economics", "Calculate landed cost + fees + return buffer upfront", "Pricing solely from supplier factory price"),
          Triple("Content & Offer", "Solve audience pain first + state delivery/price clearly", "Posting constant hard-sell flyers without trust proof"),
          Triple("Conversion Diagnosis", "Track CTR -> Product View -> Cart -> Completed Order", "Boosting ad budget blindly into a broken checkout page")
        ),
        moduleCaseStudies = listOf(
          "An online merchant imported 200 accessories based solely on social likes, only to find international air freight and platform fees wiped out net margin. Modeling landed unit cost upfront solved pricing.",
          "Shifting social posts from daily 'Buy Now' flyers to a 50% educational tips / 30% customer proof / 20% clear offer cadence doubled organic referral inquiries.",
          "Tracking funnel drop-off revealed 1,000 weekly visitors abandoned checkout at shipping calculation; displaying flat delivery fees clearly on product pages tripled conversion.",
          "Introducing same-day WhatsApp dispatch confirmations and a transparent 7-day exchange policy lifted 60-day repeat buyer orders from 9% to 28%."
        ),
        downloadExt = "csv",
        downloadBody = { _ ->
          "sku,product_name,supplier_usd,shipping_usd,fees_usd,selling_price_usd,net_margin_usd\nSKU-01,Ergonomic Laptop Stand,7.50,3.20,2.30,22.00,9.00\nSKU-02,Wireless Mic Kit,14.00,4.50,3.80,35.00,12.70\nSKU-03,LED Desk Lamp,6.00,2.80,1.90,18.00,7.30"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "In $title, why must total landed cost and marketplace fees be calculated before setting a retail price?",
            options = listOf(
              "Shipping, customs, payment fees, and returns can erase profit if ignored",
              "Because marketplace platforms charge zero commission or payment fees",
              "So sellers can avoid tracking customer satisfaction",
              "It reduces product quality automatically"
            ),
            correctAnswer = "Shipping, customs, payment fees, and returns can erase profit if ignored",
            explanation = "All fulfillment, platform, tax, and payment charges erode margin if left out of retail pricing math.",
            points = 25
          )
        },
        quizScenarioQuestion = { _ ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: A product page gets 1,000 clicks per week but only 2 sales. What should you inspect first?",
            options = listOf(
              "Product-market fit, trust signals, pricing clarity, photo quality, and checkout friction",
              "Delete customer reviews permanently",
              "Double ad spend without diagnosing why visitors leave",
              "Hide shipping times until after payment"
            ),
            correctAnswer = "Product-market fit, trust signals, pricing clarity, photo quality, and checkout friction",
            explanation = "Traffic without conversion indicates a product-offer or checkout trust issue; increasing ad spend only multiplies wasted budget.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "Why does combining educational value + customer proof outperform constant promotional posts?",
            options = listOf(
              "Audiences buy consistently from brands they trust and learn from",
              "Educational content removes the need for a clear product offer",
              "Algorithms prohibit product photos completely",
              "Customers prefer zero details before ordering"
            ),
            correctAnswer = "Audiences buy consistently from brands they trust and learn from",
            explanation = "Helpful content builds authority and organic sharing while social proof lowers buyer hesitation.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Fast fulfillment, transparent return policies, and responsive follow-up drive repeat orders more cheaply than acquiring cold traffic.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Repeat buyers convert at far lower acquisition cost when initial fulfillment builds confidence.",
            points = 33
          )
        }
      )

      else -> DomainKit(
        toolOrMethod = "Structured Action Plans, Active Practice & Peer Feedback",
        coreFramework = "Clarify Goal -> Break Into Daily Habits -> Review Progress Weekly",
        workedExample = """
          - **Active Recall & Deliberate Practice Loop:**
            1. Define a specific 25-minute practice target.
            2. Execute without distractions and summarize key principles in your own words.
            3. Test yourself or teach the concept simply to reveal gaps immediately.
        """.trimIndent(),
        commonMistake = "passive consumption without deliberate practice or clear weekly milestones",
        tableHeaderMetric = "Execution & Productivity Habit",
        tableRows = listOf(
          Triple("Study & Work Design", "25–45 minute focused blocks with one output goal", "Multitasking passively across 5 tabs with no finish line"),
          Triple("Retention Strategy", "Active recall summaries + applied mini-exercises", "Re-reading highlights repeatedly without testing yourself"),
          Triple("Quality Review", "Leave a short review buffer before deadline handoff", "Rushing unproofread drafts at the final minute")
        ),
        moduleCaseStudies = listOf(
          "A university finalist overwhelmed by coursework and part-time shifts replaced 3-hour unfocused re-reading marathons with structured 40-minute active-recall blocks and a clear daily finish line.",
          "Summarizing each chapter in plain 5-sentence teaching notes immediately exposed two weak statistical formulas before midterms rather than on exam morning.",
          "Scheduling a dedicated 20-minute error review buffer before every project handoff caught formatting and citation mistakes without late-night panic.",
          "Running Friday 15-minute milestone retrospectives kept long-term projects moving steadily week over week instead of piling up before finals."
        ),
        downloadExt = "txt",
        downloadBody = { title ->
          "DATALABS STUDY & EXECUTION PLANNER — $title\n\nWEEKLY MILESTONES:\n[ ] Day 1-2: Master core concepts & definitions\n[ ] Day 3-4: Complete hands-on guided exercise\n[ ] Day 5: Audit output against quality checklist\n[ ] Day 6-7: Finish capstone & pass assessment\n"
        },
        quizTopicQuestion = { title ->
          QuizQuestion(
            id = "q1",
            type = "MULTIPLE_CHOICE",
            prompt = "Which habit makes learning $title stick fastest for real-world use?",
            options = listOf(
              "Active recall and applying concepts to real tasks right after studying",
              "Re-reading the first paragraph passively ten times",
              "Skipping exercises until months later",
              "Studying without setting clear learning goals"
            ),
            correctAnswer = "Active recall and applying concepts to real tasks right after studying",
            explanation = "Retrieving and applying ideas immediately wires skills into long-term memory far more reliably than passive re-reading.",
            points = 25
          )
        },
        quizScenarioQuestion = { title ->
          QuizQuestion(
            id = "q2",
            type = "MULTIPLE_CHOICE",
            prompt = "Scenario: When preparing a deliverable for $title under a tight deadline, how do you maintain high quality?",
            options = listOf(
              "Break work into prioritized milestones, verify key facts, and leave buffer time for final review",
              "Wait until the final hour and skip proofreading",
              "Add unnecessary jargon to pad length",
              "Ignore stakeholder requirements completely"
            ),
            correctAnswer = "Break work into prioritized milestones, verify key facts, and leave buffer time for final review",
            explanation = "Milestone pacing plus a dedicated final verification pass prevents rushed errors while meeting deadlines cleanly.",
            points = 25
          )
        },
        moduleConceptQuestion = { _ ->
          QuizQuestion(
            id = "mq1",
            type = "MULTIPLE_CHOICE",
            prompt = "What distinguishes deliberate practice from casual repetition?",
            options = listOf(
              "Targeting a specific skill gap, getting feedback, and adjusting your method",
              "Repeating easy tasks you already know effortlessly",
              "Ignoring mistakes without reviewing why they happened",
              "Studying only when convenient once a quarter"
            ),
            correctAnswer = "Targeting a specific skill gap, getting feedback, and adjusting your method",
            explanation = "Deliberate practice deliberately stretches weak points with structured feedback loops.",
            points = 33
          )
        },
        moduleTrueFalseQuestion = { _ ->
          QuizQuestion(
            id = "mq2",
            type = "TRUE_FALSE",
            prompt = "True or False: Teaching or explaining a concept in plain language immediately exposes where your understanding is still fuzzy.",
            options = listOf("True", "False"),
            correctAnswer = "True",
            explanation = "True. Plain-language explanation forces mental clarity rather than hiding behind memorized buzzwords.",
            points = 33
          )
        }
      )
    }
  }

  private fun buildMarkdownComparisonTable(kit: DomainKit): String {
    val rows = kit.tableRows.joinToString("\n") { (a, b, c) -> "| $a | $b | $c |" }
    return """
      | ${kit.tableHeaderMetric} | High-Standard Practice | Common Risk to Avoid |
      |---|---|---|
      $rows
    """.trimIndent()
  }

  private fun buildModule1Notes(
    title: String,
    academyName: String,
    instructor: String,
    kit: DomainKit,
    caseStudy: String
  ): String {
    return """
      # Module 1: Foundations & Core Mental Models of $title
      **Academy:** $academyName • **Lead Instructor:** $instructor

      ## 1. Core Concept: Why Discipline Beats Trial-and-Error
      Most beginners struggle with **$title** not because the tools are obscure, but because they jump straight into execution before defining what decision or stakeholder outcome the work must serve. High-performing practitioners anchor every project in the **${kit.coreFramework}**, turning ambiguous requests into clear inputs, repeatable steps, and auditable outputs.

      ## 2. Foundational Principles Comparison
      ${buildMarkdownComparisonTable(kit)}

      ## 3. Core Building Blocks & Worked Example
      - **Primary Methodology:** ${kit.toolOrMethod}
      - **Primary Risk Guardrail:** Guard proactively against **${kit.commonMistake}**.
      - **Illustrative Pattern:**
      ${kit.workedExample}

      ## 4. Common Mistakes & Fixes
      - **Mistake:** Optimizing speed or visual polish before confirming exact input definitions and scope. **Fix:** Spend the first 5 minutes clarifying units, boundaries, and acceptance criteria upfront.

      ## 5. Case Study Analysis
      $caseStudy
    """.trimIndent()
  }

  private fun buildModule2Notes(title: String, kit: DomainKit, caseStudy: String): String {
    return """
      # Module 2: Methods, Patterns & Step-by-Step Walkthrough
      ## 1. Core Concept: Standardized Patterns Over Improvisation
      Improvising a fresh approach on every task creates hidden variation and silent errors. By applying **${kit.toolOrMethod}** through a repeatable 3-step execution architecture, colleagues can audit your logic cleanly in under two minutes:
      1. **Validate & Normalize Inputs:** Standardize formats, identifiers, and baseline assumptions before performing calculations or transformations.
      2. **Apply Explicit Modular Steps:** Keep staging logic separate from final presentation summaries so changes never break downstream figures.
      3. **Verify Against Concrete Examples:** Confirm logic on simple known test cases before scaling across full datasets or deliverables.

      ## 2. Before vs After Execution Contrast
      | Dimension | Ad-Hoc Approach (Fragile) | Engineered Pattern (Reliable) |
      |---|---|---|
      | **Inputs** | Unchecked raw files with mixed conventions | Validated schema, consistent units & clean keys |
      | **Logic** | Hidden manual edits or undocumented steps | Transparent **${kit.toolOrMethod}** |
      | **Verification** | Assumed correct if no crash appears | Tested on normal, edge, and missing-data rows |

      ## 3. Detailed Worked Walkthrough
      ${kit.workedExample}

      ## 4. Common Mistakes & Fixes
      - **Mistake:** Scaling an unverified draft immediately across hundreds of rows or client accounts. **Fix:** Run a small 3-to-5 item canary test covering messy edge cases first.

      ## 5. Case Study Analysis
      $caseStudy
    """.trimIndent()
  }

  private fun buildModule3Notes(title: String, shortAcademy: String, kit: DomainKit, caseStudy: String): String {
    return """
      # Module 3: Diagnosing Failures, Edge Cases & Quality Audits
      ## 1. Core Concept: Why Real-World Deliverables Fail Silently
      Under real deadlines in **$shortAcademy**, outputs rarely fail with loud red error banners—they fail quietly when **${kit.commonMistake}** slips past surface-level checks. Mastering **$title** requires systematic root-cause troubleshooting rather than guessing patches.

      ## 2. Root-Cause Troubleshooting Playbook
      | Symptom Observed | Likely Root Cause | Systematic Diagnostic Fix |
      |---|---|---|
      | Inflated totals or unexpected counts | Duplicate keys or unaggregated join grain | Inspect row counts before and after every transformation step |
      | Unexpected blank / `#N/A` results | Whitespace, casing, or type mismatches | Normalize formatting (`TRIM`, consistent types) and add clean fallbacks |
      | Conflicting stakeholder numbers | Undocumented rules or hardcoded constants | Centralize assumptions and document exact scope windows |

      ## 3. Worked Audit: Applying the 3-Minute Verification Pass
      Before finalizing any deliverable:
      1. **Boundary Audit:** Test zero values, missing fields, and outliers—does your method degrade safely instead of silently distorting averages?
      2. **Invariant Reconciliation:** Cross-check aggregate totals against an independent summary count from raw source records.
      3. **Assumption Transparency:** State data date ranges, exclusions, and uncertainty caveats explicitly alongside conclusions.

      ## 4. Common Mistakes & Fixes
      - **Mistake:** Deleting awkward edge-case records silently so charts look tidy. **Fix:** Flag anomalies transparently and document why any exclusions occurred.

      ## 5. Case Study Analysis
      $caseStudy
    """.trimIndent()
  }

  private fun buildModule4Notes(title: String, academyName: String, kit: DomainKit, caseStudy: String): String {
    return """
      # Module 4: Production Rollout, Governance & Decision Strategy
      ## 1. Core Concept: Moving From Individual Skill to Durable Systems
      Technical accuracy alone rarely drives adoption unless teammates understand, trust, and maintain the workflow after launch. Production mastery in **$title** (*$academyName*) combines clean engineering using **${kit.coreFramework}** with clear governance and executive communication.

      ## 2. Executive Decision Storytelling Framework
      Decision-makers skim under time pressure. Structure every strategic readout in four tight beats:
      1. **Headline Answer:** State the primary finding or recommendation in one clear sentence first.
      2. **Evidence & Verification:** Summarize the verified metric or workflow check proving why the conclusion holds.
      3. **Constraints & Trade-Offs:** Clarify budget, timeline, and risk safeguards against **${kit.commonMistake}**.
      4. **Concrete Next Action:** Name the specific decision, owner, and target date required next.

      ## 3. Production Governance Matrix
      | Governance Layer | Purpose | Practical Safeguard |
      |---|---|---|
      | **Standard Templates** | Prevent drift across team members | Lock reusable starter structures for **${kit.toolOrMethod}** |
      | **Scheduled Audits** | Catch silent drift when inputs evolve | Re-run verification invariants monthly or after schema updates |
      | **Handoff Docs** | Keep systems maintainable without founder bottlenecks | Record data sources, assumptions, and owner contacts cleanly |

      ## 4. Common Mistakes & Fixes
      - **Mistake:** Presenting 20 pages of raw background tables before stating what action the team should take. **Fix:** Lead with the 1-sentence insight and concrete action, placing supporting diagnostics underneath.

      ## 5. Case Study Analysis
      $caseStudy
    """.trimIndent()
  }

  private fun buildFoundationsDownload(title: String, academyName: String, kit: DomainKit, caseStudy: String): String {
    return """
      DATALABS LEARNING HUB — FOUNDATIONS STUDY GUIDE
      Course: $title
      Academy: $academyName
      Core Framework: ${kit.coreFramework}
      Primary Tooling: ${kit.toolOrMethod}

      CASE STUDY SNAPSHOT:
      $caseStudy

      KEY TAKEAWAYS:
      1. Start every task by clarifying the real-world decision or outcome required.
      2. Apply structured steps consistently instead of improvising from scratch.
      3. Guard against ${kit.commonMistake}.
      4. Document completed exercises cleanly for your professional portfolio.
    """.trimIndent()
  }

  private fun buildChecklistDownload(title: String, kit: DomainKit): String {
    return """
      DATALABS QUALITY CHECKLIST — $title
      [ ] 1. Objective & target audience defined in 1-2 clear sentences
      [ ] 2. Source inputs / requirements checked for completeness
      [ ] 3. Core method applied (${kit.toolOrMethod})
      [ ] 4. Checked specifically to prevent: ${kit.commonMistake}
      [ ] 5. Final summary reviewed for clarity, accuracy & next actions
    """.trimIndent()
  }

  private fun buildCapstoneDownload(title: String, academyName: String, kit: DomainKit): String {
    return """
      DATALABS CAPSTONE DELIVERABLE TEMPLATE — $title ($academyName)

      1. PROJECT TITLE & GOAL:
      Describe the career, freelance, or business problem you solved.

      2. PROCESS EXECUTED (${kit.coreFramework}):
      Summarize the exact steps and ${kit.toolOrMethod} applied.

      3. VERIFICATION & QUALITY CHECK:
      Explain how you verified accuracy and avoided ${kit.commonMistake}.

      4. MEASURABLE OUTCOME & NEXT STEP:
      State the final insight, deliverable link, or business impact achieved.
    """.trimIndent()
  }
}
